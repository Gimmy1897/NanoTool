// NanoTool Project Utility

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Roi;
import ij.io.FileSaver;
import ij.io.OpenDialog;
import ij.io.SaveDialog;
import ij.gui.Overlay;
import ij.plugin.frame.RoiManager;

import java.io.*;

public class NanoToolProjectUtility {

    private static final String MEAN_FILTER_COUNT_PROPERTY = "NanoTool.MeanFilterCount";
    private static final String IMAGE_EVENT_HANDLED_PROPERTY = "NanoTool.ImageEventHandled";
    /** Path of the currently open .ntproj file, or null if unsaved. */
    private static File currentProjectFile = null;
    private static ImagePlus overlayImage;
    private static String overlaySignature;

    private NanoToolProjectUtility() {}

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /** Returns the path of the currently open project file (may be null). */
    public static File getCurrentProjectFile() {
        return currentProjectFile;
    }

    public static ImagePlus getProjectImage() {
        return overlayImage;
    }

    /** Sets the current project file (called when opening or saving). */
    public static void setCurrentProjectFile(File f) {
        currentProjectFile = f;
    }

    public static int getMeanFilterCount(ImagePlus imp) {
        if (imp == null) {
            return 0;
        }
        Object value = imp.getProperty(MEAN_FILTER_COUNT_PROPERTY);
        if (value == null) {
            return 0;
        }
        try {
            return Math.max(0, Integer.parseInt(value.toString()));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static boolean isImageEventHandled(ImagePlus imp) {
        return imp != null && Boolean.TRUE.equals(imp.getProperty(IMAGE_EVENT_HANDLED_PROPERTY));
    }

    public static void markImageEventHandled(ImagePlus imp) {
        if (imp != null) {
            imp.setProperty(IMAGE_EVENT_HANDLED_PROPERTY, Boolean.TRUE);
        }
    }

    public static void incrementMeanFilterCount(ImagePlus imp) {
        if (imp == null) {
            return;
        }
        imp.setProperty(MEAN_FILTER_COUNT_PROPERTY, Integer.toString(getMeanFilterCount(imp) + 1));
        NanoToolDashboard.updateMeanFilterCount();
    }

    /**
     * Save Project: overwrites the current .ntproj file if one is open,
     * otherwise falls back to Save Project As.
     */
    public static void saveProject() {
        if (currentProjectFile != null) {
            saveToFile(currentProjectFile);
        } else {
            saveProjectAs();
        }
    }

    /**
     * Save Project As: always asks the user for a file path.
     */
public static void saveProjectAs() {
        ImagePlus imp = NanoToolDashboard.getDashboardImage();
        if (imp == null) {
            IJ.showMessage("Save Project", "No image is open.");
            return;
        }

        String defaultName = baseName(imp.getTitle());
        SaveDialog sd = new SaveDialog("Save NanoTool Project", defaultName, ".ntproj");
        String dir = sd.getDirectory();
        String name = sd.getFileName();

        if (dir == null || name == null || name.trim().isEmpty()) {
            return;
        }

        if (!name.toLowerCase().endsWith(".ntproj")) {
            name = name + ".ntproj";
        }

        File dest = new File(dir, name);
        if (saveToFile(dest)) {
            currentProjectFile = dest;
        }
    }

    public static void closeProject() {
        closeProject(overlayImage);
    }

    public static void closeProject(ImagePlus projectImage) {
        ImagePlus imp = projectImage;
        if (imp == null) {
            imp = NanoToolDashboard.getAnalysisImage();
        }
        if (imp == null) {
            imp = WindowManager.getCurrentImage();
        }
        if (imp == null || currentProjectFile == null) {
            return;
        }

        // Close all open windows except ImageJ itself and the NanoTool dashboard
        java.awt.Frame[] frames = java.awt.Frame.getFrames();
        for (java.awt.Frame f : frames) {
            String title = f.getTitle();
            if (title != null && !title.equals("NanoTool") && !title.equals("ImageJ")) {
                f.dispose();
            }
        }

        RoiManager rm = RoiManager.getInstance2();
        if (rm != null) {
            rm.close();
        }

        // Reset state before closing the image to avoid syncProjectOverlay errors
        currentProjectFile = null;
        overlayImage = null;
        overlaySignature = null;
        NanoToolDashboard.clearAnalysisImage();

        imp.close();
        NanoToolDashboard.updateRoiCount();
    }

    /**
     * Open Project: asks the user to pick a .ntproj file, then loads it.
     */
    public static void openProject() {
        // OpenDialog's third argument is the default file name, not a file filter.
        // Passing "*.ntproj" therefore puts the wildcard in the file-name field.
        OpenDialog od = new OpenDialog("Open NanoTool Project (.ntproj)", "", "");
        String dir = od.getDirectory();
        String name = od.getFileName();

        if (dir == null || name == null || name.trim().isEmpty()) {
            return;
        }
        if (!name.toLowerCase().endsWith(".ntproj")) {
            IJ.showMessage("Open Project", "Please select a .ntproj project file.");
            return;
        }

        File file = new File(dir, name);
        if (!file.exists()) {
            IJ.showMessage("Open Project", "Selected file does not exist.");
            return;
        }

        if (!NanoToolDashboard.confirmCloseForNewProject()) {
            return;
        }
        loadProjectFile(file);
    }

    /** Loads a TIFF-based .ntproj file through ImageJ's normal opener. */
    public static ImagePlus loadProjectFile(File ntprojFile) {
        return loadProjectFile(ntprojFile, true);
    }

    public static ImagePlus loadProjectFile(File ntprojFile, boolean showImage) {
        ImagePlus imp = IJ.openImage(ntprojFile.getAbsolutePath());
        if (imp == null) {
            IJ.showMessage("Open Project", "The project is not a valid ImageJ TIFF file.");
            return null;
        }

        imp.setTitle(baseName(ntprojFile.getName()));
        markImageEventHandled(imp);
        if (showImage) {
            imp.show();
        }

        initializeOpenedProject(imp);
        return imp;
    }

    public static void initializeOpenedProject(ImagePlus imp) {
        if (imp == null) {
            return;
        }

        Overlay overlay = imp.getOverlay();
        imp.deleteRoi();
        imp.setOverlay(null);

        RoiManager rm = RoiManager.getInstance2();
        if (rm == null) {
            rm = new RoiManager();
        } else {
            rm.runCommand("Show None");
            rm.reset();
        }

        if (overlay != null && overlay.size() > 0) {
            for (int i = 0; i < overlay.size(); i++) {
                rm.addRoi(overlay.get(i));
            }
            rm.runCommand(imp, "Show All");
        } else {
            imp.deleteRoi();
            imp.setOverlay(null);
        }

        if (imp.getOriginalFileInfo() != null
                && imp.getOriginalFileInfo().directory != null
                && imp.getOriginalFileInfo().fileName != null) {
            currentProjectFile = new File(imp.getOriginalFileInfo().directory,
                    imp.getOriginalFileInfo().fileName);
        }
        restoreMeanFilterCountFromInfo(imp);
        overlayImage = imp;
        overlaySignature = null;
        NanoToolDashboard.showDashboard();
        NanoToolDashboard.updateRoiCount();

        IJ.showStatus("Project loaded: " + imp.getTitle());
    }

    public static void syncProjectOverlay() {
        ImagePlus imp = WindowManager.getCurrentImage();
        if (imp == null || currentProjectFile == null || imp != overlayImage) {
            return;
        }

        RoiManager rm = RoiManager.getInstance2();
        Roi[] rois = rm == null ? new Roi[0] : rm.getRoisAsArray();
        String signature = getRoiSignature(rois);
        if (signature.equals(overlaySignature)) {
            return;
        }

        Overlay overlay = new Overlay();
        for (int i = 0; i < rois.length; i++) {
            overlay.add(rois[i]);
        }

        imp.setOverlay(overlay.size() == 0 ? null : overlay);
        imp.updateAndDraw();
        overlaySignature = signature;
    }

    private static String getRoiSignature(Roi[] rois) {
        StringBuilder signature = new StringBuilder();
        for (int i = 0; i < rois.length; i++) {
            Roi roi = rois[i];
            java.awt.Rectangle bounds = roi.getBounds();
            signature.append(roi.getType()).append(':')
                    .append(bounds.x).append(',').append(bounds.y).append(',')
                    .append(bounds.width).append(',').append(bounds.height).append(':')
                    .append(roi.getPosition()).append(';');
        }
        return signature.toString();
    }

    private static void restoreMeanFilterCountFromInfo(ImagePlus imp) {
        Object info = imp.getProperty("Info");
        if (info == null) {
            return;
        }
        String[] lines = info.toString().split("\\r?\\n");
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].startsWith(MEAN_FILTER_COUNT_PROPERTY + "=")) {
                String value = lines[i].substring((MEAN_FILTER_COUNT_PROPERTY + "=").length());
                try {
                    imp.setProperty(MEAN_FILTER_COUNT_PROPERTY, Integer.toString(Math.max(0,
                            Integer.parseInt(value.trim()))));
                } catch (NumberFormatException ignored) {
                    return;
                }
                return;
            }
        }
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /** Saves the current session as a TIFF-based .ntproj project. */
private static boolean saveToFile(File dest) {
        ImagePlus imp = overlayImage;
        if (imp == null) {
            imp = NanoToolDashboard.getDashboardImage();
        }
        if (imp == null) {
            IJ.showMessage("Save Project", "No image is open.");
            return false;
        }

        try {
            String info = imp.getProperty("Info") == null ? "" : imp.getProperty("Info").toString();
            String[] infoLines = info.split("\\r?\\n");
            StringBuilder cleanedInfo = new StringBuilder();
            for (int i = 0; i < infoLines.length; i++) {
                if (!infoLines[i].startsWith(MEAN_FILTER_COUNT_PROPERTY + "=")
                        && infoLines[i].trim().length() > 0) {
                    if (cleanedInfo.length() > 0) {
                        cleanedInfo.append('\n');
                    }
                    cleanedInfo.append(infoLines[i]);
                }
            }
            if (cleanedInfo.length() > 0) {
                cleanedInfo.append('\n');
            }
            cleanedInfo.append(MEAN_FILTER_COUNT_PROPERTY)
                    .append('=').append(getMeanFilterCount(imp));
            imp.setProperty("Info", cleanedInfo.toString());

            Overlay originalOverlay = imp.getOverlay();
            Overlay projectOverlay = new Overlay();
            RoiManager rm = RoiManager.getInstance2();
            if (rm != null) {
                Roi[] rois = rm.getRoisAsArray();
                for (int i = 0; i < rois.length; i++) {
                    projectOverlay.add(rois[i]);
                }
            }
            if (projectOverlay.size() == 0 && originalOverlay != null) {
                for (int i = 0; i < originalOverlay.size(); i++) {
                    projectOverlay.add(originalOverlay.get(i));
                }
            }
            imp.setOverlay(projectOverlay);
            FileSaver fs = new FileSaver(imp);
            boolean saved = fs.saveAsTiff(dest.getAbsolutePath());
            imp.setOverlay(originalOverlay);
            if (!saved) {
                throw new IOException("ImageJ could not save the TIFF project.");
            }

            IJ.showStatus("Project saved: " + dest.getName());
            IJ.showMessage("Save Project",
                    "Project saved successfully.\n\nFile: " + dest.getAbsolutePath());
            return true;

        } catch (Exception e) {
            IJ.showMessage("Save Project", "Error saving project: " + e.getMessage());
            return false;
        }
    }

    /** Strips file extension and returns a clean base name. */
    private static String baseName(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }

}

