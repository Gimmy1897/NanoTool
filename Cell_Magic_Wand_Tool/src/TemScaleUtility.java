// TEM Scale Utility
// Sets the image scale using the XpixCal value from TEM metadata

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TemScaleUtility {

    private static final Pattern XPIXCAL_PATTERN =
            Pattern.compile("XpixCal\\s*[:=]\\s*([\\d.]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern UNIT_PATTERN =
            Pattern.compile("Unit\\s*[:=]\\s*([a-zA-Z\\u00B5]+)", Pattern.CASE_INSENSITIVE);

    private TemScaleUtility() {}

    public static void run() {
        ImagePlus imp = WindowManager.getCurrentImage();
        if (imp == null) {
            IJ.showMessage("TEM Scale Utilities", "No images are open.");
            return;
        }

        String infoProp = String.valueOf(imp.getProperty("Info"));
        String descProp = String.valueOf(imp.getProperty("Description"));
        String title = imp.getTitle();

        String fullText = infoProp + "\n" + descProp + "\n" + title;

        Matcher m = XPIXCAL_PATTERN.matcher(fullText);
        if (!m.find()) {
            IJ.showMessage("TEM Scale Utilities", "Numeric XpixCal value not found.");
            return;
        }
        String xval = m.group(1);

        Matcher u = UNIT_PATTERN.matcher(fullText);
        String unit = u.find() ? u.group(1) : "nm";

        // The measured distance is the value read from the file; the known distance is 1.
        IJ.run(imp, "Set Scale...", "distance=" + xval + " known=1 unit=" + unit);

        IJ.showStatus("Scale set: " + xval + " px/" + unit);
        IJ.showMessage("TEM Scale Utilities", "Scale set to " + xval + " px/" + unit
            + " for image " + title + ".");
    }
}