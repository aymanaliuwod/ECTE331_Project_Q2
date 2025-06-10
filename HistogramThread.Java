public class HistogramThread implements Runnable {
    private final short[] input;
    private final int start, end;
    private final int[] hist;

    public HistogramThread(short[] input, int start, int end, int[] hist) {
        this.input = input;
        this.start = start;
        this.end = end;
        this.hist = hist;
    }

    @Override
    public void run() {
        for (int i = start; i < end; i++) {
            hist[input[i]]++;
        }
    }
}
