public class ColourImage {
    public int width;
    public int height;
    public short[][][] pixels;

    public ColourImage() {}

    public ColourImage(int width, int height) {
        this.width = width;
        this.height = height;
        this.pixels = new short[height][width][3];
    }
}
