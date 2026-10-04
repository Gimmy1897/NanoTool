
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.io.OpenDialog;
import ij.io.SaveDialog;
import ij.measure.ResultsTable;
import ij.plugin.filter.Analyzer;
import ij.plugin.frame.RoiManager;

import java.io.File;

public class RoiExportImportUtility {

    private RoiExportImportUtility() {}

    // -------------------------------------------------------------------------
    // Export Measurements (.txt)
    // -------------------------------------------------------------------------

    public static void exportMeasurements() {
        ImagePlus imp = WindowManager.getCurrentImage();
        if (imp == null) {
            IJ.showMessage("Export Measurements", "No images are open.");
            return;
        }

        ResultsTable rt = Analyzer.getResultsTable();
        int resultsCount = rt == null ? 0 : rt.getCounter();

        // If no results yet but ROIs exist, measure automatically
        if (resultsCount == 0) {
            RoiManager rm = RoiManager.getInstance2();
            if (rm != null && rm.getCount() > 0) {
                EqDiameterUtility.run();
                rt = Analyzer.getResultsTable();
                resultsCount = rt == null ? 0 : rt.getCounter();
            }
        }

        if (resultsCount == 0) {
            IJ.showMessage("Export Measurements", "No measurement results available.\nRun \"Measure nanoparticles\" first.");
            return;
        }

        String baseName = stripExtension(imp.getTitle());
        SaveDialog sd = new SaveDialog("Export Measurements", baseName + "_measurements", ".txt");
        String dir = sd.getDirectory();
        String name = sd.getFileName();

        if (dir == null || name == null || name.trim().isEmpty()) {
            return;
        }

        if (!name.toLowerCase().endsWith(".txt")) {
            name = name + ".txt";
        }

        File target = getUniqueFile(dir, stripExtension(name), ".txt");
        boolean renamed = !target.getName().equals(name);

        try {
            rt.save(target.getAbsolutePath());
            String msg = "Measurements saved to:\n" + target.getAbsolutePath();
            if (renamed) {
                msg += "\n\nNote: A file with the same name already existed. Saved with a different name to avoid overwriting.";
            }
            IJ.showStatus("Measurements exported.");
            IJ.showMessage("Export Measurements", msg);
        } catch (Exception e) {
            IJ.showMessage("Export Measurements", "Error saving measurements: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Export ROIs (.zip)
    // -------------------------------------------------------------------------

    public static void exportRois() {
        ImagePlus imp = WindowManager.getCurrentImage();
        if (imp == null) {
            IJ.showMessage("Export ROIs", "No images are open.");
            return;
        }

        RoiManager rm = RoiManager.getInstance2();
        if (rm == null || rm.getCount() == 0) {
            IJ.showMessage("Export ROIs", "ROI Manager is empty. No ROIs to export.");
            return;
        }

        String baseName = stripExtension(imp.getTitle());
        SaveDialog sd = new SaveDialog("Export ROIs", baseName + "_rois", ".zip");
        String dir = sd.getDirectory();
        String name = sd.getFileName();

        if (dir == null || name == null || name.trim().isEmpty()) {
            return;
        }

        if (!name.toLowerCase().endsWith(".zip")) {
            name = name + ".zip";
        }

        File target = getUniqueFile(dir, stripExtension(name), ".zip");
        boolean renamed = !target.getName().equals(name);

        try {
            rm.runCommand("Save", target.getAbsolutePath());
            String msg = "ROIs saved to:\n" + target.getAbsolutePath();
            if (renamed) {
                msg += "\n\nNote: A file with the same name already existed. Saved with a different name to avoid overwriting.";
            }
            IJ.showStatus("ROIs exported.");
            IJ.showMessage("Export ROIs", msg);
        } catch (Exception e) {
            IJ.showMessage("Export ROIs", "Error saving ROIs: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Import ROIs (.zip / .roi)
    // -------------------------------------------------------------------------

    public static void importRois() {
        ImagePlus imp = WindowManager.getCurrentImage();
        if (imp == null) {
            IJ.showMessage("Import ROIs", "No images are open.");
            return;
        }

        OpenDialog od = new OpenDialog("Select Saved ROIs (.zip or .roi)", "", "");
        String dir = od.getDirectory();
        String filename = od.getFileName();

        if (dir == null || filename == null || filename.trim().isEmpty()) {
            return;
        }

        String filePath = dir + filename;
        File file = new File(filePath);
        if (!file.exists()) {
            IJ.showMessage("Import ROIs", "Selected file does not exist.");
            return;
        }

        // Check if ROI filename matches active image title
        String imageTitle = imp.getTitle();
        String imgBaseName = stripExtension(imageTitle);

        String roiBaseName = stripExtension(filename);
        String cleanedRoiBase = roiBaseName;
        if (cleanedRoiBase.toLowerCase().endsWith("_rois")) {
            cleanedRoiBase = cleanedRoiBase.substring(0, cleanedRoiBase.length() - 5);
        } else if (cleanedRoiBase.toLowerCase().matches(".*_rois_\\d+$")) {
            cleanedRoiBase = cleanedRoiBase.substring(0, cleanedRoiBase.lastIndexOf("_rois_"));
        }

        boolean nameMatch = imgBaseName.equalsIgnoreCase(roiBaseName)
                || imgBaseName.equalsIgnoreCase(cleanedRoiBase)
                || imgBaseName.toLowerCase().startsWith(cleanedRoiBase.toLowerCase())
                || cleanedRoiBase.toLowerCase().startsWith(imgBaseName.toLowerCase());

        if (!nameMatch) {
            GenericDialog gd = new GenericDialog("ROI Name Mismatch Warning");
            gd.addMessage("Warning: The selected ROI filename does not match the active image title.\n\n"
                    + "Active Image: " + imageTitle + "\n"
                    + "Selected ROI File: " + filename + "\n\n"
                    + "Do you want to continue importing these ROIs anyway?");
            gd.setOKLabel("Continue");
            gd.setCancelLabel("Cancel");
            gd.showDialog();

            if (gd.wasCanceled()) {
                return;
            }
        }

        RoiManager rm = RoiManager.getInstance2();
        if (rm == null) {
            rm = new RoiManager();
        }

        try {
            rm.runCommand("Open", filePath);
            rm.runCommand(imp, "Show All");
            int count = rm.getCount();
            IJ.showStatus("Imported ROIs successfully. Total ROIs: " + count);
            IJ.showMessage("Import ROIs",
                    "Successfully imported ROIs from:\n" + filename
                    + "\nTotal ROIs in ROI Manager: " + count);
        } catch (Exception ex) {
            IJ.showMessage("Import ROIs", "Error importing ROIs: " + ex.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private static String stripExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }

    private static File getUniqueFile(String dir, String prefix, String extension) {
        File file = new File(dir, prefix + extension);
        if (!file.exists()) {
            return file;
        }
        int counter = 1;
        while (true) {
            File candidate = new File(dir, prefix + "_" + counter + extension);
            if (!candidate.exists()) {
                return candidate;
            }
            counter++;
        }
    }
}
