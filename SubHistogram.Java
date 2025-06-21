package histogram;

import java.awt.image.BufferedImage;

/**
 * A thread that computes a histogram for a specific sub-portion of the image
 * and stores it internally for later aggregation.
 */
public class SubHistogramThread extends Thread {
    private BufferedImage sourceImage;
    private int startY; // The starting row for my sub-image.
    private int endY;   // The ending row for my sub-image (exclusive).
    private int channelToProcess; // The color channel I'm responsible for.
    private int[] subHistogram;   // My own local histogram for my assigned pixels.

    // My constructor sets up which part of the image I'll work on.
    public SubHistogramThread(BufferedImage source, int startY, int endY, int channel) {
        this.sourceImage = source;
        this.startY = startY;
        this.endY = endY;
        this.channelToProcess = channel;
        this.subHistogram = new int[256]; // I initialize my own local histogram here.
    }

    @Override
    public void run() {
        // I loop through my assigned rows and build my local 'subHistogram'.
        // No synchronization needed here because this histogram is only for me.
        for (int y = startY; y < endY; y++) {
            for (int x = 0; x < sourceImage.getWidth(); x++) {
                int rgb = sourceImage.getRGB(x, y);
                int colorValue = (rgb >> (8 * (2 - channelToProcess))) & 0xFF;
                subHistogram[colorValue]++; // I just increment my own count.
            }
        }
    }

    // This method lets the main program get my calculated sub-histogram after I'm done.
    public int[] getSubHistogram() {
        return subHistogram;
    }
}
