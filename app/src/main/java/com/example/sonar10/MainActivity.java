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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    GraphView graph;
    boolean shouldRepeat = false;
    static final int maxX = 400*3*6;
    static final int maxY = Short.MAX_VALUE*2/10000;
    private static final Pulse pulse = new LinearChirp();

    LineGraphSeries<DataPoint> series = new LineGraphSeries<>();

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
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    1);

        }

        Resources res = getResources();
        thumb1 = res.getIntArray(R.array.slider_values)[0];
        thumb2 = res.getIntArray(R.array.slider_values)[1];


        graph = findViewById(R.id.graph);
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMaxY(maxY);
        graph.getViewport().setMinY(0);
        graph.getViewport().setMaxX(res.getInteger(R.integer.graph_end));
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

                    final Thread thread1 = new Thread(() -> {
                        try {
                            Thread.sleep(300);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        ToneGenerator.playSound(pulse);
                    });
                    thread1.start();

                    ToneRecorder recorder = new ToneRecorder();
                    recorder.record();

                    double[] data = CrossCorrelation.correlate(recorder.getAudio(), pulse);

                    series = new LineGraphSeries<>();

                    List<Double> distanceList = new ArrayList<>();
                    List<Double> valueList = new ArrayList<>();

                    int startIdx = 0;
                    for (int i = 0; i < data.length; i++) {
                        if (data[i] > 3 * 120000 * 2.5) {
                            startIdx = i;
                            Log.i(TAG, "startIdx: " + i);
                            break;
                        }
                    }
                    for (int j = 0; j < maxX && startIdx + j < data.length; j++) {
                        double w = 12.2;
                        double l = j/8./ToneGenerator.SAMPLE_RATE/2.*34300. + 0.2;
                        double d = Math.sqrt(l*l - w*w/4);
                        double value = startIdx+j < 100 ? 0 : data[startIdx+j-100]/120000;

                        if (d > res.getInteger(R.integer.graph_end))
                            break;

                        if (j%4 == 0)
                            series.appendData(new DataPoint(d, value), true, maxX);

                        if (d > thumb1 && d < thumb2) {
                            distanceList.add(d);
                            valueList.add(value);
                        }
                    }
                    int peak1Idx = maxIdx(valueList);

                    double peak1Distance;
                    if (distanceList.size() > 0 && valueList.get(peak1Idx) > 0.5)
                        peak1Distance = distanceList.get(peak1Idx);
                    else
                        peak1Distance = Double.NaN;

                    runOnUiThread(() -> {
                        distanceView.setText(String.format(Locale.ENGLISH, "%.1f", peak1Distance));
                    });

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
