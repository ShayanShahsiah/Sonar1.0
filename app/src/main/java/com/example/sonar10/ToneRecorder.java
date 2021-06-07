package com.example.sonar10;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTimestamp;
import android.media.MediaRecorder;
import android.util.Log;

import java.util.Arrays;

public class ToneRecorder {
    private static final String TAG = "ToneRecorder";
    private final int duration = 32768;
    static final int DELAY = 10000;
    private static final int SAMPLE_RATE = ToneGenerator.SAMPLE_RATE;
    private final short[] buffer = new short[DELAY+duration];
    private short[] audio = new short[duration];
    private final AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                                            SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                                            AudioFormat.ENCODING_PCM_16BIT,
                                            buffer.length);
    static {
        Log.i(TAG, "minBufferSize: "+AudioRecord.getMinBufferSize(44100,
                                                                            AudioFormat.CHANNEL_IN_MONO,
                                                                            AudioFormat.ENCODING_PCM_16BIT));
    }
    void record() {
        recorder.startRecording();
        recorder.read(buffer, 0, buffer.length);
        recorder.stop();
        recorder.release();
        audio = Arrays.copyOfRange(buffer, DELAY, DELAY+duration);
    }
    short[] getAudio() {
        return audio;
    }
}
