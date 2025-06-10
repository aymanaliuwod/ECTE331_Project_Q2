import java.io.File;
import java.util.concurrent.atomic.AtomicIntegerArray;

public class MainApp {

    public static void main(String[] args) {
        String inputFile = "c:/image/Rain_Tree.jpg";
        String outputFile = "c:/image/Rain_Tree_Equalised.jpg";

        int numOfThreads = 4;  // Change for testing
        int numRuns = 3;

        ColourImage inputImage = new ColourImage();
        ImageReadWrite.readJpgImage(inputFile, inputImage);

        ColourImage outputST = new ColourImage(inputImage.width, inputImage.height);
        ColourImage outputMT1 = new ColourImage(inputImage.width, inputImage.height);
        ColourImage outputMT2 = new ColourImage(inputImage.width, inputImage.height);

        Timer timer = new Timer();

        // --- Single-threaded execution ---
        double totalST = 0;
        for (int i = 0; i < numRuns; i++) {
            timer.resetTimer();
            HistogramEqualiser.equaliseSingleThread(inputImage, outputST);
            totalST += timer.elapsedTime();
        }
        System.out.printf("Average Single-Threaded Time: %.2f ms\n", totalST / numRuns);

        // --- Multi-threaded (Shared Histogram - Atomic) ---
        double totalMT1 = 0;
        for (int i = 0; i < numRuns; i++) {
            timer.resetTimer();
            HistogramEqualiser.equaliseMultiThreadSharedAtomic(inputImage, outputMT1, numOfThreads);
            totalMT1 += timer.elapsedTime();
        }
        System.out.printf("Average Multi-Threaded (Shared Atomic) Time: %.2f ms\n", totalMT1 / numRuns);

        // --- Multi-threaded (Per-thread histogram) ---
        double totalMT2 = 0;
        for (int i = 0; i < numRuns; i++) {
            timer.resetTimer();
            HistogramEqualiser.equaliseMultiThreadPerThreadHistogram(inputImage, outputMT2, numOfThreads);
            totalMT2 += timer.elapsedTime();
        }
        System.out.printf("Average Multi-Threaded (Per-thread histograms) Time: %.2f ms\n", totalMT2 / numRuns);

        ImageReadWrite.writeJpgImage(outputMT2, outputFile);
    }
}
