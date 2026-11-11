package io.github.ryan_glgr.hansel_grapher.visualizations;

import io.github.ryan_glgr.hansel_grapher.functionallogic.Node;
import io.github.ryan_glgr.hansel_grapher.functionallogic.lowunits.LowUnit;
import io.github.ryan_glgr.hansel_grapher.visualizations.gui.GUIHelper;
import io.github.ryan_glgr.hansel_grapher.visualizations.layout.HanselChainLayout;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.util.Matrix;

import java.io.InputStream;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.github.ryan_glgr.hansel_grapher.visualizations.layout.HanselChainLayout.NODE_HEIGHT;
import static io.github.ryan_glgr.hansel_grapher.visualizations.layout.HanselChainLayout.NODE_WIDTH;

/**
 * Writes Hansel-chain diagrams directly as PDF using HanselChainLayout's
 * precomputed positions.
 * <p>
 * No SVG or intermediate representation is created. The visualization is
 * translated directly into PDF drawing operations.
 * <p>
 * Rendering strategy: rather than emitting a {@code setNonStrokingColor} +
 * {@code addRect} + {@code fill} sequence per rectangle (which produces one
 * fill operator per node and dominates the content stream when there are
 * hundreds of thousands of nodes), geometry is first collected and grouped
 * by color, then each color group is emitted as a single {@code fill}
 * covering all of its rectangles. Text labels are likewise collected and
 * written inside a single {@code BT}/{@code ET} block instead of one pair
 * per label. Both changes reduce the operator count from roughly
 * {@code O(numNodes)} paint operations to {@code O(numClasses)}, which is
 * what a PDF viewer actually has to execute.
 */
public class VisualizationPDF {

    private static final String OUTPUT_DIRECTORY = "out";
    private static final String HANSEL_CHAINS_FILE_NAME = "HanselChains.pdf";

    /*
     * This corresponds to the old SVG's pixels-per-unit value.
     *
     * PDF coordinates are points, rather than pixels, but there is no requirement
     * that our world units have any particular physical meaning. Keeping the
     * same scale preserves the existing exported geometry.
     */
    private static final float POINTS_PER_UNIT = 40f;

    private static final float LABEL_FONT_SIZE = 12f;
    private static final float LABEL_LINE_HEIGHT = 14f;

    /*
     * For the first implementation, use one of PDF's built-in fonts.
     *
     * If you need the exact DejaVu Sans appearance from the SVG, this can
     * later be changed to a loaded/embedded TTF font.
     */
    private static PDFont LABEL_FONT;

    // Mirrors HanselChainRenderer.BORDER_THICKNESS_FRACTION.
    private static final float BORDER_THICKNESS_FRACTION = 0.15f;

    /**
     * Rectangle geometry queued for a batched fill. Kept as a small record-like
     * holder rather than a raw float[] purely for readability at the call site.
     */
    private record RectGeometry(float x, float y, float width, float height) {}

    /**
     * A single text label queued to be written inside the shared BT/ET block.
     */
    private record LabelDraw(String text, float x, float baselineY) {}

    public static void makeHanselChainPDF(
            final ArrayList<ArrayList<Node>> chains,
            final Map<Integer, Set<LowUnit>> lowUnitsByClass,
            final int numClasses,
            final int classificationColorShuffleCounter) throws IOException {

        final HanselChainLayout layout =
                new HanselChainLayout(chains, lowUnitsByClass);

        new File(OUTPUT_DIRECTORY).mkdirs();

        final String outPath =
                OUTPUT_DIRECTORY + File.separator + HANSEL_CHAINS_FILE_NAME;

        final float[] bounds = layout.getWorldBounds();

        final float pdfW =
                (bounds[1] - bounds[0]) * POINTS_PER_UNIT;

        final float pdfH =
                (bounds[3] - bounds[2]) * POINTS_PER_UNIT;

        /*
         * PDF pages have a coordinate system with the origin at the
         * bottom-left, which is actually convenient because our world
         * coordinates also grow upward.
         */
        try (final PDDocument document = new PDDocument()) {

            final PDPage page =
                    new PDPage(new PDRectangle(pdfW, pdfH));

            document.addPage(page);

            LABEL_FONT = loadLabelFont(document);

            /*
             * Rectangles are grouped by fill color so that every rectangle
             * sharing a color can be emitted under a single fill() call.
             * LinkedHashMap keeps a stable, deterministic emission order,
             * which is convenient for diffing output between runs.
             */
            final Map<Color, List<RectGeometry>> rectsByColor = new LinkedHashMap<>();

            /*
             * Labels are collected here and written in one BT/ET block at
             * the end, instead of one BT/ET pair per label.
             */
            final List<LabelDraw> labels = new ArrayList<>();

            final Node[][] nodeGrid = layout.getNodeGrid();
            final String[][][] labelGrid = layout.getLabelGrid();

            /*
             * Walk the grid and collect geometry/text only; nothing is
             * written to the content stream in this pass.
             */
            for (int c = 0; c < nodeGrid.length; c++) {
                for (int row = 0; row < nodeGrid[c].length; row++) {

                    final Node node = nodeGrid[c][row];

                    if (node == null) {
                        continue;
                    }

                    collectNode(
                            node,
                            layout.getLowUnitType(node),
                            labelGrid[c][row],
                            layout.getX(c),
                            layout.getY(row),
                            bounds,
                            numClasses,
                            classificationColorShuffleCounter,
                            rectsByColor,
                            labels
                    );
                }
            }

            try (final PDPageContentStream content =
                         new PDPageContentStream(document, page)) {

                // White page background.
                content.setNonStrokingColor(Color.WHITE);
                content.addRect(0, 0, pdfW, pdfH);
                content.fill();

                /*
                 * One setNonStrokingColor + one fill per distinct color,
                 * regardless of how many rectangles share that color.
                 */
                for (final Map.Entry<Color, List<RectGeometry>> entry : rectsByColor.entrySet()) {

                    content.setNonStrokingColor(entry.getKey());

                    for (final RectGeometry rect : entry.getValue()) {
                        content.addRect(rect.x, rect.y, rect.width, rect.height);
                    }

                    content.fill();
                }

                /*
                 * All labels share one BT/ET block. setTextMatrix positions
                 * each label absolutely, so no running offset needs to be
                 * tracked between labels.
                 */
                if (!labels.isEmpty()) {

                    content.beginText();
                    content.setFont(LABEL_FONT, LABEL_FONT_SIZE);
                    content.setNonStrokingColor(Color.BLACK);

                    for (final LabelDraw label : labels) {
                        content.setTextMatrix(Matrix.getTranslateInstance(label.x, label.baselineY));
                        content.showText(label.text);
                    }

                    content.endText();
                }
            }

            Files.deleteIfExists(Path.of(outPath));

            document.save(outPath);
        }
    }

    /**
     * Computes geometry and text for a single node and appends it to the
     * shared collections, instead of writing directly to a content stream.
     */
    private static void collectNode(
            final Node node,
            final LowUnit.Type lowUnitType,
            final String[] labelLines,
            final float worldX,
            final float worldY,
            final float[] bounds,
            final int numClasses,
            final int classificationColorShuffleCounter,
            final Map<Color, List<RectGeometry>> rectsByColor,
            final List<LabelDraw> labels) throws IOException {

        final float cx = toPdfX(worldX, bounds);
        final float cy = toPdfY(worldY, bounds);

        final float w = NODE_WIDTH * POINTS_PER_UNIT;
        final float h = NODE_HEIGHT * POINTS_PER_UNIT;

        final float x = cx - w / 2f;
        final float y = cy - h / 2f;

        final int shuffledClass =
                node.classification.equals(Node.IMPOSSIBLE_CLASSIFICATION)
                        ? Node.IMPOSSIBLE_CLASSIFICATION
                        : (node.classification + classificationColorShuffleCounter)
                        % numClasses;

        final boolean isExclusive =
                LowUnit.Type.EXCLUSIVE.equals(lowUnitType);

        if (isExclusive) {

            /*
             * Queue the full-size border in the target class color.
             */
            final int borderClass =
                    node.classification + 1 == numClasses
                            ? Node.IMPOSSIBLE_CLASSIFICATION
                            : (node.classification + classificationColorShuffleCounter
                            + 1) % numClasses;

            queueRectangle(
                    rectsByColor,
                    x,
                    y,
                    w,
                    h,
                    GUIHelper.getColorForClass(borderClass, true)
            );

            /*
             * Queue the inset interior.
             */
            final float inset =
                    h * BORDER_THICKNESS_FRACTION;

            queueRectangle(
                    rectsByColor,
                    x,
                    y + inset,
                    w,
                    h - 2 * inset,
                    GUIHelper.getColorForClass(shuffledClass, false)
            );

        } else {

            final boolean colorAsLowUnit =
                    lowUnitType != null;

            queueRectangle(
                    rectsByColor,
                    x,
                    y,
                    w,
                    h,
                    GUIHelper.getColorForClass(shuffledClass, colorAsLowUnit)
            );
        }

        /*
         * PDF text positioning is based on the text baseline rather than
         * SVG's dominant-baseline="middle".
         *
         * This first version approximates the old SVG vertical positioning.
         */
        final float textStartY =
                cy - (labelLines.length - 1)
                        * LABEL_LINE_HEIGHT / 2f;

        for (int i = 0; i < labelLines.length; i++) {

            final String label = labelLines[i];

            final float textY =
                    textStartY
                            + i * LABEL_LINE_HEIGHT
                            - LABEL_FONT_SIZE * 0.35f;

            queueCenteredText(
                    labels,
                    label,
                    cx,
                    textY
            );
        }
    }

    private static void queueRectangle(
            final Map<Color, List<RectGeometry>> rectsByColor,
            final float x,
            final float y,
            final float width,
            final float height,
            final Color color) {

        rectsByColor
                .computeIfAbsent(color, c -> new ArrayList<>())
                .add(new RectGeometry(x, y, width, height));
    }

    private static void queueCenteredText(
            final List<LabelDraw> labels,
            final String text,
            final float centerX,
            final float baselineY) throws IOException {

        /*
         * PDFBox does not provide SVG's text-anchor="middle", so calculate
         * the text width and offset manually.
         */
        final float textWidth =
                LABEL_FONT.getStringWidth(text)
                        / 1000f
                        * LABEL_FONT_SIZE;

        final float x =
                centerX - textWidth / 2f;

        labels.add(new LabelDraw(text, x, baselineY));
    }

    private static float toPdfX(
            final float worldX,
            final float[] bounds) {

        return (worldX - bounds[0])
                * POINTS_PER_UNIT;
    }

    private static float toPdfY(
            final float worldY,
            final float[] bounds) {

        /*
         * Unlike SVG, PDF's Y axis grows upward.
         *
         * Therefore we do NOT need the old:
         *
         *     svgH - (...)
         *
         * conversion.
         */
        return (worldY - bounds[2])
                * POINTS_PER_UNIT;
    }

    private static PDFont loadLabelFont(final PDDocument document) throws IOException {
        try (final InputStream fontStream =
                     VisualizationPDF.class.getResourceAsStream("/fonts/DejaVuSans.ttf")) {

            if (fontStream == null) {
                throw new IOException(
                        "Font resource /fonts/DejaVuSans.ttf not found on classpath");
            }

            return PDType0Font.load(document, fontStream);
        }
    }

}