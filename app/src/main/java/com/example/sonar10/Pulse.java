package com.example.sonar10;

import java.util.ArrayList;

abstract class Pulse {
    static final int SAMPLE_RATE = 48000;
    int LENGTH;
    int size() {
        return LENGTH;
    }
    double duration() {
        return (double) LENGTH / SAMPLE_RATE;
    }
    abstract double[] getDoubles();

    public short[] getShorts(boolean stereo) {
        double[] doubles = this.getDoubles();
        short[] res = new short[stereo ? 2*size() : size()];
        for (int i = 0; i < size(); i++) {
            if (stereo) {
                res[2*i] = 0;
                res[2*i+1] = (short) Math.round(Short.MAX_VALUE * doubles[i]);
            }
            else
                res[i] = (short) Math.round(Short.MAX_VALUE * doubles[i]);
        }
        return res;
    }
}

class LinearChirp extends Pulse {
    static final int INITIAL_FREQ = 23000;
    static final int FINAL_FREQ = 8000;
    {
        LENGTH = 4000;
    }

    public double[] getDoubles() {
        final double sweepRate = (double) (FINAL_FREQ - INITIAL_FREQ) / duration();
        double[] res = new double[size()];
        for (int i = 0; i < res.length; i++) {
            double t = (double) i / (SAMPLE_RATE);
            double phase = 2.*Math.PI * (.5*sweepRate*t*t + INITIAL_FREQ*t);
            double amp = Math.pow(Math.sin(Math.PI*i/(res.length-1)), 2); // Hann window
            res[i] = amp * Math.sin(phase);
        }
        return res;
    }
}

class PhaseMod extends Pulse {
    static final int FREQ = 20000;

    private static byte[] decode(String str, int n) {
        ArrayList<Integer> arr = new ArrayList<>();
        for (char ch: str.toCharArray()) {
            int val = Integer.parseInt(""+ch, 16);
            arr.add((val>>3)%2);
            arr.add((val>>2)%2);
            arr.add((val>>1)%2);
            arr.add((val)%2);
        }

        while (arr.size() > n) {
            arr.remove(0);
        }

        byte[] res = new byte[n];
        int idx = 0;
        for (int value: arr) {
            res[idx++] = value==1 ? (byte)1 : (byte)-1;
        }
        return res;
    }

    final int CYCLE = 12;//6;
    final int N_CYCLES;
    private final byte[] CODE;
    private final int SLOT;
    {
        LENGTH = 4096*CYCLE;
        N_CYCLES = LENGTH / CYCLE;

        String seq =
                "3F1A35559CE9C8AB583A1281BE032FE062B43DB6860E047B72391444E64F1184424D449E2D318F60CA5A4B5E59480A4E72D52A883AF66D2CA315A6EF623FA587" +
                "69D47BE594C882C66F8DC195422D2A95E5B42F74E2F1B70ECC1D3308C04B59327B49623680948A0F46C0BCABDB140F843CE8C712011FB99FA5F5907BCE81531F" +
                "D06610F0C9EB39E0FE20851A5C21F7003B7AA22882BB32D45060CBB76C6F1FE752E90848EC8E1CC72806357AA338C682269D8E17B745777BE0639ACADE450D41" +
                "21398C650664443E1F0A8BC1F8B73979BB973C6457070F33D52B02BDF47FBE6A6D98B8F22D32E063147308F89E3A7B7EA56768FDDE5AFCF4E71322BF8AAB1806" +
                "D11A2BBAFB58244558DCF950853E68327CFF9E835BEE4F995F245C9A95407FAB4540DF3D7ACC9965D900C8EF3014CB70A14AAA778253519019458B9B43DAAA2F" +
                "DCA34DE28C6E8A7FFB0D5B1ADFA30E68E0D2287486C06C14AFC8147ADF92CEC991B601FB283C1FD37A9A6C9B45C90921E88BBDB6DBFACF462C3FF21A87AB9A2D" +
                "B044C38CA34FFAF06E28F6AA08A4A224896D0E739DB1BB0512076D81B5D39218A57588A0BDFD6795BF1AB44B269DE918D9AD196EE1AEC1E6335C6A4B343D15D6" +
                "4A2D535B9EAF336CC363C093159F2AFC8644940F793BF477CA55110B8267BD36DA77837148469E3770BF2497801535997ADBE9F8C591EB2CD3D687E2B70D882E";

        CODE = decode(seq, 4096);
        SLOT = N_CYCLES / CODE.length;
    }

    @Override
    public double[] getDoubles() {
        byte s = 1;
        double[] res = new double[size()];
        for (int i = 0; i < size(); i++) {
            double t = (double) i / SAMPLE_RATE;

            if (i%(CYCLE*SLOT) == 0)
                s = CODE[i/(CYCLE*SLOT)];

            double halfLen = size()/2.;
            double amp = i < 200            ? i/200. :
                         i > LENGTH-1 - 200 ? (LENGTH-1-i)/200. :
                                                1.;

            res[i] = s * amp * Math.sin(2.*Math.PI*FREQ*t);
        }
        return res;
    }

}
