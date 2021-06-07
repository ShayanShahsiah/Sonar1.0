package com.example.sonar10;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTimestamp;
import android.media.AudioTrack;
import android.util.Log;

public class ToneGenerator {
    private static final String TAG = "ToneGenerator";
    static final int SAMPLE_RATE = 44100;

    static void playSound(Pulse pulse){
        final AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT, pulse.getLength(SAMPLE_RATE),
                AudioTrack.MODE_STATIC);
        audioTrack.setVolume(AudioTrack.getMaxVolume());
        audioTrack.write(pulse.getShorts(SAMPLE_RATE), 0, pulse.getLength(SAMPLE_RATE));
        audioTrack.play();
    }
}
