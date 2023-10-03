package com.example.sonar10;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.slider.RangeSlider;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.math3.stat.StatUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.IntStream;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int MAX_X = 180;
    private static final int MAX_Y = 10;
    private static final int MAX_X_SAMPLES = (int) Math.round(2. * MAX_X / 34300. * Pulse.SAMPLE_RATE);
    private static final Pulse mPulse = new LinearChirp();
    private final LineGraphSeries<DataPoint> mSeries = new LineGraphSeries<>();
    private final LineGraphSeries<DataPoint> mThumbSeries1 = new LineGraphSeries<>();
    private final LineGraphSeries<DataPoint> mThumbSeries2 = new LineGraphSeries<>();
    private final Lock mLock = new ReentrantLock();
    private final Handler mHandler = new Handler();
    private boolean mShouldRepeat = false;
    private RangeSlider mSlider;
    private TextView mDistanceView;
    private Button mToggleButton;
    private DataPoint[] mDataPoints;

    {
        mSeries.setColor(Color.rgb(3, 160, 62));
        mThumbSeries1.setColor(Color.RED);
        mThumbSeries2.setColor(Color.RED);
    }

    private ImmutablePair<Integer, Integer> findPeak(double[] data) {
        int maxIdx = 0;
        for (int i = 0; i < data.length; i++)
            maxIdx = data[i] > data[maxIdx] ? i : maxIdx;

        int delta = 5;
        double[] maxVals = new double[delta];
        double[] maxInds = new double[delta];
        int nLocal = 0;
        for (int i = maxIdx; i < data.length-1; i++) {
            if (!(data[i] > data[i-1] && data[i] > data[i+1]))
                continue;

            maxInds[nLocal] = i;
            maxVals[nLocal] = data[i];
            if (++nLocal == delta)
                break;
        }
        return ImmutablePair.of(
                (int) Math.round(StatUtils.mean(maxInds)),
                (int) Math.round(StatUtils.mean(maxVals)));
    }

    private void populateSeries(double[] data,
                                DataPoint[] dataPoints,
                                List<Double> distanceList,
                                List<Double> valueList) {
        ImmutablePair<Integer, Integer> peak = findPeak(data);
        int startIdx = peak.left;
        int startVal = peak.right;

        for (int i = 0; i < MAX_X_SAMPLES && startIdx + i < data.length; i++) {
            int offset = 10;
            double value = startIdx+i-offset >= 0 ? 40. * data[startIdx+i-offset] / startVal : 0.;
            double l = .5 * i / Pulse.SAMPLE_RATE * 34300.;
            //double d = Math.sqrt(l*l - 12.2*12.2/4);
            dataPoints[i] = new DataPoint(l, value);

            List<Float> vals = mSlider.getValues();
            float thumb1 = vals.get(0), thumb2 = vals.get(1);
            if (l > thumb1 && l < thumb2) {
                distanceList.add(l);
                valueList.add(value);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String[] permissions = {Manifest.permission.RECORD_AUDIO};
        int permissionStatus = ContextCompat.checkSelfPermission(this, permissions[0]);
        if (permissionStatus != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, permissions,1);

        GraphView graph = findViewById(R.id.graph);
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMaxY(MAX_Y);
        graph.getViewport().setMinY(0);
        graph.getViewport().setMaxX(MAX_X);
        graph.getViewport().setMinX(getResources().getInteger(R.integer.graph_start));

        graph.addSeries(mSeries);
        graph.addSeries(mThumbSeries1);
        graph.addSeries(mThumbSeries2);

        mDistanceView = findViewById(R.id.distanceView);
        mSlider = findViewById(R.id.slider);

        mToggleButton = findViewById(R.id.toggleButton);
        mToggleButton.setOnClickListener(this::onClickToggle);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != 1)
            return;
        if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permission denied!", Toast.LENGTH_LONG).show();
            this.finishAffinity();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mHandler.post(this::updateGraph);
    }

    @Override
    protected void onPause() {
        mHandler.removeCallbacks(this::updateGraph);
        super.onPause();
    }

    public void onClickToggle(View view) {
        mShouldRepeat = !mShouldRepeat;
        if (mShouldRepeat) {
            mToggleButton.setText(R.string.toggle_button_text_stop);
            new Thread(this::measure).start();
        }
        else
            mToggleButton.setText(R.string.toggle_button_text_start);
    }

    public void updateGraph() {
        List<Float> vals = mSlider.getValues();
        float thumb1 = vals.get(0), thumb2 = vals.get(1);
        DataPoint[] thumbArray1 = {new DataPoint(thumb1, 0), new DataPoint(thumb1, MAX_Y)};
        mThumbSeries1.resetData(thumbArray1);
        DataPoint[] thumbArray2 = {new DataPoint(thumb2, 0), new DataPoint(thumb2, MAX_Y)};
        mThumbSeries2.resetData(thumbArray2);

        mLock.lock();
        if (mDataPoints != null)
            mSeries.resetData(mDataPoints);
        mLock.unlock();
        mHandler.postDelayed(this::updateGraph, 30);
    }

    public void measure() {
        while (mShouldRepeat) {
            ToneRecorder recorder = new ToneRecorder();

            final Thread toneGenThread = new Thread(() -> {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (!recorder.isPastDelay())
                    Log.w(TAG, "Recorder not past delay!");
                ToneGenerator.playSound(mPulse);
            });
            toneGenThread.start();

            recorder.record();
            if (toneGenThread.isAlive())
                Log.w(TAG, "Recording has finished too early!");

            double[] data = CrossCorrelation.correlate(recorder.getAudio(), mPulse);
            List<Double> distances = new ArrayList<>();
            List<Double> values = new ArrayList<>();

            mLock.lock();
            mDataPoints = new DataPoint[MAX_X_SAMPLES];
            populateSeries(data, mDataPoints, distances, values);
            mLock.unlock();

            int peakIdx = IntStream.range(0, values.size())
                    .reduce((i, j) -> values.get(i) > values.get(j) ? i : j)
                    .orElse(-1);
            if (peakIdx == -1)
                continue;

            double peakDist = values.get(peakIdx) > 0.5 ? distances.get(peakIdx) : Double.NaN;
            String text = String.format(Locale.ENGLISH, "%.1f cm", peakDist);
            runOnUiThread(() -> mDistanceView.setText(text));
        }
    }
}
