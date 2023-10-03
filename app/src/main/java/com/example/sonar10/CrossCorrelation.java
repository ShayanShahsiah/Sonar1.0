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

    static double[] correlate(short[] recording, Pulse pulse) {
        FastFourierTransformer transformer = new FastFourierTransformer(DftNormalization.STANDARD);
        Complex[] recordingFft = transformer.transform(toDoubles(recording), TransformType.FORWARD);

        int length = recordingFft.length;

        double[] matchedFilter = Arrays.copyOf(pulse.getDoubles(), length);
        Complex[] matchedFilterFft = transformer.transform(matchedFilter, TransformType.FORWARD);

        Complex[] correlationFft = new Complex[length];
        for (int i = 0; i < length; i++) {
            correlationFft[i] = matchedFilterFft[i].conjugate().multiply(recordingFft[i]);
        }
        Complex[] correlation = transformer.transform(correlationFft, TransformType.INVERSE);

        return absolutify(realParts(correlation));
    }
}
