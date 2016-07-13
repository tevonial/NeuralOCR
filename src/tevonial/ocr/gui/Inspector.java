package tevonial.ocr.gui;

import tevonial.ocr.DataHandler;
import tevonial.ocr.ImagePanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Inspector extends javax.swing.JFrame {

    private DataHandler.Data data;
    private int max = 0;

    public Inspector() {
        initComponents();
        initListeners();
        positionTextField.setColumns(6);
        Image image = Toolkit.getDefaultToolkit().getImage(getClass().getResource("icon.png"));
        setIconImage(image);
    }

    private void initListeners() {
        openMenuItem.addActionListener(actionEvent -> {
            open();
        });
        positionTextField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent keyEvent) {
                int p = 0;
                try {
                    p = Integer.valueOf(positionTextField.getText());
                } catch (Exception e) {}
                if (seek(p)) {
                    positionSlider.setValue(p);
                }
            }
        });
        positionSlider.addChangeListener(changeEvent -> {
            int p = positionSlider.getValue();
            if (seek(p)) {
                positionTextField.setText(String.valueOf(p));
            }
        });
    }

    private void open() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
        if (fileChooser.showOpenDialog(Inspector.this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            Path path = Paths.get(file.getPath());
            byte[] temp = null;

            try {
                temp = Files.readAllBytes(path);
                System.out.println("Loaded " + path.toString());
            } catch (IOException e) {
                System.err.println("Error loading " + path.toString());
            }

            data = new DataHandler.Data(temp);

            fileLabel.setText(path.getFileName().toString());
            sizeLabel.setText(String.valueOf(data.getSize()));

            max = data.getLength();
            lengthLabel.setText(String.valueOf(max));
            ofLabel.setText(String.valueOf(max-1));

            positionSlider.setMaximum(max-1);
            positionSlider.setValue(0);
            positionTextField.setText("0");
        }
    }

    private boolean seek(int position) {
        if (position >= 0 && position < max) {
            byte[] pixels = data.getBytes(position);
            imagePanel.setImage(DataHandler.renderImage(pixels, 200, 200));
            return true;
        } else {
            return false;
        }
    }

    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        fileLabel = new javax.swing.JLabel();
        sizeLabel = new javax.swing.JLabel();
        lengthLabel = new javax.swing.JLabel();
        positionSlider = new javax.swing.JSlider();
        ofLabel = new javax.swing.JLabel();
        positionTextField = new javax.swing.JTextField();
        imagePanel = new ImagePanel();
        menuBar = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        openMenuItem = new javax.swing.JMenuItem();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Inspector");
        setResizable(false);

        jLabel1.setText("File:");

        jLabel2.setText("Size:");

        jLabel3.setText("Length:");

        fileLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        fileLabel.setText("-");

        sizeLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        sizeLabel.setText("-");

        lengthLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lengthLabel.setText("-");

        ofLabel.setText("-");

        /*
        positionTextField.setText("-");
        */

        imagePanel.setPreferredSize(new java.awt.Dimension(200, 200));

        javax.swing.GroupLayout imagePanelLayout = new javax.swing.GroupLayout(imagePanel);
        imagePanel.setLayout(imagePanelLayout);
        imagePanelLayout.setHorizontalGroup(
                imagePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 200, Short.MAX_VALUE)
        );
        imagePanelLayout.setVerticalGroup(
                imagePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 200, Short.MAX_VALUE)
        );

        fileMenu.setText("File");

        openMenuItem.setText("Open");
        fileMenu.add(openMenuItem);

        menuBar.add(fileMenu);

        setJMenuBar(menuBar);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(positionSlider, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addGroup(layout.createSequentialGroup()
                                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(jLabel1)
                                                        .addComponent(jLabel2)
                                                        .addComponent(jLabel3))
                                                .addGap(18, 18, 18)
                                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(lengthLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .addComponent(fileLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .addComponent(sizeLabel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(positionTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(ofLabel))
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(imagePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addGap(0, 0, Short.MAX_VALUE)))
                                .addContainerGap())
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel1)
                                        .addComponent(fileLabel))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel2)
                                        .addComponent(sizeLabel))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel3)
                                        .addComponent(lengthLabel))
                                .addGap(18, 18, 18)
                                .addComponent(positionSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(positionTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(ofLabel))
                                .addGap(18, 18, 18)
                                .addComponent(imagePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addContainerGap())
        );

        pack();
    }

    // Variables declaration - do not modify
    private javax.swing.JLabel fileLabel;
    private javax.swing.JMenu fileMenu;
    /*
    private javax.swing.JPanel imagePanel;
    */ private ImagePanel imagePanel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel lengthLabel;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JLabel ofLabel;
    private javax.swing.JMenuItem openMenuItem;
    private javax.swing.JSlider positionSlider;
    private javax.swing.JTextField positionTextField;
    private javax.swing.JLabel sizeLabel;
    // End of variables declaration
}
