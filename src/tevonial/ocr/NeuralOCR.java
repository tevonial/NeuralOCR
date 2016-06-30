package tevonial.ocr;

import tevonial.neural.Network;

/**
 * Created by Connor on 6/28/2016.
 */
public class NeuralOCR {
    private static OcrGui gui;
    private static Network net;

    public static void main(String[] args) {
        gui = OcrGui.createGUI();
        DataHandler.loadData();
        generateNetwork();
        gui.displayDigit(0);
    }

    public static void generateNetwork() {
        System.err.println("REGENERATE");
        net = null;
        net = new Network(784, 10)
                .setHiddenLayers(2, 100)
                .build();
    }

    public static void learn() {
        net.setLearningRate(gui.getLearningRate());

        new Thread(() -> {
            int iterations = gui.getIterations();
            double[] input = new double[784];
            double[] target = new double[10];
            byte[] bytes;

            for (int i = 1; i <= iterations; i++) {
                gui.setProgress(i, iterations);
                for (int digit = 0; digit < 10; digit++) {

                    if (digit == digit) {

                        bytes = DataHandler.getData(digit, i);
                        gui.displayDigit(digit, DataHandler.renderImage(bytes));

                        for (int j = 0; j < bytes.length; j++) {
                            input[j] = bytes[j] & 0xff;
                        }
                        for (int j = 0; j < target.length; j++) {
                            target[j] = (j == digit) ? 1.0 : 0.0;
                        }
                        net.process(input, target, true, digit);

                    }
                }
                System.out.println();
            }
        }).start();

    }

    public static void guess(int digit) {
        int head = (int)(Math.random() * 1000);
        byte[] data = DataHandler.getData(digit, head);
        gui.displayDigit(digit, DataHandler.renderImage(data));

        double[] input = new double[784];
        double[] target = new double[10];

        for (int j = 0; j < data.length; j++) {
            input[j] = data[j] & 0xff;
        }
        for (int j = 0; j < target.length; j++) {
            target[j] = (j == digit) ? 1 : 0;
        }

        double[] output = net.process(input, target, false, digit);

        double max = 0.0; int guess = 0;
        for (int j=0; j<10; j++) {
            if (output[j] > max) {
                max = output[j];
                guess = j;
            }
        }

        gui.setGuess(guess);
    }

    public static void stop() {

    }


}
