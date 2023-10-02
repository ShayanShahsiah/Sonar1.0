package com.example.sonar10;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTimestamp;
import android.media.MediaRecorder;
import android.util.Log;

import org.apache.commons.math3.transform.TransformUtils;

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
                                            SAMPLE_RATE, AudioFormat.CHANNEL_IN_DEFAULT,
                                            AudioFormat.ENCODING_PCM_16BIT,
                                            2*buffer.length);
    static {
        Log.i(TAG, "minBufferSize: "+AudioRecord.getMinBufferSize(SAMPLE_RATE,
                                                                            AudioFormat.CHANNEL_IN_DEFAULT,
                                                                            AudioFormat.ENCODING_PCM_16BIT));
    }
    private boolean didLoad = false;

    void record() {
        recorder.startRecording();
        recorder.read(buffer, 0, buffer.length);
        recorder.stop();
        recorder.release();
    }

    private void loadData() {
        //audio = Arrays.copyOfRange(buffer, DELAY, DELAY+duration);
        for (int i = 0; i < duration; i++) {
//            audioL[i] = buffer[2*DELAY + 2*i];
//            audioR[i] = buffer[2*DELAY + 2*i+1];
            audioR[i] = buffer[i];
        }
        didLoad = true;
    }

//    short[] getAudioL() {
//        if (!didLoad)
//            loadData();
//        return audioL;
//    }

    short[] getAudioR() {
        if (!didLoad)
            loadData();
        return audioR;
    }

    boolean isPastDelay() {
        AudioTimestamp stamp = new AudioTimestamp();
        int status = recorder.getTimestamp(stamp, AudioTimestamp.TIMEBASE_BOOTTIME);
        if (status != AudioRecord.SUCCESS)
            Log.e("AudioRecord, ToneRecorder", "Unable to acquire timestamp.");
        return stamp.framePosition > DELAY;
    }
}
