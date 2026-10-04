// Theo Walker
// Max Planck Florida Institute
// naroom@gmail.com

package cellMagicWand;

import ij.ImagePlus;
import ij.io.FileSaver;
import ij.io.Opener;
import ij.process.ShortProcessor;

public class RingTest {

    public static void test() {
        String testDataDir = "C:/netbeans-projects/Polar_Cell/src/testData/";
        String imagePath = testDataDir + "testCircles.tif";
        Opener opener = new Opener();
        ImagePlus image = opener.openImage(imagePath);

        int[][] circleCenters = new int[][] {
                {322, 217},
                {268, 276},
                {185, 232},
                {225, 360}
        };

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < circleCenters.length; i++) {
            ShortProcessor processor = new ShortProcessor(image.getWidth(), image.getHeight());

            for (double radius = 2; radius < 150; radius += 4) {
                Ring ring = new Ring(image, circleCenters[i][0], circleCenters[i][1], radius, true);

                // The last pixel repeats the first one.
                for (int j = 0; j < ring.ringPixels.length - 1; j++) {
                    double thetaInterval = ring.ringPixels[j].thetaMax
                            - ring.ringPixels[j].thetaMin;
                    int x = ring.ringPixels[j].x;
                    int y = ring.ringPixels[j].y;
                    int intensity = (int) Math.round(thetaInterval * 20000 + processor.get(x, y));
                    processor.set(x, y, intensity);
                }
            }

            long endTime = System.currentTimeMillis();
            print("" + (endTime - startTime));
            startTime = endTime;

            ImagePlus edgeImage = new ImagePlus("", processor);
            FileSaver fileSaver = new FileSaver(edgeImage);
            fileSaver.saveAsTiff(testDataDir + "edgeImg" + (i + 1) + ".tif");
        }

        print("yay");
    }

    public static void main(String[] args) {
        test();
    }

    public static void print(String str) {
        System.out.println(str);
    }
}
