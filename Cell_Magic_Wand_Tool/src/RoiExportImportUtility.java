
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

    public static void exportData() {
        ImagePlus imp = WindowManager.getCurrentImage();
        if (imp == null) {
            IJ.showMessage("Export NanoTool Data", "No images are open.");
            return;
        }

        RoiManager rm = RoiManager.getInstance2();
        int roiCount = rm == null ? 0 : rm.getCount();
        ResultsTable rt = Analyzer.getResultsTable();
        int resultsCount = rt == null ? 0 : rt.getCounter();

        if (roiCount == 0 && resultsCount == 0) {
            IJ.showMessage("Export NanoTool Data", "No ROIs or measurement results available to export.");
            return;
        }

        GenericDialog gd = new GenericDialog("Export Options");
        gd.addCheckbox("Export measurements (.txt)", true);
        gd.addCheckbox("Export ROIs (.zip)", true);
        gd.showDialog();

        if (gd.wasCanceled()) {
            return;
        }

        boolean exportResults = gd.getNextBoolean();
        boolean exportRois = gd.getNextBoolean();

        if (!exportResults && !exportRois) {
            IJ.showMessage("Export NanoTool Data", "No export items were selected.");
            return;
        }

        if (exportResults && resultsCount == 0 && roiCount > 0) {
            // Automatically calculate measurements if ROIs exist but have not been measured yet
            EqDiameterUtility.run();
            rt = Analyzer.getResultsTable();
            resultsCount = rt == null ? 0 : rt.getCounter();
        }

        String title = imp.getTitle();
        String baseName = title.contains(".") ? title.substring(0, title.lastIndexOf('.')) : title;

        SaveDialog sd = new SaveDialog("Export NanoTool Data", baseName, "");
        String dir = sd.getDirectory();
        String filename = sd.getFileName();

        if (dir == null || filename == null || filename.trim().isEmpty()) {
            return;
        }

        // Strip extension if user entered one, to build base name
        if (filename.contains(".")) {
            filename = filename.substring(0, filename.lastIndexOf('.'));
        }

        boolean autoRenamed = false;
        File savedTxtFile = null;
        File savedZipFile = null;

        if (exportResults) {
            if (rt == null || resultsCount == 0) {
                IJ.showMessage("Export NanoTool Data", "Results table is empty. Could not export measurements.");
            } else {
                File targetFile = getUniqueFile(dir, filename + "_measurements", ".txt");
                if (!targetFile.getName().equals(filename + "_measurements.txt")) {
                    autoRenamed = true;
                }
                try {
                    rt.save(targetFile.getAbsolutePath());
                    savedTxtFile = targetFile;
                } catch (Exception ex) {
                    IJ.showMessage("Export NanoTool Data", "Error saving measurements file: " + ex.getMessage());
                }
            }
        }

        if (exportRois) {
            if (rm == null || roiCount == 0) {
                IJ.showMessage("Export NanoTool Data", "ROI Manager is empty. Could not export ROIs.");
            } else {
                File targetFile = getUniqueFile(dir, filename + "_rois", ".zip");
                if (!targetFile.getName().equals(filename + "_rois.zip")) {
                    autoRenamed = true;
                }
                try {
                    rm.runCommand("Save", targetFile.getAbsolutePath());
                    savedZipFile = targetFile;
                } catch (Exception ex) {
                    IJ.showMessage("Export NanoTool Data", "Error saving ROIs file: " + ex.getMessage());
                }
            }
        }

        StringBuilder msg = new StringBuilder("Export successful!\n\nSaved files:\n");
        if (savedTxtFile != null) {
            msg.append("- ").append(savedTxtFile.getName()).append("\n");
        }
        if (savedZipFile != null) {
            msg.append("- ").append(savedZipFile.getName()).append("\n");
        }
        msg.append("\nDirectory: ").append(dir);

        if (autoRenamed) {
            msg.append("\n\nNote: Existing files with the same name were found, so incremental suffixes were added automatically to avoid overwriting.");
        }

        IJ.showStatus("Export complete.");
        IJ.showMessage("Export NanoTool Data", msg.toString());
    }

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
        String imgBaseName = imageTitle.contains(".") ? imageTitle.substring(0, imageTitle.lastIndexOf('.')) : imageTitle;

        String roiBaseName = filename.contains(".") ? filename.substring(0, filename.lastIndexOf('.')) : filename;
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
            IJ.showMessage("Import ROIs", "Successfully imported ROIs from:\n" + filename + "\nTotal ROIs in ROI Manager: " + count);
        } catch (Exception ex) {
            IJ.showMessage("Import ROIs", "Error importing ROIs: " + ex.getMessage());
        }
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
