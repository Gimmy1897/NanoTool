import ij.ImageListener;
import ij.ImagePlus;
import ij.plugin.PlugIn;

import java.io.File;

public class NanoToolStartup implements PlugIn, ImageListener {

    private static boolean installed;
    private static final NanoToolStartup INSTANCE = new NanoToolStartup();

    @Override
    public void run(String arg) {
        install();
    }

    public static synchronized void install() {
        if (installed) {
            return;
        }
        installed = true;
        ImagePlus.addImageListener(INSTANCE);
    }

    @Override
    public void imageOpened(ImagePlus imp) {
        if (isNanoToolProject(imp)) {
            NanoToolProjectUtility.initializeOpenedProject(imp);
        }
    }

    @Override
    public void imageClosed(ImagePlus imp) {
    }

    @Override
    public void imageUpdated(ImagePlus imp) {
    }

    private boolean isNanoToolProject(ImagePlus imp) {
        if (imp == null) {
            return false;
        }
        String title = imp.getTitle();
        if (title != null && title.toLowerCase().endsWith(".ntproj")) {
            return true;
        }
        if (imp.getOriginalFileInfo() != null) {
            String name = imp.getOriginalFileInfo().fileName;
            return name != null && name.toLowerCase().endsWith(".ntproj");
        }
        return false;
    }
}
