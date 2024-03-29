package com.example.sonar10;

import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTimestamp;
import android.media.MediaRecorder;
import android.util.Log;

import java.util.Arrays;

public class ToneRecorder {
    private static final String TAG = "ToneRecorder";
    private final int duration = 32768;//65536;//16384;
    static final int DELAY = 8000;
    private final short[] buffer = new short[DELAY+duration];
    private static final int CHANNEL = AudioFormat.CHANNEL_IN_MONO;
    @SuppressLint("MissingPermission")
    private final AudioRecord recorder = new AudioRecord(
            MediaRecorder.AudioSource.MIC,
            Pulse.SAMPLE_RATE,
            CHANNEL,
            AudioFormat.ENCODING_PCM_16BIT,
            2*buffer.length);

    static {
        Log.i(TAG, "minBufferSize: " + AudioRecord.getMinBufferSize(
                Pulse.SAMPLE_RATE,
                CHANNEL,
                AudioFormat.ENCODING_PCM_16BIT));
    }

    void record() {
        recorder.startRecording();
        recorder.read(buffer, 0, buffer.length);
        recorder.stop();
        recorder.release();
    }

    short[] getAudio() {
        return Arrays.copyOfRange(buffer, DELAY, DELAY+duration);
    }

    boolean isPastDelay() {
        AudioTimestamp stamp = new AudioTimestamp();
        int status = recorder.getTimestamp(stamp, AudioTimestamp.TIMEBASE_BOOTTIME);
        if (status != AudioRecord.SUCCESS)
            Log.e("AudioRecord, ToneRecorder", "Unable to acquire timestamp.");
        return stamp.framePosition > DELAY;
    }
}
