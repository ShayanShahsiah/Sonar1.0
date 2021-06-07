package com.example.sonar10;

abstract class Pulse {
    static double DURATION = (double) 700 / 44100;
    int getLength(int sampleRate) {
        return (int) (sampleRate * DURATION);
    }
    abstract double[] getDoubles(int sampleRate);
    abstract short[] getShorts(int sampleRate);
}

class SingleFreqPulse extends Pulse {
    static final int FREQUENCY = 10000;

    @Override
    public double[] getDoubles(int sampleRate) {
        double[] res = new double[getLength(sampleRate)];
        for (int i = 0; i < getLength(sampleRate); i++) {
            res[i] = Math.sin(2.0 * Math.PI * FREQUENCY * i / sampleRate);
        }
        return res;
    }

    @Override
    public short[] getShorts(int sampleRate) {
        short[] res = new short[getLength(sampleRate)];
        for (int i = 0; i < getLength(sampleRate); i++) {
            res[i] = (short) Math.round(Short.MAX_VALUE * Math.sin(2.0 * Math.PI * FREQUENCY * i / sampleRate));
        }
        return res;
    }
}

class LinearChirp extends Pulse {
    static final int INITIAL_FREQ = 5000;
    static final int FINAL_FREQ = 15000;

    @Override
    public double[] getDoubles(int sampleRate) {
        final double freqStep = (double) (FINAL_FREQ - INITIAL_FREQ) / getLength(sampleRate);
        double[] res = new double[getLength(sampleRate)];
        for (int i = 0; i < getLength(sampleRate); i++) {
            double freq = INITIAL_FREQ + i * freqStep;
            double t = (double) i / sampleRate;
            double amp = 1; //0.08 + 0.92 * Math.pow(Math.cos(Math.PI*i/130), 2);
            res[i] = amp * Math.sin(2.0 * Math.PI * freq * t);
        }
        return res;
    }

    @Override
    public short[] getShorts(int sampleRate) {
        final double freqStep = (double) (FINAL_FREQ - INITIAL_FREQ) / getLength(sampleRate);
        short[] res = new short[getLength(sampleRate)];
        for (int i = 0; i < getLength(sampleRate); i++) {
            double freq = INITIAL_FREQ + i * freqStep;
            double t = (double) i / sampleRate;
            double amp = 1; //0.08 + 0.92 * Math.pow(Math.cos(Math.PI*i/130), 2);
            res[i] = (short) Math.round(amp * Short.MAX_VALUE * Math.sin(2.0 * Math.PI * freq * t));
        }
        return res;
    }
}

class LinearChirpWithWindow extends Pulse {
    static final int INITIAL_FREQ = 10000;
    static final int FINAL_FREQ = 20000;

    @Override
    public double[] getDoubles(int sampleRate) {
        final double freqStep = (double) (FINAL_FREQ - INITIAL_FREQ) / getLength(sampleRate);
        double[] res = new double[getLength(sampleRate)];
        for (int i = 0; i < getLength(sampleRate); i++) {
            double freq = INITIAL_FREQ + i * freqStep;
            double t = (double) i / sampleRate;
            double fraction = 1.0 - Math.abs(i - getLength(sampleRate) / 2.0) / (getLength(sampleRate) / 2.0);
            res[i] = fraction * Math.sin(2.0 * Math.PI * freq * t);
        }
        return res;
    }

    @Override
    public short[] getShorts(int sampleRate) {
        final double freqStep = (double) (FINAL_FREQ - INITIAL_FREQ) / getLength(sampleRate);
        short[] res = new short[getLength(sampleRate)];
        for (int i = 0; i < getLength(sampleRate); i++) {
            double freq = INITIAL_FREQ + i * freqStep;
            double t = (double) i / sampleRate;

            double fraction = 1;
            if (i < 0.33 * getLength(sampleRate))
                fraction = 3.0 * i / getLength(sampleRate);
            else if (i > 0.66 * getLength(sampleRate))
                fraction = 3.0 * (getLength(sampleRate) - i) / getLength(sampleRate);

            res[i] = (short) Math.round(fraction * Short.MAX_VALUE * Math.sin(2.0 * Math.PI * freq * t));
        }
        return res;
    }
}
