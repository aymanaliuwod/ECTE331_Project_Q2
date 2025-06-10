public class Timer {
    private long startTime;

    public Timer() {
        resetTimer();
    }

    public void resetTimer() {
        startTime = System.nanoTime();
    }

    public double elapsedTime() {
        return (System.nanoTime() - startTime) / 1_000_000.0;
    }
}
