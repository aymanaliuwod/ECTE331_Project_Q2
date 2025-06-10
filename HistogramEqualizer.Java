import java.util.concurrent.atomic.AtomicIntegerArray;

public class HistogramEqualiser {

    private final int MAX_INTENSITY = 256;

    public void equaliseSingleThread(ColourImage input, ColourImage output) {
        for (int channel = 0; channel < 3; channel++) {
            short[] inputChannel = extractChannel(input, channel);
            short[] equalised = equaliseChannel(inputChannel);
            insertChannel(output, equalised, channel, input.width, input.height);
        }
    }

    public void equaliseMultiThread(ColourImage input, ColourImage output, int numThreads) {
        for (int channel = 0; channel < 3; channel++) {
            short[] inputChannel = extractChannel(input, channel);
            short[] equalised = equaliseChannelMultithread(inputChannel, numThreads);
            insertChannel(output, equalised, channel, input.width, input.height);
        }
    }

    private short[] extractChannel(ColourImage img, int channel) {
        int size = img.width * img.height;
        short[] vect = new short[size];
        for (int y = 0; y < img.height; y++) {
            for (int x = 0; x < img.width; x++) {
                vect[y * img.width + x] = img.pixels[y][x][channel];
            }
        }
        return vect;
    }

    private void insertChannel(ColourImage img, short[] channelData, int channel, int width, int height) {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                img.pixels[y][x][channel] = channelData[y * width + x];
            }
        }
    }

    private short[] equaliseChannel(short[] input) {
        int[] hist = new int[MAX_INTENSITY];
        int[] cdf = new int[MAX_INTENSITY];
        short[] output = new short[input.length];

        for (short val : input) hist[val]++;
        cdf[0] = hist[0];
        for (int i = 1; i < MAX_INTENSITY; i++) cdf[i] = cdf[i - 1] + hist[i];
        for (int i = 0; i < MAX_INTENSITY; i++)
            cdf[i] = (cdf[i] * (MAX_INTENSITY - 1)) / input.length;
        for (int i = 0; i < input.length; i++)
            output[i] = (short) cdf[input[i]];

        return output;
    }

    private short[] equaliseChannelMultithread(short[] input, int numThreads) {
        int[][] subHist = new int[numThreads][MAX_INTENSITY];
        Thread[] threads = new Thread[numThreads];
        int size = input.length;
        int chunk = size / numThreads;

        for (int i = 0; i < numThreads; i++) {
            int start = i * chunk;
            int end = (i == numThreads - 1) ? size : (start + chunk);
            threads[i] = new Thread(new HistogramThread(input, start, end, subHist[i]));
            threads[i].start();
        }

        try {
            for (Thread t : threads) t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        int[] totalHist = new int[MAX_INTENSITY];
        for (int i = 0; i < numThreads; i++)
            for (int j = 0; j < MAX_INTENSITY; j++)
                totalHist[j] += subHist[i][j];

        int[] cdf = new int[MAX_INTENSITY];
        cdf[0] = totalHist[0];
        for (int i = 1; i < MAX_INTENSITY; i++) cdf[i] = cdf[i - 1] + totalHist[i];
        for (int i = 0; i < MAX_INTENSITY; i++)
            cdf[i] = (cdf[i] * (MAX_INTENSITY - 1)) / size;

        short[] output = new short[size];
        for (int i = 0; i < size; i++) {
            output[i] = (short) cdf[input[i]];
        }
        return output;
    }
}
