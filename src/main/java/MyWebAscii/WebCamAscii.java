package MyWebAscii;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import com.github.sarxos.webcam.Webcam;

import javax.imageio.ImageIO;


public class WebCamAscii {
    
    private static final String ascii = ".`\":I!>~_?[{)|/frnvULOwpbho#W8B$";

    public WebCamAscii() {

    }

    /**
     * Loads an image from its path relative to the working directory.
     * @param imgPath The relative path of the image compared to the working directory.
     * @return the image.
     */
    public static BufferedImage loadImage(String imgPath) {
        String path = new File("").getAbsolutePath();
        System.out.println(path);
        try {
            BufferedImage img = ImageIO.read(new File(path + "\\" + imgPath));
            return img;
        }
        catch (IOException e) {
            System.out.println("Image failed to load; exception: " + e);
        }
        return null;
    }

    /**
     * Takes an image and returns a (potentially downscaled) ascii visual string of the image.
     * @param img the image you wish to turn into ascii.
     * @return an ascii representation of an image.
     * @throws InterruptedException
     */
    public static StringBuilder getAsciiFromImage(BufferedImage img) throws InterruptedException {
        return getAsciiFromImage(img, 1);
    }

    /**
     * Takes an image and returns a (potentially downscaled) ascii visual string of the image.
     * @param img the image you wish to turn into ascii.
     * @param scale the downscaling factor.
     * @return an ascii representation of an image.
     * @throws InterruptedException
     */
    public static StringBuilder getAsciiFromImage(BufferedImage img, int scale) throws InterruptedException {
        int width = img.getWidth();
        int height = img.getHeight();
        int cols = width / scale;
        int rows = height / scale;

        Queue <int []> q = new ConcurrentLinkedQueue<int []>();
        for (int x = 0; x < cols; x++) {
            for (int y = 0; y < rows; y++) {
                int [] arr = {x, y};
                q.add(arr);
            }
        }

        StringBuilder str = new StringBuilder();
        str.setLength(rows * (2 * cols + 1));
        int threads = 12;
        CountDownLatch latch = new CountDownLatch(threads);
        for (int t = 0; t < threads; t++) {
            Thread thread = new Thread() {
                public void run() {
                    int [] arr = q.poll();
                    while (arr != null) {
                        int x = arr[0];
                        int y = arr[1];
                        int pixel = averagePixel(img, scale, x, y);
                        int index = pixel / 32;
                        char c = ascii.charAt(index);
                        index = y * (2 * cols) + 2 * x;
                        str.setCharAt(index, c);
                        index = y * (2 * cols) + 2 * x + 1;
                        str.setCharAt(index, ' ');
                        arr = q.poll();
                    }
                    latch.countDown();
                }
            };
            thread.start();
        }
        latch.await();
        for (int y = 1; y < rows; y++) {
            int index = y * (2 * cols) - 1;
            str.setCharAt(index, '\n');
        }
        return str;
    }

    /**
     * 
     * @param img the image.
     * @param scale the downscaling factor.
     * @param i the upper left x coordinate of this partition of the image.
     * @param j the upper left y coordinate of this partition of the image.
     * @return the average grayscale pixel value of this partition of the image.
     */
    public static int averagePixel(BufferedImage img, int scale, int i, int j) {
        int pixel = 0;
        for (int x = 0; x < scale; x++) {
            for (int y = 0; y < scale; y++) {
                int pxl = img.getRGB(i * scale + x, j * scale + y);
                Color color = new Color(pxl);
                int r = color.getRed();
                int g = color.getGreen();
                int b = color.getBlue();
                pixel += (int) (r * 0.30 + g * 0.59 + b * 0.11);
            }
        }
        pixel /= scale * scale;
        return pixel;
    }


    /**
     * Opens the default webcam of the operating system, captures an image,
     * then converts that image into a 1-to-1 ascii representation, and prints it.
     * To properly use this file, use a terminal, navigate to the project folder, and
     * use the following command: "mvn exec:java -Dexec.mainClass="MyWebAscii.WebCamAscii"".
     * Make sure to change font size so that you can see the image properly.
     */
    public static void main(String [] args) throws InterruptedException {
        Webcam webcam = Webcam.getDefault();
        webcam.open();
        while (true) {
            BufferedImage capture = webcam.getImage();
            StringBuilder str = getAsciiFromImage(capture);
            System.out.print("\033[H\033[2J");  
            System.out.flush();
            System.out.println(str);
        }
    }
}
