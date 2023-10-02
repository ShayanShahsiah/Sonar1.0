package com.example.sonar10;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTimestamp;
import android.media.MediaRecorder;
import android.util.Log;

import java.util.Arrays;

public class ToneRecorder {
    private static final String TAG = "ToneRecorder";
    private final int duration = 32768;//32768;//8192;//65536;//16384;
    static final int DELAY = 8000;
    private static final int SAMPLE_RATE = Pulse.SAMPLE_RATE;
    private final short[] buffer = new short[DELAY+duration];
//    private final short[] audioL = new short[duration];
    private final short[] audioR = new short[duration];
    private final AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                                            SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                                            AudioFormat.ENCODING_PCM_16BIT,
                                            2*buffer.length);
    static {
        Log.i(TAG, "minBufferSize: "+AudioRecord.getMinBufferSize(SAMPLE_RATE,
                                                                            AudioFormat.CHANNEL_IN_MONO,
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
