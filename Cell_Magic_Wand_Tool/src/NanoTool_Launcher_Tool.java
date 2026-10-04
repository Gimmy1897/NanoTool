import ij.ImagePlus;
import ij.plugin.tool.PlugInTool;

import java.awt.Color;
import java.awt.Image;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

public class NanoTool_Launcher_Tool extends PlugInTool {

    private static final String TOOL_ICON = "C555D70Db0D31D41D71D81Db1D42D52D72D82Da2Db2D43D53D73D83Da3De3D04D14D24D54D64D94Da4Dc4Dd4De4D25D35D45Db5Dc5Dd5D46Db6Dc6D17D27D37Dc7Dd7De7Df7D08D18D28D38Dc8Dd8De8D49Db9D2aD3aD4aDbaDcaDdaD1bD2bD3bD5bD6bD9bDabDdbDebD1cD5cD7cD8cD9cDacDbcD4dD5dD7dD8dDadDbdD4eD7eD8eDbeD8fC128D74D84D55D65D95Da5D56Da6D47Db7D48Db8D59Da9D5aD6aD9aDaaD7bD8bC23aD85D66Da7D69D99C24dD76D86D97D78C139D75Da8D8aC239D57D58D7aC24cD67D98D89C35eD77D88C24bD68D79C23bD96C35fD87";

    public NanoTool_Launcher_Tool() {
    }

    @Override
    public void run(String arg) {
        showDashboard();
    }

    public void mousePressed(ImagePlus imp, MouseEvent e) {
        showDashboard();
    }

    public void showOptionsDialog() {
        showDashboard();
    }

    private void showDashboard() {
        NanoToolDashboard.showDashboard();
    }

    public String getToolIcon() {
        return TOOL_ICON;
    }

    public static Image getToolIconImage() {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Color color = new Color(0, 0, 0, 0);
        int index = 0;
        while (index < TOOL_ICON.length()) {
            char command = TOOL_ICON.charAt(index++);
            if (command == 'C') {
                int red = Character.digit(TOOL_ICON.charAt(index++), 16) * 16;
                int green = Character.digit(TOOL_ICON.charAt(index++), 16) * 16;
                int blue = Character.digit(TOOL_ICON.charAt(index++), 16) * 16;
                color = new Color(red, green, blue);
            } else if (command == 'D') {
                int x = Character.digit(TOOL_ICON.charAt(index++), 16);
                int y = Character.digit(TOOL_ICON.charAt(index++), 16);
                image.setRGB(x, y, color.getRGB());
            }
        }
        return image;
    }

    public String getToolName() {
        return "NanoTool Dashboard Tool";
    }
}