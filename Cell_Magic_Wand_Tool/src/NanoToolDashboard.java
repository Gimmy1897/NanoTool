import ij.plugin.PlugIn;
import ij.plugin.frame.PlugInFrame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class NanoToolDashboard extends PlugInFrame implements PlugIn {

    private static NanoToolDashboard dashboard;
    private final Cell_Magic_Wand_Tool wandTool = new Cell_Magic_Wand_Tool();
    private final Timer roiCountTimer = new Timer(250, e -> updateRoiCount());
    private JLabel roiCountLabel;

    public NanoToolDashboard() {
        super("NanoTool");
        dashboard = this;
        setIconImage(NanoTool_Launcher_Tool.getToolIconImage());
        try {
            ij.gui.Toolbar.addPlugInTool(wandTool);
            ij.gui.Toolbar.addPlugInTool(new NanoTool_Launcher_Tool());
        } catch (Exception ignored) {}
        EqDiameterUtility.ensureDefaultMeasurements();
        buildUI();
        pack();
        setResizable(false);
        positionOnRight();
        setVisible(true);
        updateRoiCount();
        roiCountTimer.start();
    }

    public void run(String arg) {
        try {
            ij.gui.Toolbar.addPlugInTool(wandTool);
            ij.gui.Toolbar.addPlugInTool(new NanoTool_Launcher_Tool());
        } catch (Exception ignored) {}
        setVisible(true);
        toFront();
        updateRoiCount();
    }

    private void buildUI() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Section 1: Preprocessing
        JButton btnSetScale = makeButton("Set scale (auto)", e -> TemScaleUtility.run());
        JButton btnMeanFilter = makeButton("Apply Mean filter (2 px radius)", e -> MeanFilterUtility.run());
        JPanel prepSection = createSectionPanel("Image Preprocessing", btnSetScale, btnMeanFilter);

        // Section 2: Selection
        JButton btnWandTool = makeButton("Selection Tool", this::runMagicWand);
        JButton btnWandSettings = makeButton("Selection tool settings", this::openWandSettings);
        JPanel selectSection = createSectionPanel("Selection and Segmentation", btnWandTool, btnWandSettings);

        // Section 3: Analysis
        JButton btnMeasure = makeButton("Measure nanoparticles", e -> EqDiameterUtility.run());
        JButton btnMeasureSettings = makeButton("Measurement settings", e -> ij.IJ.run("Set Measurements..."));
        roiCountLabel = new JLabel();
        roiCountLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        JPanel measureSection = createSectionPanel("Measurement and Analysis", btnMeasure, btnMeasureSettings, roiCountLabel);

        // Section 4: Data Management
        JButton btnExport = makeButton("Export measurements / ROIs...", e -> {
            RoiExportImportUtility.exportData();
            updateRoiCount();
        });
        JButton btnImport = makeButton("Import saved ROIs...", e -> {
            RoiExportImportUtility.importRois();
            updateRoiCount();
        });
        JPanel dataSection = createSectionPanel("Data and ROI Management", btnExport, btnImport);

        JLabel footerLabel = new JLabel("made with <3 in Pisa by gimmy1897.dev - Sept. 2026");
        footerLabel.setFont(new Font("Dialog", Font.ITALIC, 10));
        footerLabel.setForeground(Color.GRAY);
        footerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton btnCredits = new JButton("Credits");
        btnCredits.setFont(new Font("Dialog", Font.PLAIN, 10));
        btnCredits.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnCredits.addActionListener(e -> showCredits());

        mainPanel.add(prepSection);
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(selectSection);
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(measureSection);
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(dataSection);
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(footerLabel);
        mainPanel.add(Box.createVerticalStrut(4));
        mainPanel.add(btnCredits);

        add(mainPanel);
    }

    private void showCredits() {
        String creditsMessage =
                "NanoTool is inspired by Cell Magic Wand.\n\n" +
                "Thanks to Theo Walker for creating the Cell Magic Wand plugin. (https://github.com/manimino).\n\n" +
                "GitHub Project: https://github.com/fitzlab/CellMagicWand";

        JOptionPane.showMessageDialog(this, creditsMessage, "Credits and Acknowledgements", JOptionPane.INFORMATION_MESSAGE);
    }

    private JPanel createSectionPanel(String title, JComponent... components) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), title),
                BorderFactory.createEmptyBorder(6, 8, 8, 8)
        ));
        panel.setAlignmentX(Component.CENTER_ALIGNMENT);

        for (int i = 0; i < components.length; i++) {
            if (i > 0) {
                panel.add(Box.createVerticalStrut(6));
            }
            components[i].setAlignmentX(Component.CENTER_ALIGNMENT);
            panel.add(components[i]);
        }
        return panel;
    }

    private void positionOnRight() {
        try {
            Rectangle bounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
            int x = bounds.x + bounds.width - getWidth() - 20;
            int y = bounds.y + (bounds.height - getHeight()) / 2;
            setLocation(Math.max(0, x), Math.max(0, y));
        } catch (Exception ex) {
            setLocationRelativeTo(null);
        }
    }

    public static void updateRoiCount() {
        if (dashboard == null || dashboard.roiCountLabel == null) {
            return;
        }

        ij.plugin.frame.RoiManager roiManager = ij.plugin.frame.RoiManager.getInstance();
        int count = roiManager == null ? 0 : roiManager.getCount();
        dashboard.roiCountLabel.setText("Nanoparticles selected: " + count);
    }

    private JButton makeButton(String label, ActionListener listener) {
        JButton b = new JButton(label);
        b.setAlignmentX(Component.CENTER_ALIGNMENT);
        b.setMaximumSize(new Dimension(260, b.getPreferredSize().height));
        b.addActionListener(listener);
        return b;
    }

    private void runMagicWand(ActionEvent e) {
        wandTool.run("");
        ij.IJ.showStatus("Magic Wand active - click the image");
    }

    private void openWandSettings(ActionEvent e) {
        wandTool.showOptionsDialog();
    }
}