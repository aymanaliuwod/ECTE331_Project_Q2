package histogram;

import java.awt.image.BufferedImage; // For image manipulation

/**
 * A thread dedicated to performing histogram equalization on a single color channel (R, G, or B).
 */
public class EqualizationThread extends Thread {
    private BufferedImage sourceImage; // The original image to read from
    private BufferedImage targetImage; // The shared image to write results to
    private int channelToProcess;      // Which channel this thread is responsible for (0=R, 1=G, 2=B)

    /**
     * Constructor for the equalization thread.
     * @param source The input image.
     * @param target The image where equalized pixels will be written (shared by threads).
     * @param channel The specific color channel this thread will process.
     */
    public EqualizationThread(BufferedImage source, BufferedImage target, int channel) {
        this.sourceImage = source;
        this.targetImage = target;
        this.channelToProcess = channel;
    }

    @Override
    public void run() {
        int width = sourceImage.getWidth();
        int height = sourceImage.getHeight();
        int totalPixels = width * height;

        int[] histogram = new int[256]; // My histogram for this thread's specific channel

        // Step 1: Computing histogram for the assigned channel.
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = sourceImage.getRGB(x, y);
                // Extracting the color component relevant to this thread's channel.
                int colorValue = (rgb >> (8 * (2 - channelToProcess))) & 0xFF;
                histogram[colorValue]++;
            }
        }

        // Step 2: Calculating cumulative histogram (CDF) and creating the lookup table.
        int[] cdf = new int[256];
        cdf[0] = histogram[0];
        for (int i = 1; i < 256; i++) {
            cdf[i] = cdf[i - 1] + histogram[i];
        }

        int maxIntensity = 255; // For 8-bit images.
        int[] lookupTable = new int[256];
        for (int i = 0; i < 256; i++) {
            // Applying the equalization formula to get new pixel mapping.
            lookupTable[i] = (int) (((double)(cdf[i] - cdf[0]) / (totalPixels - cdf[0])) * maxIntensity);
            // Ensuring values stay within 0-255.
            if (lookupTable[i] < 0) lookupTable[i] = 0;
            if (lookupTable[i] > maxIntensity) lookupTable[i] = maxIntensity;
        }

        // Step 3: Applying the new pixel mapping to the image.
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // IMPORTANT: Synchronizing on the target image. This is because multiple threads
                // are writing to the SAME image. 'synchronized' ensures only one thread writes at a time
                // to prevent data corruption (race conditions).
                synchronized (targetImage) {
                    int originalRGB = sourceImage.getRGB(x, y);
                    // Getting current R, G, B components from the pixel.
                    int r = (originalRGB >> 16) & 0xFF;
                    int g = (originalRGB >> 8) & 0xFF;
                    int b = (originalRGB >> 0) & 0xFF; 

                    // Applying the equalization ONLY to the channel this thread handles.
                    if (channelToProcess == 0) r = lookupTable[r]; 
                    else if (channelToProcess == 1) g = lookupTable[g]; 
                    else if (channelToProcess == 2) b = lookupTable[b];

                    // Reconstructing the new pixel color.
                    int newRGB = (r << 16) | (g << 8) | b;
                    targetImage.setRGB(x, y, newRGB); // Writing the updated pixel to the shared image.
                }
            }
        }
    }
}
