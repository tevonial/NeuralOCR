package tevonial.ocr;

import tevonial.neural.Network;

import javax.swing.*;
import java.awt.*;
import java.io.*;

/**
 * Created by Connor on 6/28/2016.
 */
public class NeuralOCR {
    private static OcrGui gui;
    private static Network net;
    private static File currentFile;

    public static void main(String[] args) {
        gui = OcrGui.createGUI();
        gui.setFile(currentFile);
        DataHandler.loadData();
        generateNetwork();
        gui.displayDigit(0);
    }

    public static void generateNetwork() {
        net = null;
        net = new Network(784, 10)
                .setHiddenLayers(1, 200)
                .build();
    }

    public static Network getNetwork() {
        return net;
    }

    public static void learn() {
        net.setLearningRate(gui.getLearningRate());

        new Thread(() -> {
            int iterations = gui.getIterations();
            double[] input = new double[784];
            double[] target = new double[10];
            byte[] bytes; int i;

            for (int iter = 0; iter < iterations; iter++) {
                i = iter % 1000;

                gui.setProgress(iter+1, iterations);
                for (int digit = 0; digit < 10; digit++) {

                    bytes = DataHandler.getData(digit, i);
                    gui.displayDigit(digit, DataHandler.renderImage(bytes));

                    for (int j = 0; j < bytes.length; j++) {
                        input[j] = bytes[j] & 0xff;
                    }
                    for (int j = 0; j < target.length; j++) {
                        target[j] = (j == digit) ? 1.0 : 0.0;
                    }
                    net.process(input, target, true, null);

                }
            }
        }).start();

    }

    public static void guess(int digit) {
        int head = (int)(Math.random() * 1000);
        byte[] data = DataHandler.getData(digit, head);
        gui.displayDigit(digit, DataHandler.renderImage(data));

        double[] input = new double[784];

        for (int j = 0; j < data.length; j++) {
            input[j] = data[j] & 0xff;
        }

        double[] output = net.process(input, null, false, digit);

        double max = 0.0; int guess = 0;
        for (int j=0; j<10; j++) {
            if (output[j] > max) {
                max = output[j];
                guess = j;
            }
        }

        gui.setGuess(guess);
    }

    public static void save(File file) {
        try {
            if (file == null) file = currentFile;
            if (!file.exists()) {
                file.createNewFile();
            }
            FileOutputStream fos = new FileOutputStream(file);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(net);

            oos.close(); fos.close();
            currentFile = file; gui.setFile(currentFile);
        } catch (IOException e) { e.printStackTrace(); }
    }


    public static void open(File file) {
        try {
            FileInputStream fis = new FileInputStream(file);
            ObjectInputStream ois = new ObjectInputStream(fis);
            net = (Network) ois.readObject();

            ois.close(); fis.close();
            currentFile = file; gui.setFile(currentFile);
        } catch (FileNotFoundException e) {
            JOptionPane.showMessageDialog(gui, "File not found", "File Error", JOptionPane.ERROR_MESSAGE);
        } catch (ClassNotFoundException e) {
            JOptionPane.showMessageDialog(gui, "File is corrupt", "File Error", JOptionPane.ERROR_MESSAGE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void testAll() {
        new Thread(() -> {
            int iterations = 1000;
            double[] input = new double[784];
            double[] target = new double[10];
            byte[] bytes;
            double max = 0.0; int guess = 0;
            int errorCount = 0;

            for (int i = 0; i < iterations; i++) {
                gui.setProgress(i+1, iterations);
                for (int digit = 0; digit < 10; digit++) {

                    bytes = DataHandler.getData(digit, i);
                    gui.displayDigit(digit, DataHandler.renderImage(bytes));

                    for (int j = 0; j < bytes.length; j++) {
                        input[j] = bytes[j] & 0xff;
                    }

                    double[] output = net.process(input, target, false, null);

                    max = 0.0;
                    for (int j=0; j<10; j++) {
                        if (output[j] > max) {
                            max = output[j];
                            guess = j;
                        }
                    }

                    if (guess != digit) {
                        errorCount++;
                    }
                }
            }
            gui.setError(((double)errorCount / ((double)iterations * 10.0)) * 100.0);
        }).start();
    }

}
