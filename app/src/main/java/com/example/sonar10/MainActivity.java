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

import android.util.Log;
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


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    GraphView graph;
    boolean shouldRepeat = false;
    static final int factor = 1;
    static final int maxX = (int)Math.round(180*factor*ToneGenerator.SAMPLE_RATE*2./34300.);//400*3*6;
    static final int maxY = 10;
    private static final Pulse pulse = new LinearChirp();//new PhaseMod();//new LinearChirp();

    LineGraphSeries<DataPoint> series = new LineGraphSeries<>();
    LineGraphSeries<DataPoint> seriesL = new LineGraphSeries<>();

    LineGraphSeries<DataPoint> seriesP = new LineGraphSeries<>();

    private float thumb1;
    private float thumb2;

    private void drawBounds() {
        graph.removeAllSeries();

        LineGraphSeries<DataPoint> thumbSeries1 = new LineGraphSeries<>();
        thumbSeries1.appendData(new DataPoint(thumb1, 0), true, maxX);
        thumbSeries1.appendData(new DataPoint(thumb1, maxY), true, maxX);
        thumbSeries1.setColor(Color.RED);
        graph.addSeries(thumbSeries1);

        LineGraphSeries<DataPoint> thumbSeries2 = new LineGraphSeries<>();
        thumbSeries2.appendData(new DataPoint(thumb2, 0), true, maxX);
        thumbSeries2.appendData(new DataPoint(thumb2, maxY), true, maxX);
        thumbSeries2.setColor(Color.RED);
        graph.addSeries(thumbSeries2);

        series.setColor(Color.rgb(3, 160, 62));
        graph.addSeries(series);

        seriesL.setColor(Color.rgb(3, 62, 160));
        graph.addSeries(seriesL);

        seriesP.setColor(Color.RED);
        graph.addSeries(seriesP);
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
                                LineGraphSeries<DataPoint> series,
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

            series.appendData(new DataPoint(l, value), true, maxX);

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

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.RECORD_AUDIO},1);

        }

        Resources res = getResources();
        thumb1 = res.getIntArray(R.array.slider_values)[0];
        thumb2 = res.getIntArray(R.array.slider_values)[1];


        graph = findViewById(R.id.graph);
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMaxY(maxY);
        graph.getViewport().setMinY(0);
        graph.getViewport().setMaxX((double) maxX/ToneGenerator.SAMPLE_RATE/2.*34300./factor);
        graph.getViewport().setMinX(res.getInteger(R.integer.graph_start));


        Button button1 = findViewById(R.id.button1);
        TextView distanceView = findViewById(R.id.distanceView);
        RangeSlider slider = findViewById(R.id.slider);

        drawBounds();

        slider.addOnChangeListener((slider1, value, fromUser) -> {
            thumb1 = slider.getValues().get(0);
            thumb2 = slider.getValues().get(1);
        });

        button1.setOnClickListener(v -> {
            shouldRepeat = !shouldRepeat;
            final Thread thread = new Thread(() -> {
                while (shouldRepeat) {
                    ToneRecorder recorder = new ToneRecorder();

                    final Thread thread1 = new Thread(() -> {
                        try {
                            Thread.sleep(300);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        //Log.i("MinActivity", ""+recorder.isPastDelay());
                        ToneGenerator.playSound(pulse);
                    });
                    thread1.start();

                    recorder.record();
                    if (thread1.isAlive())
                        Log.e("RECORDING", "Recording has finished too early!");

                    double[] data = CrossCorrelation.correlate(recorder.getAudio(), pulse);
                    series = new LineGraphSeries<>();
                    List<Double> distanceList = new ArrayList<>();
                    List<Double> valueList = new ArrayList<>();
                    populateSeries(data, series, distanceList, valueList);

                    int peakIdx = maxIdx(valueList);
                    double peakDist;
                    if (distanceList.size() > 0 && valueList.get(peakIdx) > 0.5)
                        peakDist = distanceList.get(peakIdx);
                    else
                        peakDist = Double.NaN;

                    runOnUiThread(() -> distanceView.setText(String.format(Locale.ENGLISH,
                            "%.1f cm", peakDist)));

                    drawBounds();
                }
            });
            thread.start();
        });
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1 && (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED)) {
            Toast.makeText(MainActivity.this, "Permission denied!", Toast.LENGTH_LONG).show();
            this.finishAffinity();
        }
    }
}
