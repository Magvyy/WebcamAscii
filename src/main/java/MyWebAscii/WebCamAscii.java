package MyWebAscii;
import java.awt.AWTException;
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

    public static BufferedImage loadImage(String imgName) {
        String path = new File("").getAbsolutePath();
        System.out.println(path);
        try {
            BufferedImage img = ImageIO.read(new File(path + "\\" + imgName));
            return img;
        }
        catch (IOException e) {
            System.out.println("Image failed to load; exception: " + e);
        }
        return null;
    }

    public static StringBuilder printAsciiImage(BufferedImage img, int scale) throws IOException, InterruptedException {
        int width = img.getWidth();
        int height = img.getHeight();
        // System.out.println(width + " : " + height);
        // return new StringBuilder();
        int cols = width / scale;
        int rows = height / scale;

        Queue <int []> q = new ConcurrentLinkedQueue<int []>();
        for (int x = 0; x < cols; x++) {
            for (int y = 0; y < rows; y++) {
                int [] arr = {x, y};
                q.add(arr);
            }
        }

        // StringBuilder str = new StringBuilder();
        // str.setLength(rows * (cols + 1));
        // for (int y = 1; y < rows; y++) {
        //     int index = y * (cols + 1) - 1;
        //     str.setCharAt(index, '\n');
        // }

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


    public static void main(String [] args) throws IOException, InterruptedException, AWTException {
        Webcam webcam = Webcam.getDefault();
        webcam.open();
        while (true) {
            BufferedImage capture = webcam.getImage();
            StringBuilder str = printAsciiImage(capture, 1);
            System.out.print("\033[H\033[2J");  
            System.out.flush();
            System.out.println(str);
        }


        // asciiImage(mcImg, scale, file + ".txt");
        // printAsciiImage(mcImg, 1);
    }
}
