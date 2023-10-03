package com.example.sonar10;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTimestamp;
import android.media.AudioTrack;
import android.util.Log;

public class ToneGenerator {
    private static final String TAG = "ToneGenerator";

    static void playSound(Pulse pulse){
        final AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                Pulse.SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT, 2*pulse.size(),
                AudioTrack.MODE_STATIC);
        audioTrack.setVolume(AudioTrack.getMaxVolume());
        audioTrack.write(pulse.getShorts(), 0, pulse.size());
        audioTrack.play();
        try {
            Thread.sleep((int)(1000*pulse.duration()));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        audioTrack.release();
    }
}
