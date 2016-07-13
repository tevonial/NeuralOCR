package tevonial.ocr.gui;

import tevonial.ocr.ImagePanel;
import tevonial.ocr.NeuralOCR;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;

public class Draw extends JFrame {
    DefaultTableModel tableModel;
    int guess = -1;


    public Draw() {
        Image image = Toolkit.getDefaultToolkit().getImage(getClass().getResource("icon.png"));
        setIconImage(image);

        tableModel = new DefaultTableModel(
                new Object [][] {
                        {"0", null},
                        {"1", null},
                        {"2", null},
                        {"3", null},
                        {"4", null},
                        {"5", null},
                        {"6", null},
                        {"7", null},
                        {"8", null},
                        {"9", null}
                },
                new String [] {
                        "Output", "Value"
                }
        ) {
            boolean[] canEdit = new boolean [] {
                    false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        };

        setResizable(false);
        initComponents();

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable jTable, Object o, boolean b, boolean b1, int i, int i1) {
                Component c = super.getTableCellRendererComponent(table, o, b, b1, i, i1);
                c.setBackground((i == guess) ? Color.GREEN : Color.WHITE);
                return c;
            }
        });
    }

    class DrawPanel extends JPanel implements MouseListener, MouseMotionListener {
        private int index;
        private Point[] arr;
        private BufferedImage previewImage;
        private Graphics2D previewGraphics;

        public DrawPanel() {
            addMouseListener(this);
            addMouseMotionListener(this);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setStroke(new BasicStroke(10));
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            super.paintComponent(g2d);
            for (int i = 0; i < index - 1; i++)
                g2d.drawLine(arr[i].x, arr[i].y, arr[i + 1].x, arr[i + 1].y);

            previewImage = new BufferedImage(28, 28, BufferedImage.TYPE_BYTE_GRAY);
            previewGraphics = previewImage.createGraphics();
            previewGraphics.setStroke(new BasicStroke(2));

            for (int i = 0; i < index - 1; i++)
                previewGraphics.drawLine(arr[i].x / 7, arr[i].y / 7, arr[i + 1].x / 7, arr[i + 1].y / 7);

            preview.setImage(previewImage);
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            arr[index++] = new Point(e.getX(), e.getY());
            repaint();
        }

        @Override
        public void mousePressed(MouseEvent e) {
            arr = new Point[10000];
            index = 0;
            arr[index++] = new Point(e.getX(), e.getY());
            repaint();
        }

        @Override
        public void mouseReleased(MouseEvent mouseEvent) {
            byte[] data = ((DataBufferByte) previewImage.getData().getDataBuffer()).getData();
            double[] input = new double[784];

            //String log = "";

            for (int j = 0; j < data.length; j++) {
                input[j] = data[j] & 0xff;
                //log += input[j];
            }

            //System.out.println(log);

            double[] output = NeuralOCR.getNetwork().process(input, null, false, null);
            double max = 0.0; guess = 0;
            for (int j=0; j<10; j++) {
                if (output[j] > max) {
                    max = output[j];
                    guess = j;
                }
            }

            String f = "%2.4f  ";

            for (int i=0; i<output.length; i++) {
                tableModel.setValueAt(String.format(f, output[i]*100), i, 1);
            }
            tableModel.fireTableDataChanged();
        }

        public void mouseMoved(MouseEvent mouseEvent) {}
        public void mouseClicked(MouseEvent mouseEvent) {}
        public void mouseEntered(MouseEvent mouseEvent) {}
        public void mouseExited(MouseEvent mouseEvent) {}
    }

    @Override
    public void paint(Graphics graphics) {
        super.paint(graphics);
    }

    private void initComponents() {

        jSplitPane1 = new javax.swing.JSplitPane();
        jScrollPane1 = new javax.swing.JScrollPane();
        table = new javax.swing.JTable();
        drawPanel = new DrawPanel();
        preview = new ImagePanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Analyze Drawing");

        jScrollPane1.setMaximumSize(new java.awt.Dimension(196, 196));
        jScrollPane1.setMinimumSize(new java.awt.Dimension(196, 196));
        jScrollPane1.setPreferredSize(new java.awt.Dimension(196, 196));

        table.setModel(tableModel);
        table.getTableHeader().setReorderingAllowed(false);
        jScrollPane1.setViewportView(table);
        if (table.getColumnModel().getColumnCount() > 0) {
            table.getColumnModel().getColumn(0).setPreferredWidth(50);
            table.getColumnModel().getColumn(1).setPreferredWidth(100);
        }

        jSplitPane1.setRightComponent(jScrollPane1);

        drawPanel.setBackground(new java.awt.Color(255, 255, 255));
        drawPanel.setMaximumSize(new java.awt.Dimension(196, 196));
        drawPanel.setMinimumSize(new java.awt.Dimension(196, 196));
        drawPanel.setPreferredSize(new java.awt.Dimension(196, 196));

        preview.setPreferredSize(new java.awt.Dimension(28, 28));

        javax.swing.GroupLayout previewLayout = new javax.swing.GroupLayout(preview);
        preview.setLayout(previewLayout);
        previewLayout.setHorizontalGroup(
                previewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 28, Short.MAX_VALUE)
        );
        previewLayout.setVerticalGroup(
                previewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 28, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout drawPanelLayout = new javax.swing.GroupLayout(drawPanel);
        drawPanel.setLayout(drawPanelLayout);
        drawPanelLayout.setHorizontalGroup(
                drawPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(drawPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(preview, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(158, Short.MAX_VALUE))
        );
        drawPanelLayout.setVerticalGroup(
                drawPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(drawPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(preview, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(157, Short.MAX_VALUE))
        );

        jSplitPane1.setLeftComponent(drawPanel);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jSplitPane1)
                                .addContainerGap())
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jSplitPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }

    // Variables declaration - do not modify
    private DrawPanel drawPanel;/*
    private javax.swing.JPanel drawPanel;
    */
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSplitPane jSplitPane1;
    private ImagePanel preview;/*
    private javax.swing.JPanel preview;
    */
    private javax.swing.JTable table;
    // End of variables declaration
}
