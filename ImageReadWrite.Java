import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class ImageReadWrite {

    public static void readJpgImage(String fileName, ColourImage img) {
        try {
            File file = new File(fileName);
            BufferedImage image = ImageIO.read(file);

            img.width = image.getWidth();
            img.height = image.getHeight();
            img.pixels = new short[img.height][img.width][3];

            for (int y = 0; y < img.height; y++) {
                for (int x = 0; x < img.width; x++) {
                    Color color = new Color(image.getRGB(x, y));
                    img.pixels[y][x][0] = (short) color.getRed();
                    img.pixels[y][x][1] = (short) color.getGreen();
                    img.pixels[y][x][2] = (short) color.getBlue();
                }
            }

        } catch (IOException e) {
            System.out.println("Error reading image: " + e.getMessage());
        }
    }

    public static void writeJpgImage(ColourImage img, String fileName) {
        try {
            BufferedImage image = new BufferedImage(img.width, img.height, BufferedImage.TYPE_INT_RGB);

            for (int y = 0; y < img.height; y++) {
                for (int x = 0; x < img.width; x++) {
                    int r = img.pixels[y][x][0];
                    int g = img.pixels[y][x][1];
                    int b = img.pixels[y][x][2];
                    image.setRGB(x, y, new Color(r, g, b).getRGB());
                }
            }

            ImageIO.write(image, "jpg", new File(fileName));

        } catch (IOException e) {
            System.out.println("Error writing image: " + e.getMessage());
        }
    }
}
