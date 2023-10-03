package com.example.sonar10;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.Resources;
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

import org.apache.commons.math3.stat.StatUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "MainActivity";
    private final Handler mHandler = new Handler();
    GraphView mGraph;
    DataPoint[] mDataPoints;
    boolean shouldRepeat = false;
    static final int factor = 1;
    static final int maxX = (int)Math.round(180*factor*ToneGenerator.SAMPLE_RATE*2./34300.);//400*3*6;
    static final int maxY = 10;
    private static final Pulse pulse = new LinearChirp();//new PhaseMod();

    RangeSlider mSlider;
    TextView mDistanceView;

    private final LineGraphSeries<DataPoint> mSeries = new LineGraphSeries<>();
    private final LineGraphSeries<DataPoint> thumbSeries1 = new LineGraphSeries<>();
    private final LineGraphSeries<DataPoint> thumbSeries2 = new LineGraphSeries<>();

    private final Lock mLock = new ReentrantLock();

    {
        mSeries.setColor(Color.rgb(3, 160, 62));
        thumbSeries1.setColor(Color.RED);
        thumbSeries2.setColor(Color.RED);
    }

    private int[] findPeak(double[] data) {
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

        return new int[] {(int) Math.round(StatUtils.mean(maxInds)), (int) Math.round(StatUtils.mean(maxVals))};
    }

    int startIdx = 0;
    int startY = 0;
    private void populateSeries(double[] data,
                                DataPoint[] series,
                                List<Double> distanceList,
                                List<Double> valueList) {
        int[] peak = findPeak(data);
        startIdx = peak[0];
        startY = peak[1];

        for (int j = 0; j < maxX && startIdx + j < data.length; j++) {
            double l = (double) j/ToneGenerator.SAMPLE_RATE/2.*34300./factor;
            //double w = 12.2;
            //double d = Math.sqrt(l*l - w*w/4);
            int offset = 10 * factor;
            double value = startIdx+j < offset ? 0 : data[startIdx+j-offset]*10/(0.25*startY);

            series[j] = new DataPoint(l, value);

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

        Resources res = getResources();

        mGraph = findViewById(R.id.graph);
        mGraph.getViewport().setYAxisBoundsManual(true);
        mGraph.getViewport().setXAxisBoundsManual(true);
        mGraph.getViewport().setMaxY(maxY);
        mGraph.getViewport().setMinY(0);
        mGraph.getViewport().setMaxX((double) maxX/ToneGenerator.SAMPLE_RATE/2.*34300./factor);
        mGraph.getViewport().setMinX(res.getInteger(R.integer.graph_start));

        mGraph.addSeries(mSeries);
        mGraph.addSeries(thumbSeries1);
        mGraph.addSeries(thumbSeries2);

        mDistanceView = findViewById(R.id.distanceView);
        mSlider = findViewById(R.id.slider);

        Button button = findViewById(R.id.button1);
        button.setOnClickListener(this);
    }

    private static int maxIdx(List<Double> list) {
        int maxIdx = 0;
        double max = 0;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) > max) {
                max = list.get(i);
                maxIdx = i;
            }
        }
        return maxIdx;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == 1 &&
                (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED)) {
            Toast.makeText(MainActivity.this, "Permission denied!", Toast.LENGTH_LONG).show();
            this.finishAffinity();
        }
    }

    @Override
    public void onClick(View view) {
        shouldRepeat = !shouldRepeat;
        new Thread(this::pulsate).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mHandler.postDelayed(this::updateGraph, 30);
    }

    @Override
    protected void onPause() {
        mHandler.removeCallbacks(this::updateGraph);
        super.onPause();
    }

    public void updateGraph() {
        List<Float> vals = mSlider.getValues();
        float thumb1 = vals.get(0), thumb2 = vals.get(1);
        DataPoint[] thumbArray1 = {new DataPoint(thumb1, 0), new DataPoint(thumb1, maxY)};
        thumbSeries1.resetData(thumbArray1);
        DataPoint[] thumbArray2 = {new DataPoint(thumb2, 0), new DataPoint(thumb2, maxY)};
        thumbSeries2.resetData(thumbArray2);

        mLock.lock();
        if (mDataPoints != null)
            mSeries.resetData(mDataPoints);
        mLock.unlock();
        mHandler.postDelayed(this::updateGraph, 30);
    }

    public void pulsate() {
        while (shouldRepeat) {
            ToneRecorder recorder = new ToneRecorder();

            final Thread toneGenThread = new Thread(() -> {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                //Log.i(TAG, ""+recorder.isPastDelay());
                ToneGenerator.playSound(pulse);
            });
            toneGenThread.start();

            recorder.record();
            if (toneGenThread.isAlive())
                Log.e(TAG, "Recording has finished too early!");

            double[] data = CrossCorrelation.correlate(recorder.getAudio(), pulse);
            List<Double> distanceList = new ArrayList<>();
            List<Double> valueList = new ArrayList<>();

            mLock.lock();
            mDataPoints = new DataPoint[maxX];
            populateSeries(data, mDataPoints, distanceList, valueList);
            mLock.unlock();

            int peakIdx = maxIdx(valueList);
            double peakDist;
            if (distanceList.size() > 0 && valueList.get(peakIdx) > 0.5)
                peakDist = distanceList.get(peakIdx);
            else
                peakDist = Double.NaN;

            runOnUiThread(() -> mDistanceView.setText(String.format(Locale.ENGLISH,
                    "%.1f cm", peakDist)));
        }
    }
}
