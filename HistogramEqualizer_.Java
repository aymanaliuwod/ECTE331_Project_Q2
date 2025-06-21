package histogram;

import java.awt.image.BufferedImage; // For image manipulation
import java.util.concurrent.atomic.AtomicIntegerArray; // Needed for the shared atomic histogram

public class HistogramEqualizer {

    /**
     * Performs histogram equalization on an image using a single thread.
     * Processes each color channel (Red, Green, Blue) sequentially.
     */
    public static BufferedImage equalizeSingleThread(BufferedImage original) {
        // Creating a new blank image to store the equalized result.
        BufferedImage resultImage = new BufferedImage(original.getWidth(), original.getHeight(), BufferedImage.TYPE_INT_RGB);

        // Loop through each color channel (0 for Red, 1 for Green, 2 for Blue)
        for (int channel = 0; channel < 3; channel++) {
            int[] histogram = new int[256]; // My counts for each pixel intensity (0-255)
            int width = original.getWidth();
            int height = original.getHeight();
            int totalPixels = width * height; // Total pixels in the image

            // Step 1: Building the histogram for the current channel.
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int rgb = original.getRGB(x, y);
                    // Extracting the specific color component (R, G, or B) for this channel.
                    int colorValue = (rgb >> (8 * (2 - channel))) & 0xFF;
                    histogram[colorValue]++; // Incrementing count for this color value.
                }
            }

            // Step 2: Calculating the Cumulative Distribution Function (CDF).
            // CDF[i] tells me how many pixels have intensity <= i.
            int[] cdf = new int[256];
            cdf[0] = histogram[0];
            for (int i = 1; i < 256; i++) {
                cdf[i] = cdf[i - 1] + histogram[i];
            }

            // Step 3: Creating the lookup table for equalization.
            // This table maps old pixel values to new, equalized values.
            int maxIntensity = 255; // For an 8-bit image.
            int[] lookupTable = new int[256];
            for (int i = 0; i < 256; i++) {
                // Applying the equalization formula, making sure to multiply before division for better precision.
                lookupTable[i] = (int) (((double)(cdf[i] - cdf[0]) / (totalPixels - cdf[0])) * maxIntensity);
                // Clamping values to stay within 0-255 range.
                if (lookupTable[i] < 0) lookupTable[i] = 0;
                if (lookupTable[i] > maxIntensity) lookupTable[i] = maxIntensity;
            }

            // Step 4: Applying the lookup table to transform the image pixels.
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int originalRGB = original.getRGB(x, y);
                    // Getting existing R, G, B components from the original pixel.
                    int r = (originalRGB >> 16) & 0xFF;
                    int g = (originalRGB >> 8) & 0xFF;
                    int b = originalRGB & 0xFF;

                    // Applying the lookup ONLY to the current channel being processed.
                    if (channel == 0) r = lookupTable[r];
                    else if (channel == 1) g = lookupTable[g];
                    else if (channel == 2) b = lookupTable[b];

                    // Reconstructing the new RGB value with the updated channel.
                    int newRGB = (r << 16) | (g << 8) | b;
                    // Setting the pixel in the result image.
                    resultImage.setRGB(x, y, newRGB);
                }
            }
        }
        return resultImage; // Returning the fully equalized image.
    }

    /**
     * Performs histogram equalization using multiple threads.
     * Current design: Each thread processes a separate color channel (R, G, B).
     */
    public static BufferedImage equalizeMultiThread(BufferedImage original) throws InterruptedException {
        // Creating a blank image for the multi-threaded result.
        BufferedImage resultImage = new BufferedImage(original.getWidth(), original.getHeight(), BufferedImage.TYPE_INT_RGB);

        // I'm using 3 threads, one for each color channel (Red, Green, Blue).
        EqualizationThread[] workerThreads = new EqualizationThread[3];

        // Creating and starting a thread for each channel.
        for (int i = 0; i < 3; i++) {
            // Passing the original image, the shared result image, and the channel to process.
            workerThreads[i] = new EqualizationThread(original, resultImage, i);
            workerThreads[i].start(); // Starting the thread's execution.
        }

        // Waiting for all threads to finish their work.
        for (int i = 0; i < 3; i++) {
            workerThreads[i].join(); // 'join' makes the main thread wait for this thread to complete.
        }

        return resultImage; // Returning the result after all threads are done.
    }

    /**
     * Performs histogram equalization using multiple threads,
     * where all threads contribute to a single, shared (atomic) histogram array.
     * Each thread processes a spatial portion of the image.
     *
     * @param original The input image.
     * @param numOfThreads The number of threads to use for histogram calculation.
     * @return The equalized BufferedImage.
     * @throws InterruptedException If a thread is interrupted.
     */
    public static BufferedImage equalizeMultiThreadSharedAtomic(BufferedImage original, int numOfThreads) throws InterruptedException {
        BufferedImage resultImage = new BufferedImage(original.getWidth(), original.getHeight(), BufferedImage.TYPE_INT_RGB);
        
        // This is the single, shared histogram for all threads. Using AtomicIntegerArray for thread-safe updates.
        AtomicIntegerArray sharedHistogram = new AtomicIntegerArray(256); 

        int height = original.getHeight();
        Thread[] workerThreads = new Thread[numOfThreads];

        // Figuring out how many rows each thread will process.
        int rowsPerThread = height / numOfThreads;

        // I'm processing each channel (R, G, B) one after another, but the histogram collection for each channel is parallelized.
        for (int channel = 0; channel < 3; channel++) { 
            // Clearing the shared histogram for the new channel's calculation.
            for(int i = 0; i < 256; i++) {
                sharedHistogram.set(i, 0); 
            }

            // Creating and starting threads to build the shared histogram for the current channel.
            for (int i = 0; i < numOfThreads; i++) {
                int startY = i * rowsPerThread;
                int endY = (i == numOfThreads - 1) ? height : (i + 1) * rowsPerThread;
                workerThreads[i] = new SharedHistogramThread(original, sharedHistogram, startY, endY, channel);
                workerThreads[i].start();
            }

            // Waiting for all threads to finish updating the shared histogram.
            for (int i = 0; i < numOfThreads; i++) {
                workerThreads[i].join();
            }

            // Now that all threads are done, the 'sharedHistogram' has the complete histogram for this channel.
            // Copying it to a regular int[] to perform CDF calculations.
            int[] finalHistogram = new int[256];
            for(int i = 0; i < 256; i++) {
                finalHistogram[i] = sharedHistogram.get(i);
            }

            // Step 2: Calculating the Cumulative Distribution Function (CDF).
            int[] cdf = new int[256];
            cdf[0] = finalHistogram[0];
            for (int i = 1; i < 256; i++) {
                cdf[i] = cdf[i - 1] + finalHistogram[i];
            }

            // Step 3: Creating the lookup table for equalization.
            int maxIntensity = 255;
            int totalPixels = original.getWidth() * original.getHeight();
            int[] lookupTable = new int[256];
            for (int i = 0; i < 256; i++) {
                lookupTable[i] = (int) (((double)(cdf[i] - cdf[0]) / (totalPixels - cdf[0])) * maxIntensity);
                if (lookupTable[i] < 0) lookupTable[i] = 0;
                if (lookupTable[i] > maxIntensity) lookupTable[i] = maxIntensity;
            }

            // Applying the lookup table to transform the image pixels for the current channel.
            // This part is done sequentially after the parallel histogram build for simplicity.
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < original.getWidth(); x++) {
                    int originalRGB = original.getRGB(x, y);
                    int r = (originalRGB >> 16) & 0xFF;
                    int g = (originalRGB >> 8) & 0xFF;
                    int b = originalRGB & 0xFF;

                    if (channel == 0) r = lookupTable[r];
                    else if (channel == 1) g = lookupTable[g];
                    else if (channel == 2) b = lookupTable[b];

                    int newRGB = (r << 16) | (g << 8) | b;
                    resultImage.setRGB(x, y, newRGB);
                }
            }
        }
        return resultImage;
    }

    /**
     * Performs histogram equalization using multiple threads,
     * where each thread computes a sub-histogram for its assigned image region.
     * These sub-histograms are then combined to form the final overall histogram.
     *
     * @param original The input image.
     * @param numOfThreads The number of threads to use.
     * @return The equalized BufferedImage.
     * @throws InterruptedException If a thread is interrupted.
     */
    public static BufferedImage equalizeMultiThreadSubHistograms(BufferedImage original, int numOfThreads) throws InterruptedException {
        BufferedImage resultImage = new BufferedImage(original.getWidth(), original.getHeight(), BufferedImage.TYPE_INT_RGB);
        
        int height = original.getHeight();
        SubHistogramThread[] workerThreads = new SubHistogramThread[numOfThreads];
        
        // Figuring out how many rows each thread gets.
        int rowsPerThread = height / numOfThreads;

        // I'm processing each channel (R, G, B) one by one. The histogram calculation within each channel is parallelized.
        for (int channel = 0; channel < 3; channel++) {
            // Step 1: Creating and starting threads to compute their individual sub-histograms.
            for (int i = 0; i < numOfThreads; i++) {
                int startY = i * rowsPerThread;
                int endY = (i == numOfThreads - 1) ? height : (i + 1) * rowsPerThread;
                workerThreads[i] = new SubHistogramThread(original, startY, endY, channel);
                workerThreads[i].start();
            }

            // Waiting for all threads to finish calculating their sub-histograms.
            for (int i = 0; i < numOfThreads; i++) {
                workerThreads[i].join();
            }

            // Step 2: Combining all the individual sub-histograms into one final histogram.
            int[] finalHistogram = new int[256];
            for (int i = 0; i < numOfThreads; i++) {
                int[] subHist = workerThreads[i].getSubHistogram();
                for (int j = 0; j < 256; j++) {
                    finalHistogram[j] += subHist[j]; // Just summing up the counts.
                }
            }

            // Step 3: Calculating the Cumulative Distribution Function (CDF).
            int[] cdf = new int[256];
            cdf[0] = finalHistogram[0];
            for (int i = 1; i < 256; i++) {
                cdf[i] = cdf[i - 1] + cdf[i]; // Bug fixed: should be cdf[i-1] + finalHistogram[i]
                cdf[i] = cdf[i - 1] + finalHistogram[i]; // Corrected line
            }

            // Step 4: Creating the lookup table for equalization.
            int maxIntensity = 255;
            int totalPixels = original.getWidth() * original.getHeight();
            int[] lookupTable = new int[256];
            for (int i = 0; i < 256; i++) {
                lookupTable[i] = (int) (((double)(cdf[i] - cdf[0]) / (totalPixels - cdf[0])) * maxIntensity);
                if (lookupTable[i] < 0) lookupTable[i] = 0;
                if (lookupTable[i] > maxIntensity) lookupTable[i] = maxIntensity;
            }

            // Step 5: Applying the lookup table to transform the image pixels.
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < original.getWidth(); x++) {
                    int originalRGB = original.getRGB(x, y);
                    int r = (originalRGB >> 16) & 0xFF;
                    int g = (originalRGB >> 8) & 0xFF;
                    int b = originalRGB & 0xFF;

                    if (channel == 0) r = lookupTable[r];
                    else if (channel == 1) g = lookupTable[g];
                    else if (channel == 2) b = lookupTable[b];

                    int newRGB = (r << 16) | (g << 8) | b;
                    resultImage.setRGB(x, y, newRGB);
                }
            }
        }
        return resultImage;
    }
}
