import ij.plugin.PlugIn;
import ij.plugin.frame.PlugInFrame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class NanoToolDashboard extends PlugInFrame implements PlugIn {

    private static final String VERSION = "1.0.2";
    private static NanoToolDashboard dashboard;
    private static ij.ImagePlus analysisImageOverride;
    private final Cell_Magic_Wand_Tool wandTool = new Cell_Magic_Wand_Tool();
    private final Timer roiCountTimer = new Timer(250, e -> updateRoiCount());
    private JLabel roiCountLabel;
    private JLabel scaleLabel;
    private JLabel meanFilterCountLabel;
    private JLabel imageNameLabel;
    private JLabel statusLabel;
    private JButton btnSetScale;
    private JButton btnMeanFilter;
    private JButton btnWandTool;
    private JButton btnWandSettings;
    private JButton btnMeasure;
    private JButton btnMeasureSettings;
    private JButton btnStatistics;
    private JButton btnSave;
    private JButton btnSaveAs;
    private JButton btnCloseProject;
    private JButton btnExportMeasurements;
    private JButton btnExportRois;
    private JButton btnImport;

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
        btnSetScale = makeButton("Set scale (auto)", e -> {
            TemScaleUtility.run();
            updateScaleLabel();
            setStatus("Scale updated.");
        });
        JButton btnOpenImage = makeButton("Open Image or Project...", e -> openImageOrProject());
        scaleLabel = new JLabel();
        scaleLabel.setPreferredSize(new Dimension(220, 18));
        scaleLabel.setMinimumSize(scaleLabel.getPreferredSize());
        scaleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        updateScaleLabel();
        btnMeanFilter = makeButton("Mean filter (r=2)", e -> {
            MeanFilterUtility.run();
            setStatus("Mean filter applied.");
        });
        meanFilterCountLabel = new JLabel("Mean filter applied: 0 times");
        meanFilterCountLabel.setPreferredSize(new Dimension(190, 18));
        meanFilterCountLabel.setMinimumSize(new Dimension(190, 18));
        meanFilterCountLabel.setMaximumSize(new Dimension(190, 18));
        meanFilterCountLabel.setHorizontalAlignment(SwingConstants.CENTER);
        meanFilterCountLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        JPanel prepSection = createSectionPanel("Image Pre-processing",
                btnOpenImage, btnSetScale, scaleLabel,
                createFilterRow(btnMeanFilter, meanFilterCountLabel));

        // Section 2: Selection
        btnWandTool = makeButton("Selection Tool", this::runMagicWand);
        btnWandSettings = makeButton("Selection tool settings", this::openWandSettings);
        JPanel selectSection = createSectionPanel("Selection Tool",
                btnWandTool, btnWandSettings);

        // Section 3: Analysis
        btnMeasure = makeButton("Measure nanoparticles", e -> {
            EqDiameterUtility.run();
            setStatus("Equivalent diameters calculated.");
        });
        btnMeasureSettings = makeButton("Measurement settings", e -> ij.IJ.run("Set Measurements..."));
        btnStatistics = makeButton("View statistics and histogram", e -> {
            NanoToolStatisticsUtility.showStatisticsAndHistogram();
            setStatus("Statistics and histogram displayed.");
        });
        roiCountLabel = new JLabel("Nanoparticles selected: 0");
        roiCountLabel.setPreferredSize(new Dimension(190, 18));
        roiCountLabel.setMinimumSize(new Dimension(190, 18));
        roiCountLabel.setMaximumSize(new Dimension(190, 18));
        roiCountLabel.setHorizontalAlignment(SwingConstants.CENTER);
        roiCountLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        JPanel measureSection = createSectionPanel("Measurement",
                btnMeasure, btnMeasureSettings, btnStatistics, roiCountLabel);

        // Section 4: Project Management
        btnSave = makeButton("Save Project", e -> {
            NanoToolProjectUtility.saveProject();
            setStatus("Project saved.");
        });
        btnSaveAs = makeButton("Save Project As...", e -> {
            NanoToolProjectUtility.saveProjectAs();
            setStatus("Project saved.");
        });
        btnCloseProject = makeButton("Close Project", e -> {
            NanoToolProjectUtility.closeProject();
            clearAnalysisImage();
            updateRoiCount();
            setStatus("Project closed.");
        });
        JButton btnOpenProject = makeButton("Open Project...", e -> {
            NanoToolProjectUtility.openProject();
            updateRoiCount();
            updateScaleLabel();
            setStatus("Project opened.");
        });
        btnExportMeasurements = makeButton("Export measurements", e -> {
            RoiExportImportUtility.exportMeasurements();
            setStatus("Measurements exported.");
        });
        btnExportRois = makeButton("Export ROIs", e -> {
            RoiExportImportUtility.exportRois();
            setStatus("ROIs exported.");
        });
        btnImport = makeButton("Import ROIs...", e -> {
            RoiExportImportUtility.importRois();
            updateRoiCount();
            setStatus("ROIs imported.");
        });
        imageNameLabel = new JLabel("Image: no image open");
        imageNameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        imageNameLabel.setHorizontalAlignment(SwingConstants.CENTER);

        statusLabel = new JLabel("Ready");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        statusLabel.setForeground(Color.DARK_GRAY);

        JLabel footerLabel = new JLabel("NanoTool v" + VERSION + " - made with <3 in Pisa by Gimmy1897.dev");
        footerLabel.setFont(new Font("Dialog", Font.ITALIC, 10));
        footerLabel.setForeground(Color.GRAY);
        footerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel sessionInfoSection = createSectionPanel("Status",
                imageNameLabel, statusLabel);

        setMenuBar(createMenuBar(btnOpenProject, btnSave, btnSaveAs, btnCloseProject,
                btnImport, btnExportMeasurements, btnExportRois));

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(3, 3, 2, 3));
        contentPanel.add(prepSection);
        contentPanel.add(Box.createVerticalStrut(3));
        contentPanel.add(selectSection);
        contentPanel.add(Box.createVerticalStrut(3));
        contentPanel.add(measureSection);
        contentPanel.add(Box.createVerticalStrut(3));
        contentPanel.add(sessionInfoSection);

        JPanel footerPanel = new JPanel();
        footerPanel.setLayout(new BoxLayout(footerPanel, BoxLayout.Y_AXIS));
        footerPanel.add(footerLabel);
        footerPanel.setBorder(BorderFactory.createEmptyBorder(2, 1, 3, 1));

        JPanel rootPanel = new JPanel(new BorderLayout());
        rootPanel.add(contentPanel, BorderLayout.CENTER);
        rootPanel.add(footerPanel, BorderLayout.SOUTH);
        add(rootPanel);
        updateButtonStates();
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
                BorderFactory.createEmptyBorder(2, 2, 2, 2)
        ));
        panel.setAlignmentX(Component.CENTER_ALIGNMENT);

        for (int i = 0; i < components.length; i++) {
            if (i > 0) {
                panel.add(Box.createVerticalStrut(2));
            }
            components[i].setAlignmentX(Component.CENTER_ALIGNMENT);
            panel.add(components[i]);
        }
        Dimension sectionSize = new Dimension(280, panel.getPreferredSize().height);
        panel.setMinimumSize(sectionSize);
        panel.setPreferredSize(sectionSize);
        panel.setMaximumSize(sectionSize);
        return panel;
    }

    private JPanel createFilterRow(JButton filterButton, JLabel count) {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
        row.add(filterButton);
        row.add(count);
        return row;
    }

    private java.awt.MenuBar createMenuBar(JButton openProject, JButton save,
            JButton saveAs, JButton closeProject, JButton importRois,
            JButton exportMeasurements, JButton exportRois) {
        java.awt.MenuBar menuBar = new java.awt.MenuBar();
        java.awt.Menu fileMenu = new java.awt.Menu("File");
        addMenuItem(fileMenu, "Open project...", openProject);
        addMenuItem(fileMenu, "Save project", save);
        addMenuItem(fileMenu, "Save project as...", saveAs);
        addMenuItem(fileMenu, "Close project", closeProject);
        fileMenu.addSeparator();
        addMenuItem(fileMenu, "Import ROIs...", importRois);
        addMenuItem(fileMenu, "Export measurements", exportMeasurements);
        addMenuItem(fileMenu, "Export ROIs", exportRois);
        menuBar.add(fileMenu);
        java.awt.Menu helpMenu = new java.awt.Menu("Help");
        MenuItem credits = new MenuItem("Credits");
        credits.addActionListener(e -> showCredits());
        helpMenu.add(credits);
        menuBar.add(helpMenu);
        return menuBar;
    }

    private void addMenuItem(java.awt.Menu menu, String label, JButton source) {
        MenuItem item = new MenuItem(label);
        item.addActionListener(e -> source.doClick());
        menu.add(item);
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

        ij.ImagePlus currentImage = ij.WindowManager.getCurrentImage();
        if (analysisImageOverride == null || currentImage == analysisImageOverride) {
            NanoToolProjectUtility.syncProjectOverlay();
        }
        ij.plugin.frame.RoiManager roiManager = ij.plugin.frame.RoiManager.getInstance2();
        int count = roiManager == null ? 0 : roiManager.getCount();
        dashboard.roiCountLabel.setText("Nanoparticles selected: " + count);
        dashboard.updateScaleLabel();
        dashboard.updateMeanFilterCount();
        dashboard.updateImageName();
        dashboard.updateButtonStates();
    }

    public static void updateScaleLabel() {
        if (dashboard == null || dashboard.scaleLabel == null) {
            return;
        }

        ij.ImagePlus imp = getDashboardImage();
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
        ij.ImagePlus imp = getDashboardImage();
        int count = NanoToolProjectUtility.getMeanFilterCount(imp);
        dashboard.meanFilterCountLabel.setText("Mean filter applied: " + count + " time"
                + (count == 1 ? "" : "s"));
    }

    private void updateImageName() {
        ij.ImagePlus imp = getDashboardImage();
        imageNameLabel.setText(imp == null ? "Image: no image open" : "Image: " + imp.getTitle());
    }

    private void updateButtonStates() {
        ij.ImagePlus imp = getDashboardImage();
        boolean hasImage = imp != null;
        ij.plugin.frame.RoiManager rm = ij.plugin.frame.RoiManager.getInstance2();
        boolean hasRois = rm != null && rm.getCount() > 0;
        boolean hasResults = NanoToolStatisticsUtility.hasEquivalentDiameterResults();
        boolean hasProject = NanoToolProjectUtility.getCurrentProjectFile() != null;

        btnSetScale.setEnabled(hasImage);
        btnMeanFilter.setEnabled(hasImage);
        btnWandTool.setEnabled(hasImage);
        btnWandSettings.setEnabled(hasImage);
        btnMeasure.setEnabled(hasImage && hasRois);
        btnMeasureSettings.setEnabled(hasImage);
        btnStatistics.setEnabled(hasImage && hasResults);
        btnSave.setEnabled(hasImage && hasProject);
        btnSaveAs.setEnabled(hasImage);
        btnCloseProject.setEnabled(hasProject);
        btnExportMeasurements.setEnabled(hasImage && hasResults);
        btnExportRois.setEnabled(hasImage && hasRois);
        btnImport.setEnabled(hasImage);
    }

    public static void setAnalysisImage(ij.ImagePlus image) {
        analysisImageOverride = image;
    }

    public static void clearAnalysisImage() {
        analysisImageOverride = null;
    }

    public static ij.ImagePlus getAnalysisImage() {
        return analysisImageOverride;
    }

    private static ij.ImagePlus getDashboardImage() {
        if (analysisImageOverride != null) {
            return analysisImageOverride;
        }
        return ij.WindowManager.getCurrentImage();
    }

    private void setStatus(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
        }
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
        size.width = 260;
        b.setPreferredSize(size);
        b.setMaximumSize(new Dimension(260, size.height));
        b.addActionListener(listener);
        return b;
    }

    private void runMagicWand(ActionEvent e) {
        wandTool.run("");
        setStatus("Magic Wand active - click the image.");
        ij.IJ.showStatus("Magic Wand active - click the image");
    }

    private void openWandSettings(ActionEvent e) {
        wandTool.showOptionsDialog();
        setStatus("Selection tool settings opened.");
    }

    private void openImageOrProject() {
        ij.io.OpenDialog dialog = new ij.io.OpenDialog("Open Image or Project", "", "");
        String directory = dialog.getDirectory();
        String fileName = dialog.getFileName();
        if (directory == null || fileName == null || fileName.trim().isEmpty()) {
            return;
        }

        java.io.File selectedFile = new java.io.File(directory, fileName);
        if (fileName.toLowerCase().endsWith(".ntproj")) {
            if (NanoToolProjectUtility.loadProjectFile(selectedFile) != null) {
                updateRoiCount();
                updateScaleLabel();
                setStatus("Project opened.");
            } else {
                setStatus("Unable to open project.");
            }
            return;
        }

        ij.ImagePlus image = ij.IJ.openImage(directory + fileName);
        if (image == null) {
            ij.IJ.showMessage("NanoTool", "The selected file could not be opened as an image.");
            setStatus("Unable to open image.");
            return;
        }

        NanoToolProjectUtility.setCurrentProjectFile(null);
        clearAnalysisImage();
        ij.plugin.frame.RoiManager roiManager = ij.plugin.frame.RoiManager.getInstance2();
        if (roiManager != null) {
            roiManager.reset();
        }
        image.show();
        updateRoiCount();
        setStatus("Image opened.");
    }
}