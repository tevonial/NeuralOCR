package tevonial.ocr;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class DataHandler {
    private static final int WIDTH = 28, HEIGHT = 28, SIZE = 784, SCALE_WIDTH = 100, SCALE_HEIGHT = 100;

    public static BufferedImage renderImage(byte[] data) {
        return renderImage(data, SCALE_WIDTH, SCALE_HEIGHT);
    }

    public static BufferedImage renderImage(byte[] data, int w, int h) {
        int[] pixels = new int[SIZE];
        for (int i = 0; i < data.length; i++) {
            pixels[i] = data[i] & 0xff;
        }

        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_BYTE_GRAY);
        WritableRaster raster = (WritableRaster) image.getRaster();
        raster.setPixels(0, 0, WIDTH, HEIGHT, pixels);

        Image tmp = image.getScaledInstance(w, h, Image.SCALE_SMOOTH);
        BufferedImage scaleImage = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);

        Graphics2D g2d = scaleImage.createGraphics();
        g2d.drawImage(tmp, 0, 0, w, h, null);
        g2d.dispose();

        return scaleImage;
    }

    public static Data loadCustomData() {
        byte[][] temp = new byte[10][]; int length = 0;

        for (int i = 0; i<10; i++) {
            Path path = Paths.get("data/custom/data" + i);
            try {
                temp[i] = Files.readAllBytes(path);
                if (temp[i].length < length || length == 0)
                    length = temp[i].length;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        length /= 784;

        ByteArrayOutputStream img_stream = new ByteArrayOutputStream();
        ByteArrayOutputStream lbl_stream = new ByteArrayOutputStream();

        try {
            img_stream.write(new byte[2]);
            img_stream.write(ByteBuffer.allocate(2).put((byte)8).put((byte)3).array());
            lbl_stream.write(new byte[2]);
            lbl_stream.write(ByteBuffer.allocate(2).put((byte)8).put((byte)1).array());
            byte[] l = ByteBuffer.allocate(4).putInt(length).array();
            img_stream.write(l);
            lbl_stream.write(l);
            img_stream.write(ByteBuffer.allocate(8).putInt(28).putInt(28).array());

        } catch (IOException e) {}

        for (int j=0; j<length; j++) {
            for (int k=0; k<10; k++) {
                try {
                    img_stream.write(Arrays.copyOfRange(temp[k], j*784, (j+1)*784));
                    lbl_stream.write((byte)k);
                } catch (IOException e) {}
            }
        }

        return new Data(lbl_stream.toByteArray(), img_stream.toByteArray());
    }


    public static Data loadData(String label, String image) {
        Path lblpath = Paths.get(label);
        Path imgpath = Paths.get(image);

        try {
            return new Data(Files.readAllBytes(lblpath), Files.readAllBytes(imgpath));
        } catch(IOException e) {
            e.printStackTrace();
            return null;

        }
    }

    public static class Data {
        private int fileSize;
        private byte[] image;
        private byte[] label;
        private int head = -1;
        private int SIZE;
        private int total;

        public Data(byte[] label_raw, byte[] image_raw) {
            fileSize = image_raw.length;
            total = ByteBuffer.wrap(Arrays.copyOfRange(image_raw, 4, 8)).getInt();
            SIZE = ByteBuffer.wrap(Arrays.copyOfRange(image_raw, 8, 12)).getInt()
                    * ByteBuffer.wrap(Arrays.copyOfRange(image_raw, 12, 16)).getInt();
            image = Arrays.copyOfRange(image_raw, 4 + (image_raw[3] * 4), image_raw.length);

            if (label_raw != null) {
                int label_size = ByteBuffer.wrap(Arrays.copyOfRange(label_raw, 4, 8)).getInt();
                total = (label_size < total) ? label_size : total;
                label = Arrays.copyOfRange(label_raw, 4 + (label_raw[3] * 4), image_raw.length);
            }

        }

        public int getFileSize() {
            return fileSize;
        }

        public int getLength() {
            return total;
        }

        public void reset() {
            head = -1;
        }

        public int next() {
            return label[++head];
        }

        public int getLabel(int pos) {
            return label[pos];
        }

        public byte[] getImageBytes() {
            return getImageBytes(head);
        }

        public byte[] getImageBytes(int pos) {
            return Arrays.copyOfRange(image, pos * SIZE, (pos + 1) * SIZE);
        }
    }
}
