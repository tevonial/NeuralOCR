package tevonial.ocr;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * Created by Connor on 6/28/2016.
 */
public class DataHandler {
    private static Data[] data = new Data[10];
    private static final int WIDTH = 28, HEIGHT = 28, SIZE = 784, SCALE_WIDTH = 100, SCALE_HEIGHT = 100;

    private static class Data {
        private byte[] bytes = new byte[SIZE * 1000];

        public Data(byte[] bytes) {
            this.bytes = bytes;
        }

        public byte[] getBytes(int head) {
            int offset = head * SIZE;
            return Arrays.copyOfRange(bytes, offset, offset + SIZE);
        }
    }

    public static BufferedImage renderImage(byte[] data) {
        int[] pixels = new int[SIZE];
        for (int i=0; i<data.length; i++) {
            pixels[i] = data[i] & 0xff;
        }

        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_BYTE_GRAY);
        WritableRaster raster = (WritableRaster) image.getRaster();
        raster.setPixels(0, 0, WIDTH, HEIGHT, pixels);

        Image tmp = image.getScaledInstance(SCALE_WIDTH, SCALE_HEIGHT, Image.SCALE_SMOOTH);
        BufferedImage scaleImage = new BufferedImage(SCALE_WIDTH, SCALE_HEIGHT, BufferedImage.TYPE_BYTE_GRAY);

        Graphics2D g2d = scaleImage.createGraphics();
        g2d.drawImage(tmp, 0, 0, SCALE_WIDTH, SCALE_HEIGHT, null);
        g2d.dispose();

        return scaleImage;
    }

    public static BufferedImage getImage(int digit) {
        return getImage(digit, 0);
    }

    public static BufferedImage getImage(int digit, int head) {
        if (digit >= 0 && digit <= 9) {
            byte[] temp = data[digit].getBytes(head);
            if (temp.length == SIZE) {
                return renderImage(temp);
            } else {
                System.err.println("temp.length = " + temp.length);
            }
        }
        return null;
    }

    public static void loadData() {
        byte[] temp = new byte[SIZE];
        for (int i = 0; i< data.length; i++){
            Path path = Paths.get("data/data" + i);
            try {
                temp = Files.readAllBytes(path);
                System.out.println("Loaded " + path.toString());
            } catch (IOException e) {
                System.err.println("Error loading " + path.toString());
            }
            data[i] = new Data(temp);
        }
    }

    public static byte[] getData(int digit, int head) {
        return data[digit].getBytes(head);
    }
}
