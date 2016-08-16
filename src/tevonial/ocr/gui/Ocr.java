package tevonial.ocr.gui;

import tevonial.ocr.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class Ocr extends JFrame {
    private static Ocr instance;
    private static Object obj = new Object();
    private int selectedIndex;
    private boolean busy;

    private long last_iterations = 0, last_i = 0;
    private long time;
    private double diff[];
    private int diff_ptr;

    public static Ocr createGUI() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | javax.swing.UnsupportedLookAndFeelException | IllegalAccessException ex) {}

        java.awt.EventQueue.invokeLater(() -> {
            instance = new Ocr();
            instance.setVisible(true);
            synchronized (obj) {
                obj.notifyAll();
            }
        });
        synchronized (obj) {
            try {
                obj.wait();
            } catch (InterruptedException e) {
            }
        }
        return instance;
    }

    private Ocr() {
        initComponents();
        initListeners();
    }

    public void displayDigit(int digit, BufferedImage img) {
        target.setText(String.valueOf(digit));
        if (img != null ) {
            imagePanel.setImage(img);
        }
    }

    public void setGuess(Integer g) {
        if (g == null) {
            guess.setText(" ");
        } else {
            guess.setText(String.valueOf(g));
        }
    }

    public void setError(double e) {
        String f = "%4.3f";
        error.setText(String.format(f, e) + "%");
    }

    public int getIterations() {
        int iter = 0;
        try {
            iter = Integer.valueOf(iterationsTextField.getText());
        } catch (NumberFormatException e) {}
        return iter;
    }

    public double getLearningRate() {
        double l = 0;
        try {
            l = Double.valueOf(learningRateTextField.getText());
        } catch (NumberFormatException e) {}
        return l;
    }

    public void setProgress(int i, int iterations) {
        iterationsCounter.setText(i + "/" + iterations);
        progressBar.setValue((i-- * 100) / iterations);
        if (i % 10 == 0) updateEta(i, iterations);
    }

    public void updateEta(int i, int iterations) {
        if (iterations != last_iterations || i < last_i) {
            diff = new double[20];
            diff_ptr = 0;
            time = System.currentTimeMillis() + 10000;
            last_iterations = iterations;
        }

        last_i = i;

        diff[diff_ptr++] = System.currentTimeMillis() - time;
        time = System.currentTimeMillis();

        if (diff_ptr == 20)
            diff_ptr = 0;

        if (i % 50 == 0) {
            double avg = 0.0, b = 0.0;
            for (int a = 0; a < 5; a++)
                if (diff[a] > 0) {
                    avg += diff[a];
                    b++;
                }

            avg /= b;

            long s = (long) ((avg / 10000.0) * (double) (iterations - i));
            labelEta.setVisible(true);
            etaCounter.setText(String.format("%d:%02d:%02d", s / 3600, (s % 3600) / 60, (s % 60)));
        }
    }

    private void listSelected(int index) {
        int digit = index;
        try {
            digit = Integer.valueOf(jList.getModel().getElementAt(index));
        } catch (Exception e) {}
        selectedIndex = index;
        jList.repaint();

        NeuralOCR.guess(digit);
    }

    public void setFile(File file) {
        String filename;
        if (file == null) {
            filename = "untitled";
            saveMenuItem.setEnabled(false);
        } else {
            filename = file.getName();
            saveMenuItem.setEnabled(true);
        }
        setTitle(filename + " - Neural OCR");
    }

    public void setBusy(boolean busy) {
        this.busy = busy;
        if (busy) {
            stopButton.setText("Stop");
            learnButton.setEnabled(false);
            jList.setEnabled(false);
        } else {
            stopButton.setText("Reset");
            learnButton.setEnabled(true);
            iterationsCounter.setText("-/-");
            progressBar.setValue(0);
            labelEta.setVisible(false);
            etaCounter.setText(null);
            jList.setEnabled(true);
        }
    }

    private void initListeners() {
        learnButton.addActionListener(evt -> {
            NeuralOCR.learn();
        });
        stopButton.addActionListener(evt -> {
            if (busy) {
                NeuralOCR.task.interrupt();
                //setBusy(false);
            } else {
                NeuralOCR.generateNetwork();
            }
        });

        jList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                JList list = (JList)evt.getSource();
                if (evt.getClickCount() == 2 && list.isEnabled()) {
                    listSelected(list.locationToIndex(evt.getPoint()));
                }
            }
        });
        jList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent keyEvent) {
                JList list = (JList)keyEvent.getSource();
                if (keyEvent.getKeyCode() == KeyEvent.VK_ENTER  && list.isEnabled()) {
                    listSelected(list.getSelectedIndex());
                }
            }
        });
        jList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus) ;

                if(index == selectedIndex && !cellHasFocus) {
                    c.setBackground(Color.lightGray);
                }
                return c ;
            }
        });
        iterationsTextField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent keyEvent) {
                String t = iterationsTextField.getText();
                if (t.isEmpty()) { t = "-"; }
                iterationsCounter.setText("-/" + t);
            }
        });
        saveMenuItem.addActionListener(actionEvent -> NeuralOCR.save(null));
        saveAsMenuItem.addActionListener(actionEvent -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
            if (fileChooser.showSaveDialog(Ocr.this) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                NeuralOCR.save(file);
            }
        });
        openMenuItem.addActionListener(actionEvent -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
            if (fileChooser.showOpenDialog(Ocr.this) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                NeuralOCR.open(file);
            }
        });
        testAllMenuItem.addActionListener(actionEvent -> {
            NeuralOCR.testAll();
        });
        testDrawMenuItem.addActionListener(actionEvent -> {
            new Draw().setVisible(true);
        });
        inspectMenuItem.addActionListener(actionEvent -> {
            new Inspector().setVisible(true);
        });
    }

    // <editor-fold defaultstate="collapsed" desc="Generated Code">
    private void initComponents() {

        scrollPane = new javax.swing.JScrollPane();
        jList = new javax.swing.JList<>();
        controlPanel = new javax.swing.JPanel();
        labelIterations = new javax.swing.JLabel();
        labelLR = new javax.swing.JLabel();
        labelEta = new javax.swing.JLabel();
        iterationsTextField = new javax.swing.JTextField();
        learningRateTextField = new javax.swing.JTextField();
        iterationsCounter = new javax.swing.JLabel();
        etaCounter = new javax.swing.JLabel();
        learnButton = new javax.swing.JButton();
        stopButton = new javax.swing.JButton();
        progressBar = new javax.swing.JProgressBar();
        previewPanel = new javax.swing.JPanel();
        imagePanel = new ImagePanel();
        labelTarget = new javax.swing.JLabel();
        labelGuess = new javax.swing.JLabel();
        labelError = new javax.swing.JLabel();
        target = new javax.swing.JLabel();
        guess = new javax.swing.JLabel();
        error = new javax.swing.JLabel();
        menuBar = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        openMenuItem = new javax.swing.JMenuItem();
        saveMenuItem = new javax.swing.JMenuItem();
        saveAsMenuItem = new javax.swing.JMenuItem();
        testMenu = new javax.swing.JMenu();
        testAllMenuItem = new javax.swing.JMenuItem();
        testDrawMenuItem = new javax.swing.JMenuItem();
        inspectMenuItem = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("DigitDisplayTest");
        setResizable(false);

        jList.setModel(new javax.swing.AbstractListModel<String>() {
            String[] strings = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9" };
            public int getSize() { return strings.length; }
            public String getElementAt(int i) { return strings[i]; }
        });
        scrollPane.setViewportView(jList);

        labelIterations.setText("Iterations:");

        labelLR.setText("Learning rate:");

        labelEta.setVisible(false);
        labelEta.setText("Time remaining:");

        iterationsCounter.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        iterationsCounter.setText("-/-");

        etaCounter.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        etaCounter.setText(" ");

        learnButton.setText("Learn");

        stopButton.setText("Reset");

        javax.swing.GroupLayout controlPanelLayout = new javax.swing.GroupLayout(controlPanel);
        controlPanel.setLayout(controlPanelLayout);
        controlPanelLayout.setHorizontalGroup(
                controlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(controlPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(controlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(controlPanelLayout.createSequentialGroup()
                                                .addGroup(controlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(labelLR)
                                                        .addComponent(labelIterations))
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addGroup(controlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(learningRateTextField, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 69, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(iterationsTextField, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 69, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                        .addComponent(progressBar, javax.swing.GroupLayout.DEFAULT_SIZE, 207, Short.MAX_VALUE)
                                        .addGroup(controlPanelLayout.createSequentialGroup()
                                                .addGroup(controlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                                        .addGroup(controlPanelLayout.createSequentialGroup()
                                                                .addComponent(learnButton)
                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(stopButton))
                                                        .addComponent(labelEta, javax.swing.GroupLayout.DEFAULT_SIZE, 138, Short.MAX_VALUE))
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addGroup(controlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(etaCounter, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .addComponent(iterationsCounter, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                                .addContainerGap())
        );
        controlPanelLayout.setVerticalGroup(
                controlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(controlPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(controlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(labelIterations)
                                        .addComponent(iterationsTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(controlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(labelLR)
                                        .addComponent(learningRateTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 27, Short.MAX_VALUE)
                                .addGroup(controlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(etaCounter)
                                        .addComponent(labelEta))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(controlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(learnButton)
                                        .addComponent(stopButton)
                                        .addComponent(iterationsCounter))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap())
        );

        previewPanel.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));

        imagePanel.setPreferredSize(new java.awt.Dimension(100, 100));

        javax.swing.GroupLayout imagePanelLayout = new javax.swing.GroupLayout(imagePanel);
        imagePanel.setLayout(imagePanelLayout);
        imagePanelLayout.setHorizontalGroup(
                imagePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 100, Short.MAX_VALUE)
        );
        imagePanelLayout.setVerticalGroup(
                imagePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 100, Short.MAX_VALUE)
        );

        labelTarget.setText("Target:");

        labelGuess.setText("Guess:");

        labelError.setText("Error:");

        target.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        target.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        target.setText(" ");

        guess.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        guess.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        guess.setText(" ");

        error.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        error.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        error.setText(" ");

        javax.swing.GroupLayout previewPanelLayout = new javax.swing.GroupLayout(previewPanel);
        previewPanel.setLayout(previewPanelLayout);
        previewPanelLayout.setHorizontalGroup(
                previewPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, previewPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(imagePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(previewPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(previewPanelLayout.createSequentialGroup()
                                                .addComponent(labelTarget)
                                                .addGap(18, 18, 18)
                                                .addComponent(target, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                        .addGroup(previewPanelLayout.createSequentialGroup()
                                                .addGroup(previewPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(labelGuess)
                                                        .addComponent(labelError))
                                                .addGap(5, 5, 5)
                                                .addGroup(previewPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(error, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .addComponent(guess, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                                .addContainerGap())
        );
        previewPanelLayout.setVerticalGroup(
                previewPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(previewPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(previewPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(imagePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGroup(previewPanelLayout.createSequentialGroup()
                                                .addGroup(previewPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                        .addComponent(labelTarget)
                                                        .addComponent(target))
                                                .addGap(39, 39, 39)
                                                .addGroup(previewPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                        .addComponent(guess)
                                                        .addComponent(labelGuess))
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addGroup(previewPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                        .addComponent(labelError)
                                                        .addComponent(error))))
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        fileMenu.setText("File");

        openMenuItem.setText("Open");
        fileMenu.add(openMenuItem);

        saveMenuItem.setText("Save");
        fileMenu.add(saveMenuItem);

        saveAsMenuItem.setText("Save As...");
        fileMenu.add(saveAsMenuItem);

        menuBar.add(fileMenu);

        testMenu.setText("Test");

        testAllMenuItem.setText("Test all");
        testMenu.add(testAllMenuItem);

        testDrawMenuItem.setText("Draw...");
        testMenu.add(testDrawMenuItem);

        inspectMenuItem.setText("Inspector");
        testMenu.add(inspectMenuItem);

        menuBar.add(testMenu);

        setJMenuBar(menuBar);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(scrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(previewPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(controlPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(scrollPane)
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(previewPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(controlPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                                .addContainerGap())
        );

        pack();
    }// </editor-fold>

    // Variables declaration - do not modify
    private javax.swing.JPanel controlPanel;
    private javax.swing.JLabel error;
    private javax.swing.JLabel etaCounter;
    private javax.swing.JMenu fileMenu;
    private javax.swing.JLabel guess;
    /*
    private javax.swing.JPanel imagePanel;
    */private ImagePanel imagePanel;
    private javax.swing.JMenuItem inspectMenuItem;
    private javax.swing.JLabel iterationsCounter;
    private javax.swing.JTextField iterationsTextField;
    private javax.swing.JList<String> jList;
    private javax.swing.JLabel labelError;
    private javax.swing.JLabel labelEta;
    private javax.swing.JLabel labelGuess;
    private javax.swing.JLabel labelIterations;
    private javax.swing.JLabel labelLR;
    private javax.swing.JLabel labelTarget;
    private javax.swing.JButton learnButton;
    private javax.swing.JTextField learningRateTextField;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JMenuItem openMenuItem;
    private javax.swing.JPanel previewPanel;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JMenuItem saveAsMenuItem;
    private javax.swing.JMenuItem saveMenuItem;
    private javax.swing.JScrollPane scrollPane;
    private javax.swing.JButton stopButton;
    private javax.swing.JLabel target;
    private javax.swing.JMenuItem testAllMenuItem;
    private javax.swing.JMenuItem testDrawMenuItem;
    private javax.swing.JMenu testMenu;
    // End of variables declaration
}
