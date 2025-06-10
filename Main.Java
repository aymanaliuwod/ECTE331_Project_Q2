public class MainApp {

    public static void main(String[] args) {
        String inputImagePath = "c:/image/Rain_Tree.jpg";
        String outputImagePathST = "c:/image/Output_SingleThread.jpg";
        String outputImagePathMT = "c:/image/Output_MultiThread.jpg";
        int numThreads = 4;
        int repeat = 3;

        ColourImage inputImage = new ColourImage();
        ImageReadWrite.readJpgImage(inputImagePath, inputImage);

        ColourImage resultST = new ColourImage(inputImage.width, inputImage.height);
        ColourImage resultMT = new ColourImage(inputImage.width, inputImage.height);

        HistogramEqualiser equaliser = new HistogramEqualiser();

        System.out.println("Running Single Threaded Version...");
        double avgTimeST = 0;
        for (int i = 0; i < repeat; i++) {
            Timer timer = new Timer();
            equaliser.equaliseSingleThread(inputImage, resultST);
            avgTimeST += timer.elapsedTime();
        }
        avgTimeST /= repeat;
        System.out.printf("Average Time (Single Thread): %.2f ms\n", avgTimeST);

        ImageReadWrite.writeJpgImage(resultST, outputImagePathST);

        System.out.println("Running Multi Threaded Version...");
        double avgTimeMT = 0;
        for (int i = 0; i < repeat; i++) {
            Timer timer = new Timer();
            equaliser.equaliseMultiThread(inputImage, resultMT, numThreads);
            avgTimeMT += timer.elapsedTime();
        }
        avgTimeMT /= repeat;
        System.out.printf("Average Time (Multi Thread - %d threads): %.2f ms\n", numThreads, avgTimeMT);

        ImageReadWrite.writeJpgImage(resultMT, outputImagePathMT);
    }
}
