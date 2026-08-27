import ij.plugin.PlugIn;
import ij.plugin.frame.PlugInFrame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class NanoToolDashboard extends PlugInFrame implements PlugIn {

    private static NanoToolDashboard dashboard;
    private final Cell_Magic_Wand_Tool wandTool = new Cell_Magic_Wand_Tool();
    private final Timer roiCountTimer = new Timer(250, e -> {
        updateRoiCount();
        keepOnTop();
    });
    private JLabel roiCountLabel;

    public NanoToolDashboard() {
        super("NanoTool");
        dashboard = this;
        setIconImage(NanoTool_Launcher_Tool.getToolIconImage());
        try {
            ij.gui.Toolbar.addPlugInTool(wandTool);
            ij.gui.Toolbar.addPlugInTool(new NanoTool_Launcher_Tool());
        } catch (Exception ignored) {}
        buildUI();
        pack();
        setResizable(false);
        setAlwaysOnTop(true);
        setLocationRelativeTo(null);
        setVisible(true);
        updateRoiCount();
        keepOnTop();
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
        keepOnTop();
    }

    private void buildUI() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        panel.add(makeButton("Set scale (from TEM .tiff metadata)", e -> TemScaleUtility.run()));
        panel.add(Box.createVerticalStrut(6));
        panel.add(makeButton("Apply Mean filter (2 px radius)", e -> MeanFilterUtility.run()));
        panel.add(Box.createVerticalStrut(15));
        panel.add(makeButton("Selection Tool (Cell Magic Wand)", this::runMagicWand));
        panel.add(Box.createVerticalStrut(6));
        panel.add(makeButton("Cell Magic Wand settings", this::openWandSettings));
        panel.add(Box.createVerticalStrut(12));
        panel.add(makeButton("Measure nanoparticles", e -> EqDiameterUtility.run()));
        panel.add(Box.createVerticalStrut(6));
        roiCountLabel = new JLabel();
        roiCountLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(roiCountLabel);

        add(panel);
    }

    public static void updateRoiCount() {
        if (dashboard == null || dashboard.roiCountLabel == null) {
            return;
        }

        ij.plugin.frame.RoiManager roiManager = ij.plugin.frame.RoiManager.getInstance();
        int count = roiManager == null ? 0 : roiManager.getCount();
        dashboard.roiCountLabel.setText("Nanoparticles selected: " + count);
    }

    private void keepOnTop() {
        if (isVisible() && !isAlwaysOnTop()) {
            setAlwaysOnTop(true);
        }
        if (isVisible()) {
            toFront();
        }
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