// Mean Filter Utility
// Applies the Mean filter (Process > Filters > Mean...) with a 2 px radius
// to the current image.

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;

public class MeanFilterUtility {

    private static final double DEFAULT_RADIUS = 2.0;

    private MeanFilterUtility() {}

    public static void run() {
        run(DEFAULT_RADIUS);
    }

    public static void run(double radius) {
        ImagePlus imp = WindowManager.getCurrentImage();
        if (imp == null) {
            IJ.showMessage("NanoTool", "No images are open.");
            return;
        }

        IJ.run(imp, "Mean...", "radius=" + radius);
        imp.updateAndDraw();

        IJ.showStatus("Mean filter applied (radius=" + radius + " px).");
        IJ.showMessage("NanoTool", "Mean filter " + radius + " px applied to image " + imp.getTitle() + ".");
    }
}