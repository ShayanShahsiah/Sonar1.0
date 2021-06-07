package com.example.sonar10;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

import java.util.Arrays;

public class CrossCorrelation {

    private static double[] toDoubles(short[] input) {
        double[] res = new double[input.length];
        for (int i = 0; i < input.length; i++) {
            res[i] = input[i];
        }
        return res;
    }

    private static double[] absolutify(double[] input) {
        double[] res = new double[input.length];
        for (int i = 0; i < input.length; i++) {
            res[i] = Math.abs(input[i]);
        }
        return res;
    }

    private static double[] realParts(Complex[] input) {
        double[] res = new double[input.length];
        for (int i = 0; i < input.length; i++) {
            res[i] = input[i].getReal();
        }
        return res;
    }

//    private static double[] binomify(double[] input) {
//        int newLength = (int) Math.pow(2, Math.ceil(Math.log(input.length) / Math.log(2)));
//        return Arrays.copyOf(input, newLength);
//    }

    static Complex[] interpolate(Complex[] fft, int factor) {
        int n = fft.length;
        int p = factor * n;
        for (int i = 0; i < fft.length; i++) {
            fft[i] = fft[i].multiply(Math.sqrt((double) p / n));
        }
        Complex[] newFft = new Complex[p];
        int fftIdx = 0;
        for (int i = 0; i < n / 2 + 1; i++) {
            newFft[i] = fft[fftIdx++];
        }
        for (int i = n / 2 + 1; i < p - n / 2; i++) {
            newFft[i] = new Complex(0);
        }
        fftIdx--;
        for (int i = p - n / 2; i < p; i++) {
            newFft[i] = fft[fftIdx++];
        }
        return newFft;
    }

    static double[] correlate(short[] recording, Pulse pulse) {
        int factor = 8;

        FastFourierTransformer transformer = new FastFourierTransformer(DftNormalization.STANDARD);
        Complex[] recordingFft = transformer.transform(toDoubles(recording), TransformType.FORWARD);
        recordingFft = interpolate(recordingFft, factor);

        int length = recordingFft.length;

        double[] transmittedPulse = Arrays.copyOf(pulse.getDoubles(ToneGenerator.SAMPLE_RATE * factor), length);
        Complex[] transmittedPulseFft = transformer.transform(transmittedPulse, TransformType.FORWARD);

        Complex[] correlationFft = new Complex[length];
        for (int i = 0; i < length; i++) {
            correlationFft[i] = transmittedPulseFft[i].conjugate().multiply(recordingFft[i]);
        }
        Complex[] correlation = transformer.transform(correlationFft, TransformType.INVERSE);

        return absolutify(realParts(correlation));
    }
}
