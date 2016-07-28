package tevonial.ocr;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import tevonial.neural.*;
import tevonial.ocr.gui.Ocr;

import javax.swing.*;
import java.io.*;

public class NeuralOCR {
    private static Ocr gui;
    private static Network net;
    private static File currentFile;

    private static Kryo kryo;

    private static DataHandler.Data trainingData, testData;

    public static void main(String[] args) {
        gui = Ocr.createGUI();
        gui.setFile(currentFile);
        trainingData = DataHandler.loadData("data/train-labels", "data/train-images");
        testData = DataHandler.loadData("data/test-labels", "data/test-images");

        generateNetwork();

        kryo = new Kryo();
        kryo.setRegistrationRequired(false);

        guess(0);
    }

    public static void generateNetwork() {
        //net = Network.buildFullyConnectedNetwork(784, 10, 1, 350);
        net = Network.buildConvolutionalNetwork(22, 5, 10);
    }

    public static Network getNetwork() {
        return net;
    }

    public static void learn() {
        net.setLearningRate(gui.getLearningRate());

        new Thread(() -> {
            int iterations = gui.getIterations();
            int length = trainingData.getLength();
            double[] input = new double[784];
            double[] target = new double[10];
            byte[] bytes; int digit;

            for (int i = 0; i < iterations; i++) {
                if (i % length == 0) trainingData.reset();

                digit = trainingData.next();
                bytes = trainingData.getImageBytes();

                gui.displayDigit(digit, DataHandler.renderImage(bytes));

                for (int j = 0; j < bytes.length; j++) {
                    input[j] = bytes[j] & 0xff;
                }
                for (int j = 0; j < target.length; j++) {
                    target[j] = (j == digit) ? 1.0 : 0.0;
                }
                net.process(input, target, true, null);

                gui.setProgress(i+1, iterations);
            }
        }).start();

    }

    public static void guess(int digit) {
        int rand = (int)(Math.random() * testData.getLength());
        while (testData.getLabel(rand) != digit) {
            rand++;
        }
        byte[] data = testData.getImageBytes(rand);

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

            Output output = new Output(fos);
            kryo.writeClassAndObject(output, net);

            output.close();

            fos.close();
            currentFile = file; gui.setFile(currentFile);
        } catch (IOException e) { e.printStackTrace(); }
    }


    public static void open(File file) {
        try {
            FileInputStream fis = new FileInputStream(file);

            Input input = new Input(fis);
            net = (Network) kryo.readClassAndObject(input);
            input.close();

            fis.close();
            currentFile = file; gui.setFile(currentFile);

        } catch (FileNotFoundException e) {
            JOptionPane.showMessageDialog(gui, "File not found", "File Error", JOptionPane.ERROR_MESSAGE);
        } catch (KryoException e) {
            JOptionPane.showMessageDialog(gui, "File is corrupt", "File Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void testAll() {
        new Thread(() -> {
            int iterations = testData.getLength();
            double[] input = new double[784];
            double[] target = new double[10];
            byte[] bytes; int digit;
            double max = 0.0; int guess = 0;
            int errorCount = 0;
            testData.reset();

            for (int i = 0; i < iterations; i++) {
                gui.setProgress(i+1, iterations);

                digit = testData.next();
                bytes = testData.getImageBytes();

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
            gui.setError(((double)errorCount / ((double)iterations)) * 100.0);
        }).start();
    }
}
