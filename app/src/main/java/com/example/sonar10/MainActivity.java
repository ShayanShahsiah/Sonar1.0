package com.example.sonar10;

import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

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

    private LineGraphSeries<DataPoint> thumbSeries1 = new LineGraphSeries<>();
    private LineGraphSeries<DataPoint> thumbSeries2 = new LineGraphSeries<>();
    LineGraphSeries<DataPoint> series = new LineGraphSeries<>();

    private float thumb1;
    private float thumb2;

    private void drawBounds() {
        graph.removeAllSeries();

        //graph.removeSeries(thumbSeries1);
        thumbSeries1 = new LineGraphSeries<>();
        thumbSeries1.appendData(new DataPoint(thumb1, 0), true, maxX);
        thumbSeries1.appendData(new DataPoint(thumb1, maxY), true, maxX);
        thumbSeries1.setColor(Color.RED);
        graph.addSeries(thumbSeries1);

        //graph.removeSeries(thumbSeries2);
        thumbSeries2 = new LineGraphSeries<>();
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
                    //graph.removeAllSeries();

                    final Thread thread1 = new Thread(() -> {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        ToneGenerator.playSound(pulse);
                    });
                    thread1.start();

                    ToneRecorder recorder = new ToneRecorder();
                    recorder.record();

                    double[] data = CrossCorrelation.correlate(recorder.getAudio(), pulse);
                    //graph.removeSeries(series);
                    series = new LineGraphSeries<>();

                    List<Double> distanceList = new ArrayList<>();
                    List<Double> valueList = new ArrayList<>();

                    for (int i = 0; i < data.length; i++) {
                        if (data[i] > 30000*12) {
                            //System.out.println(i + "\t" + (data.size() - i));
                            Log.i(TAG, "PEAK: " + i);
                            for (int j = 0; j < maxX && i + j < data.length; j++) {
                                double w = 12.2;
                                double l = (j - 500/3.0)/8.0/ToneGenerator.SAMPLE_RATE/2.0*34300 + 2.0;
                                double d = Math.sqrt(l*l - w*w/4);
                                double value = data[i + j - 100]/120000;

                                if (d > res.getInteger(R.integer.graph_end))
                                    break;

                                if (j%4 == 0)
                                    series.appendData(new DataPoint(d, value), true, maxX);

                                if (d > thumb1 && d < thumb2) {
                                    distanceList.add(d);
                                    valueList.add(value);
                                }
                            }
                            break;
                        }
                    }
                    int peak1Idx = maxIdx(valueList);
                    double peak1Distance = distanceList.size() > 0 ? distanceList.get(peak1Idx) : Double.NaN;

                    runOnUiThread(() -> {
                        distanceView.setText(String.format(Locale.ENGLISH, "%.1f", peak1Distance));
                    });


                    //graph.addSeries(series);
                    drawBounds();

                    try {
                        Thread.sleep(900);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
            thread.start();
        });
    }

    private int maxIdx(List<Double> list) {
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
}
