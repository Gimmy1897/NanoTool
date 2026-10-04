import ij.IJ;
import ij.ImagePlus;
import ij.gui.Plot;
import ij.gui.PlotWindow;
import ij.measure.ResultsTable;
import ij.plugin.filter.Analyzer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.util.Arrays;

public class NanoToolStatisticsUtility {

    private static final String EQ_DIAMETER_COLUMN = "Eq. Diameter";
    private NanoToolStatisticsUtility() {}

    public static boolean hasEquivalentDiameterResults() {
        ResultsTable results = Analyzer.getResultsTable();
        return results != null
                && results.getCounter() > 0
                && results.getColumnIndex(EQ_DIAMETER_COLUMN) >= 0;
    }

    public static void showStatisticsAndHistogram() {
        ResultsTable results = Analyzer.getResultsTable();
        if (results == null || results.getCounter() == 0) {
            IJ.showMessage("NanoTool", "No measurement results are available.\nRun \"Measure nanoparticles\" first.");
            return;
        }

        int column = results.getColumnIndex(EQ_DIAMETER_COLUMN);
        if (column < 0) {
            IJ.showMessage("NanoTool", "Equivalent diameter results are not available.\nRun \"Measure nanoparticles\" first.");
            return;
        }

        double[] values = results.getColumnAsDoubles(column);
        values = positiveValues(values);
        if (values.length == 0) {
            IJ.showMessage("NanoTool", "No valid equivalent diameter values are available.");
            return;
        }

        double min = values[0];
        double max = values[0];
        double sum = 0.0;
        for (int i = 0; i < values.length; i++) {
            min = Math.min(min, values[i]);
            max = Math.max(max, values[i]);
            sum += values[i];
        }
        double mean = sum / values.length;
        double median = median(values);
        double variance = 0.0;
        for (int i = 0; i < values.length; i++) {
            double difference = values[i] - mean;
            variance += difference * difference;
        }
        double standardDeviation = values.length > 1
                ? Math.sqrt(variance / (values.length - 1))
                : 0.0;

        ij.ImagePlus sourceImage = ij.WindowManager.getCurrentImage();
        NanoToolDashboard.setAnalysisImage(sourceImage);
        String unit = sourceImage == null
                ? "pixels"
                : sourceImage.getCalibration().getUnit();
        showStatisticsPlot(values, unit, min, max, mean, median, standardDeviation);
    }

    private static void showStatisticsPlot(double[] values, String unit, double min,
            double max, double mean, double median, double standardDeviation) {
        int binCount = Math.max(5, Math.min(20, (int) Math.ceil(Math.sqrt(values.length))));
        double histogramMin = min;
        double histogramMax = max;
        if (histogramMax == histogramMin) {
            histogramMin -= 0.5;
            histogramMax += 0.5;
        }

        double binWidth = (histogramMax - histogramMin) / binCount;
        double[] counts = new double[binCount];
        for (int i = 0; i < values.length; i++) {
            int bin = (int) ((values[i] - histogramMin) / binWidth);
            if (bin >= binCount) {
                bin = binCount - 1;
            }
            counts[bin]++;
        }

        LogNormalFit fit = estimateLogNormal(values);
        double[] curveX = new double[100];
        double[] curveY = new double[100];
        double peak = 0.0;
        for (int i = 0; i < curveX.length; i++) {
            curveX[i] = histogramMin + (histogramMax - histogramMin) * i
                    / (curveX.length - 1);
            curveY[i] = logNormalDensity(curveX[i], fit.mu, fit.sigma)
                    * values.length * binWidth;
            peak = Math.max(peak, curveY[i]);
        }
        fit.rSquared = calculateRSquared(counts, histogramMin, binWidth, values.length, fit);
        double yMax = Math.max(1.0, Math.max(peak, max(counts)) * 1.15);

        String title = "Equivalent Diameter Histogram";
        Plot plot = new Plot(title, "Equivalent diameter (" + unit + ")",
                "Number of particles");
        plot.setSize(760, 500);
        plot.setLimits(histogramMin, histogramMax, 0, yMax);
        plot.setColor(new Color(70, 130, 180));
        drawHistogramBars(plot, counts, histogramMin, binWidth);
        plot.setColor(Color.RED);
        plot.setLineWidth(2);
        plot.addPoints(curveX, curveY, Plot.LINE);
        PlotWindow plotWindow = plot.show();
        Panel statisticsPanel = createStatisticsPanel(values.length, unit, min, max, mean,
                median, standardDeviation, fit.rSquared);
        plotWindow.setVisible(false);
        plotWindow.setLayout(new BorderLayout(8, 8));
        plotWindow.add(statisticsPanel, BorderLayout.EAST);
        plotWindow.setSize(new Dimension(1050, 560));
        plotWindow.validate();
        plotWindow.setVisible(true);
        plotWindow.toFront();
    }

    private static Panel createStatisticsPanel(int particleCount, String unit, double min,
            double max, double mean, double median, double standardDeviation, double rSquared) {
        Panel panel = new Panel(new GridLayout(0, 1, 0, 4));
        panel.setBackground(new Color(238, 238, 238));
        panel.setPreferredSize(new Dimension(250, 240));
        addStatisticLabel(panel, "Equivalent diameter statistics", true);
        addStatisticLabel(panel, "", false);
        addStatisticLabel(panel, String.format("Particles: %d", particleCount), false);
        addStatisticLabel(panel, String.format("Mean: %.4g %s", mean, unit), false);
        addStatisticLabel(panel, String.format("Median: %.4g %s", median, unit), false);
        addStatisticLabel(panel, String.format("Minimum: %.4g %s", min, unit), false);
        addStatisticLabel(panel, String.format("Maximum: %.4g %s", max, unit), false);
        addStatisticLabel(panel, String.format("Std. deviation: %.4g %s",
                standardDeviation, unit), false);
        addStatisticLabel(panel, "", false);
        addStatisticLabel(panel, "Lognormal fit", true);
        addStatisticLabel(panel, String.format("R2: %.3f", rSquared), false);
        return panel;
    }

    private static void addStatisticLabel(Panel panel, String text, boolean bold) {
        Label label = new Label(text, Label.LEFT);
        label.setFont(new Font("Dialog", bold ? Font.BOLD : Font.PLAIN, 12));
        panel.add(label);
    }

    private static void drawHistogramBars(Plot plot, double[] counts, double min,
            double binWidth) {
        for (int i = 0; i < counts.length; i++) {
            double x1 = min + i * binWidth;
            double x2 = min + (i + 1) * binWidth;
            double barTop = counts[i];
            plot.drawLine(x1, barTop, x2, barTop);
            plot.setColor(new Color(30, 70, 110));
            plot.drawLine(x1, 0, x1, barTop);
            plot.drawLine(x2, 0, x2, barTop);
            plot.setColor(new Color(70, 130, 180));
        }
    }

    private static double max(double[] values) {
        double result = 0.0;
        for (double value : values) {
            result = Math.max(result, value);
        }
        return result;
    }

    private static LogNormalFit estimateLogNormal(double[] values) {
        double sum = 0.0;
        for (double value : values) {
            sum += Math.log(value);
        }
        double mu = sum / values.length;
        double variance = 0.0;
        for (double value : values) {
            double difference = Math.log(value) - mu;
            variance += difference * difference;
        }
        double sigma = values.length > 1
                ? Math.sqrt(variance / (values.length - 1))
                : 1e-9;
        return new LogNormalFit(mu, Math.max(sigma, 1e-9));
    }

    private static double logNormalDensity(double x, double mu, double sigma) {
        if (x <= 0) {
            return 0.0;
        }
        double z = (Math.log(x) - mu) / sigma;
        return Math.exp(-0.5 * z * z) / (x * sigma * Math.sqrt(2.0 * Math.PI));
    }

    private static class LogNormalFit {
        final double mu;
        final double sigma;
        double rSquared;

        LogNormalFit(double mu, double sigma) {
            this.mu = mu;
            this.sigma = sigma;
            this.rSquared = 0.0;
        }
    }

    private static double calculateRSquared(double[] counts, double min, double binWidth,
            int sampleCount, LogNormalFit fit) {
        double mean = 0.0;
        for (double count : counts) {
            mean += count;
        }
        mean /= counts.length;
        double total = 0.0;
        double residual = 0.0;
        for (int i = 0; i < counts.length; i++) {
            double center = min + (i + 0.5) * binWidth;
            double expected = logNormalDensity(center, fit.mu, fit.sigma)
                    * sampleCount * binWidth;
            double difference = counts[i] - expected;
            residual += difference * difference;
            double meanDifference = counts[i] - mean;
            total += meanDifference * meanDifference;
        }
        return total == 0.0 ? 1.0 : 1.0 - residual / total;
    }

    private static double[] positiveValues(double[] values) {
        double[] valid = new double[values.length];
        int count = 0;
        for (int i = 0; i < values.length; i++) {
            if (!Double.isNaN(values[i]) && !Double.isInfinite(values[i]) && values[i] > 0) {
                valid[count++] = values[i];
            }
        }
        return Arrays.copyOf(valid, count);
    }

    private static double median(double[] values) {
        double[] sorted = values.clone();
        Arrays.sort(sorted);
        int middle = sorted.length / 2;
        if (sorted.length % 2 == 0) {
            return (sorted[middle - 1] + sorted[middle]) / 2.0;
        }
        return sorted[middle];
    }
}
