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
        ImagePlus imp = WindowManager.getCurrentImage();
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
        ImagePlus imp = WindowManager.getCurrentImage();
        if (imp == null || currentProjectFile == null) {
            return;
        }

        RoiManager rm = RoiManager.getInstance2();
        if (rm != null) {
            rm.close();
        }

        java.awt.Frame results = WindowManager.getFrame("Results");
        if (results != null) {
            results.dispose();
        }

        imp.close();
        currentProjectFile = null;
        overlayImage = null;
        overlaySignature = null;
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
        if (overlay != null && overlay.size() > 0) {
            RoiManager rm = RoiManager.getInstance2();
            if (rm == null) {
                rm = new RoiManager();
            }
            rm.reset();
            for (int i = 0; i < overlay.size(); i++) {
                rm.addRoi(overlay.get(i));
            }
            rm.runCommand(imp, "Show All");
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
        if (imp == null || currentProjectFile == null) {
            return;
        }

        RoiManager rm = RoiManager.getInstance2();
        Roi[] rois = rm == null ? new Roi[0] : rm.getRoisAsArray();
        String signature = getRoiSignature(rois);
        if (imp == overlayImage && signature.equals(overlaySignature)) {
            return;
        }

        Overlay overlay = new Overlay();
        for (int i = 0; i < rois.length; i++) {
            overlay.add(rois[i]);
        }

        imp.setOverlay(overlay.size() == 0 ? null : overlay);
        imp.updateAndDraw();
        overlayImage = imp;
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
        ImagePlus imp = WindowManager.getCurrentImage();
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
