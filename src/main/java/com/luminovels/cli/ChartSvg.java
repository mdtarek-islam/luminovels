package com.luminovels.cli;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Renders a grouped bar chart of top-1 accuracy, macro F1 and micro F1 per setup as plain SVG text.
 */
final class ChartSvg {

    private static final String[] METRICS = {"top1_accuracy", "macro_f1", "micro_f1"};
    private static final String[] LABELS = {"Top-1 accuracy", "Macro F1", "Micro F1"};
    private static final String[] COLOURS = {"#2b6cb0", "#dd6b20", "#2f855a", "#805ad5"};
    private static final int WIDTH = 640;
    private static final int HEIGHT = 360;
    private static final int LEFT = 60;
    private static final int BOTTOM = 300;
    private static final int PLOT_HEIGHT = 220;

    private ChartSvg() {
    }

    /** Draws one bar group per metric and one bar per run found under {@code runs}. */
    static String render(JsonNode runs) {
        StringBuilder svg = new StringBuilder();
        svg.append(String.format(Locale.ROOT,
                "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"%d\" height=\"%d\" viewBox=\"0 0 %d %d\" "
                        + "font-family=\"sans-serif\" font-size=\"12\">%n", WIDTH, HEIGHT, WIDTH, HEIGHT));
        svg.append("<rect width=\"100%\" height=\"100%\" fill=\"#ffffff\"/>\n");
        svg.append("<text x=\"").append(LEFT).append("\" y=\"28\" font-size=\"16\" font-weight=\"bold\">")
                .append("LumiNovels classifier performance (held-out split)</text>\n");
        axis(svg);
        List<String> names = new ArrayList<>();
        runs.fieldNames().forEachRemaining(names::add);
        int groupWidth = (WIDTH - LEFT - 20) / METRICS.length;
        int barWidth = Math.max(10, (groupWidth - 30) / Math.max(1, names.size()));
        for (int m = 0; m < METRICS.length; m++) {
            int groupX = LEFT + m * groupWidth + 15;
            for (int r = 0; r < names.size(); r++) {
                double value = runs.get(names.get(r)).path("classification").path(METRICS[m]).asDouble(0.0);
                bar(svg, groupX + r * barWidth, barWidth, value, COLOURS[r % COLOURS.length]);
            }
            svg.append(String.format(Locale.ROOT, "<text x=\"%d\" y=\"%d\" text-anchor=\"middle\">%s</text>%n",
                    groupX + groupWidth / 2 - 15, BOTTOM + 20, LABELS[m]));
        }
        for (int r = 0; r < names.size(); r++) {
            int x = LEFT + r * 160;
            svg.append(String.format(Locale.ROOT,
                    "<rect x=\"%d\" y=\"330\" width=\"12\" height=\"12\" fill=\"%s\"/>%n", x, COLOURS[r % COLOURS.length]));
            svg.append(String.format(Locale.ROOT, "<text x=\"%d\" y=\"341\">%s output</text>%n", x + 18, names.get(r)));
        }
        svg.append("</svg>\n");
        return svg.toString();
    }

    private static void axis(StringBuilder svg) {
        for (int tick = 0; tick <= 4; tick++) {
            int y = BOTTOM - PLOT_HEIGHT * tick / 4;
            svg.append(String.format(Locale.ROOT,
                    "<line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" stroke=\"#e2e8f0\"/>%n", LEFT, y, WIDTH - 20, y));
            svg.append(String.format(Locale.ROOT, "<text x=\"%d\" y=\"%d\" text-anchor=\"end\">%.2f</text>%n",
                    LEFT - 6, y + 4, tick / 4.0));
        }
    }

    private static void bar(StringBuilder svg, int x, int barWidth, double value, String colour) {
        int barHeight = (int) Math.round(Math.max(0.0, Math.min(1.0, value)) * PLOT_HEIGHT);
        svg.append(String.format(Locale.ROOT, "<rect x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\" fill=\"%s\"/>%n",
                x, BOTTOM - barHeight, barWidth - 4, barHeight, colour));
        svg.append(String.format(Locale.ROOT, "<text x=\"%d\" y=\"%d\" text-anchor=\"middle\">%.3f</text>%n",
                x + (barWidth - 4) / 2, BOTTOM - barHeight - 4, value));
    }
}
