// Eq. Diameter Utility
// Measures the ROIs in the ROI Manager and adds an "Eq. Diameter" column.

import ij.IJ;
import ij.ImagePlus;
import ij.measure.Measurements;
import ij.plugin.filter.Analyzer;
import ij.plugin.frame.RoiManager;

public class EqDiameterUtility {

    private EqDiameterUtility() {}

    public static void run() {
        ImagePlus image = ij.WindowManager.getCurrentImage();
        if (image == null) {
            IJ.showMessage("NanoTool", "No images are open.");
            return;
        }

        RoiManager roiManager = RoiManager.getInstance2();
        if (roiManager == null || roiManager.getCount() == 0) {
            IJ.showMessage("NanoTool", "No ROIs are available in the ROI Manager.");
            return;
        }

        ensureAreaMeasurement();

        String macroCode =
                "roiManager(\"Measure\");\n" +
                "for (i = 0; i < nResults; i++) {\n" +
                "    area = getResult(\"Area\", i);\n" +
                "    if (area > 0) {\n" +
                "        d_eq = 2.0 * sqrt(area / PI);\n" +
                "        setResult(\"Eq. Diameter\", i, d_eq);\n" +
                "    }\n" +
                "}\n" +
                "updateResults();\n";

        IJ.runMacro(macroCode);
        IJ.showStatus("Equivalent diameter calculated.");
    }

    private static void ensureAreaMeasurement() {
        int current = Analyzer.getMeasurements();
        if ((current & Measurements.AREA) == 0) {
            Analyzer.setMeasurements(current | Measurements.AREA);
        }
    }
}