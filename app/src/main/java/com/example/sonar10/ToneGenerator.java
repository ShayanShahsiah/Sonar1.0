package com.example.sonar10;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTimestamp;
import android.media.AudioTrack;
import android.util.Log;

public class ToneGenerator {
    private static final String TAG = "ToneGenerator";
    private static final int CHANNEL = AudioFormat.CHANNEL_OUT_STEREO;

    static void playSound(Pulse pulse){
        int bufSizeShorts = 2*pulse.size();
        final AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                Pulse.SAMPLE_RATE, CHANNEL,
                AudioFormat.ENCODING_PCM_16BIT,
                2*bufSizeShorts,
                AudioTrack.MODE_STATIC);
        audioTrack.setVolume(AudioTrack.getMaxVolume());
        audioTrack.write(
                pulse.getShorts(CHANNEL == AudioFormat.CHANNEL_OUT_STEREO),
                0,
                bufSizeShorts);
        audioTrack.play();
        try {
            Thread.sleep((int)(1000*pulse.duration()));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        audioTrack.release();
    }
}
