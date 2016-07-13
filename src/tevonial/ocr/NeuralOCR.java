package tevonial.ocr;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import tevonial.neural.Layer;
import tevonial.neural.Network;
import tevonial.neural.Neuron;
import tevonial.ocr.gui.Ocr;

import javax.swing.*;
import java.io.*;

public class NeuralOCR {
    private static Ocr gui;
    private static Network net;
    private static File currentFile;

    private static Kryo kryo;

    private static int MAX;

    public static void main(String[] args) {
        gui = Ocr.createGUI();
        gui.setFile(currentFile);
        MAX = DataHandler.loadData();
        System.out.println(MAX);

        generateNetwork();
        gui.displayDigit(0);

        kryo = new Kryo();

        kryo.register(Network.class, 0);
        kryo.register(Layer.class, 1);
        kryo.register(Neuron.class, 2);
    }

    public static void generateNetwork() {
        net = null;
        net = new Network(784, 10)
                .setHiddenLayers(1, 350)
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
                i = iter % MAX;

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
        int head = (int)(Math.random() * MAX);
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
            int iterations = MAX;
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


    public static class DecompressibleInputStream extends ObjectInputStream {

        public DecompressibleInputStream(InputStream in) throws IOException {
            super(in);
        }

        protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException {
            ObjectStreamClass resultClassDescriptor = super.readClassDescriptor(); // initially streams descriptor
            Class localClass; // the class in the local JVM that this descriptor represents.
            try {
                localClass = Class.forName(resultClassDescriptor.getName());
            } catch (ClassNotFoundException e) {
                return resultClassDescriptor;
            }
            ObjectStreamClass localClassDescriptor = ObjectStreamClass.lookup(localClass);
            if (localClassDescriptor != null) { // only if class implements serializable
                final long localSUID = localClassDescriptor.getSerialVersionUID();
                final long streamSUID = resultClassDescriptor.getSerialVersionUID();
                if (streamSUID != localSUID) { // check for serialVersionUID mismatch.
                    final StringBuffer s = new StringBuffer("Overriding serialized class version mismatch: ");
                    s.append("local serialVersionUID = ").append(localSUID);
                    s.append(" stream serialVersionUID = ").append(streamSUID);
                    Exception e = new InvalidClassException(s.toString());
                    resultClassDescriptor = localClassDescriptor; // Use local class descriptor for deserialization
                }
            }
            return resultClassDescriptor;
        }
    }
}
