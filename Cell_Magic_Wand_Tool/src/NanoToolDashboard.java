import ij.plugin.PlugIn;
import ij.plugin.frame.PlugInFrame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class NanoToolDashboard extends PlugInFrame implements PlugIn {

    private static final String VERSION = "1.0.2";
    private static NanoToolDashboard dashboard;
    private final Cell_Magic_Wand_Tool wandTool = new Cell_Magic_Wand_Tool();
    private final Timer roiCountTimer = new Timer(250, e -> updateRoiCount());
    private JLabel roiCountLabel;
    private JLabel scaleLabel;
    private JLabel meanFilterCountLabel;

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
        // Section 1: Preprocessing
        JButton btnSetScale = makeButton("Set scale (auto)", e -> {
            TemScaleUtility.run();
            updateScaleLabel();
        });
        scaleLabel = new JLabel();
        scaleLabel.setPreferredSize(new Dimension(250, 18));
        scaleLabel.setMinimumSize(scaleLabel.getPreferredSize());
        scaleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        updateScaleLabel();
        JButton btnMeanFilter = makeButton("Apply Mean filter (2 px radius)", e -> MeanFilterUtility.run());
        meanFilterCountLabel = new JLabel("Mean filter applied: 0 times");
        meanFilterCountLabel.setPreferredSize(new Dimension(250, 18));
        meanFilterCountLabel.setMinimumSize(new Dimension(250, 18));
        meanFilterCountLabel.setMaximumSize(new Dimension(250, 18));
        meanFilterCountLabel.setHorizontalAlignment(SwingConstants.CENTER);
        meanFilterCountLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        JPanel prepSection = createSectionPanel("Image Preprocessing",
                btnSetScale, scaleLabel, btnMeanFilter, meanFilterCountLabel);

        // Section 2: Selection
        JButton btnWandTool = makeButton("Selection Tool", this::runMagicWand);
        JButton btnWandSettings = makeButton("Selection tool settings", this::openWandSettings);
        JPanel selectSection = createSectionPanel("Selection and Segmentation", btnWandTool, btnWandSettings);

        // Section 3: Analysis
        JButton btnMeasure = makeButton("Measure nanoparticles", e -> EqDiameterUtility.run());
        JButton btnMeasureSettings = makeButton("Measurement settings", e -> ij.IJ.run("Set Measurements..."));
        roiCountLabel = new JLabel("Nanoparticles selected: 0");
        roiCountLabel.setPreferredSize(new Dimension(250, 18));
        roiCountLabel.setMinimumSize(new Dimension(250, 18));
        roiCountLabel.setMaximumSize(new Dimension(250, 18));
        roiCountLabel.setHorizontalAlignment(SwingConstants.CENTER);
        roiCountLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        JPanel measureSection = createSectionPanel("Measurement and Analysis", btnMeasure, btnMeasureSettings, roiCountLabel);

        // Section 4: Project Management
        JButton btnSave = makeButton("Save Project", e -> NanoToolProjectUtility.saveProject());
        JButton btnSaveAs = makeButton("Save Project As...", e -> NanoToolProjectUtility.saveProjectAs());
        JButton btnCloseProject = makeButton("Close Project", e -> NanoToolProjectUtility.closeProject());
        JButton btnOpenProject = makeButton("Open Project...", e -> {
            NanoToolProjectUtility.openProject();
            updateRoiCount();
            updateScaleLabel();
        });
        JPanel projectActions = createVerticalActions(
                btnSave, btnSaveAs, btnOpenProject, btnCloseProject);
        JPanel projectSection = createSectionPanel("Project Management", projectActions);

        JButton btnExportMeasurements = makeButton("Export measurements", e -> RoiExportImportUtility.exportMeasurements());
        JButton btnExportRois = makeButton("Export ROIs", e -> RoiExportImportUtility.exportRois());
        JButton btnImport = makeButton("Import ROIs...", e -> {
            RoiExportImportUtility.importRois();
            updateRoiCount();
        });
        JPanel exportActions = createVerticalActions(
                btnExportMeasurements, btnExportRois, btnImport);
        JPanel exportSection = createSectionPanel("Import and Export", exportActions);

        JLabel footerLabel = new JLabel("NanoTool v" + VERSION + ", Oct '26 - made with <3 in Pisa by Gimmy1897.dev");
        footerLabel.setFont(new Font("Dialog", Font.ITALIC, 10));
        footerLabel.setForeground(Color.GRAY);
        footerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton btnCredits = new JButton("Credits");
        btnCredits.setFont(new Font("Dialog", Font.PLAIN, 10));
        btnCredits.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnCredits.addActionListener(e -> showCredits());

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 4, 10));
        contentPanel.add(prepSection);
        contentPanel.add(Box.createVerticalStrut(6));
        contentPanel.add(selectSection);
        contentPanel.add(Box.createVerticalStrut(6));
        contentPanel.add(measureSection);
        contentPanel.add(Box.createVerticalStrut(6));
        contentPanel.add(projectSection);
        contentPanel.add(Box.createVerticalStrut(6));
        contentPanel.add(exportSection);

        JPanel footerPanel = new JPanel();
        footerPanel.setLayout(new BoxLayout(footerPanel, BoxLayout.Y_AXIS));
        footerPanel.add(btnCredits);
        footerPanel.add(Box.createVerticalStrut(2));
        footerPanel.add(footerLabel);
        footerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        footerPanel.setBorder(BorderFactory.createEmptyBorder(2, 4, 5, 4));

        JPanel rootPanel = new JPanel(new BorderLayout());
        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        rootPanel.add(scrollPane, BorderLayout.CENTER);
        rootPanel.add(footerPanel, BorderLayout.SOUTH);
        add(rootPanel);
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
                BorderFactory.createEmptyBorder(4, 6, 5, 6)
        ));
        panel.setAlignmentX(Component.CENTER_ALIGNMENT);

        for (int i = 0; i < components.length; i++) {
            if (i > 0) {
                panel.add(Box.createVerticalStrut(3));
            }
            components[i].setAlignmentX(Component.CENTER_ALIGNMENT);
            panel.add(components[i]);
        }
        panel.setMaximumSize(new Dimension(300, panel.getPreferredSize().height));
        return panel;
    }

    private JPanel createVerticalActions(JButton... buttons) {
        JPanel actions = new JPanel();
        actions.setLayout(new BoxLayout(actions, BoxLayout.Y_AXIS));
        for (int i = 0; i < buttons.length; i++) {
            if (i > 0) {
                actions.add(Box.createVerticalStrut(3));
            }
            actions.add(buttons[i]);
        }
        return actions;
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

        NanoToolProjectUtility.syncProjectOverlay();
        ij.plugin.frame.RoiManager roiManager = ij.plugin.frame.RoiManager.getInstance2();
        int count = roiManager == null ? 0 : roiManager.getCount();
        dashboard.roiCountLabel.setText("Nanoparticles selected: " + count);
        dashboard.updateScaleLabel();
        dashboard.updateMeanFilterCount();
    }

    public static void updateScaleLabel() {
        if (dashboard == null || dashboard.scaleLabel == null) {
            return;
        }

        ij.ImagePlus imp = ij.WindowManager.getCurrentImage();
        if (imp == null || !imp.getCalibration().scaled()) {
            dashboard.scaleLabel.setText("Scale: not set");
            return;
        }

        ij.measure.Calibration cal = imp.getCalibration();
        double pixelsPerUnit = 1.0 / cal.pixelWidth;
        dashboard.scaleLabel.setText(String.format("Scale: %.4g px/%s", pixelsPerUnit, cal.getUnit()));
    }

    public static void updateMeanFilterCount() {
        if (dashboard == null || dashboard.meanFilterCountLabel == null) {
            return;
        }
        ij.ImagePlus imp = ij.WindowManager.getCurrentImage();
        int count = NanoToolProjectUtility.getMeanFilterCount(imp);
        dashboard.meanFilterCountLabel.setText("Mean filter applied: " + count + " time"
                + (count == 1 ? "" : "s"));
    }

    public static synchronized void showDashboard() {
        java.awt.Frame existing = ij.WindowManager.getFrame("NanoTool");
        if (existing != null && existing != dashboard) {
            existing.setVisible(true);
            existing.toFront();
            if (existing instanceof NanoToolDashboard) {
                dashboard = (NanoToolDashboard) existing;
            }
            updateRoiCount();
            return;
        }

        if (dashboard == null || !dashboard.isDisplayable()) {
            new NanoToolDashboard();
        } else {
            dashboard.setVisible(true);
            dashboard.toFront();
            updateRoiCount();
        }
    }

    private JButton makeButton(String label, ActionListener listener) {
        JButton b = new JButton(label);
        b.setAlignmentX(Component.CENTER_ALIGNMENT);
        Dimension size = b.getPreferredSize();
        size.width = 220;
        b.setPreferredSize(size);
        b.setMaximumSize(new Dimension(220, size.height));
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