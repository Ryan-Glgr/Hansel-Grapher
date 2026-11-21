package io.github.ryan_glgr.hansel_grapher.visualizations.gui;

import com.jogamp.opengl.util.awt.TextRenderer;

import java.awt.Font;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;

/**
 * Draws node labels that scale with zoom so they always fill the same fraction of a node.
 * Create it on the GL thread (e.g. in init) and use begin/drawLabel/end from display.
 */
public class LabelPainter {

    private static final int FONT_SIZE = 18;
    private static final float TEXT_PADDING_INSIDE_NODE = 1.5f;

    // Labels are only drawn once the text would fit at its native size (scale >= MIN_TEXT_SCALE).
    private static final float MIN_TEXT_SCALE = 1.0f;
    private static final float TEXT_FILL_FRACTION = 0.9f;

    private final TextRenderer textRenderer;

    // Per-frame state, set by begin().
    private float viewLeft;
    private float viewBottom;
    private float viewWidth;
    private float viewHeight;
    private int surfaceWidth;
    private int surfaceHeight;
    private float textScale;
    private double lineHeight;
    private double totalHeight;

    public LabelPainter() {
        textRenderer = new TextRenderer(new Font("SansSerif", Font.PLAIN, FONT_SIZE));
    }

    /**
     * Starts a batch of labels. Returns false (and draws nothing) when the labels would be
     * too small to be worth drawing; in that case do not call drawLabel or end.
     *
     * @param sampleLabel any one label, used to measure how large the text is
     */
    public boolean begin(final int surfaceWidth, final int surfaceHeight,
                         final float left, final float right, final float bottom, final float top,
                         final float nodeWidth, final float nodeHeight, final String[] sampleLabel) {

        this.surfaceWidth = surfaceWidth;
        this.surfaceHeight = surfaceHeight;
        this.viewLeft = left;
        this.viewBottom = bottom;
        this.viewWidth = right - left;
        this.viewHeight = top - bottom;

        final float nodeWidthPx = nodeWidth * (surfaceWidth / viewWidth);
        final float nodeHeightPx = nodeHeight * (surfaceHeight / viewHeight);

        // Text metrics at the font's native size.
        final double nativeLineHeight = textRenderer.getBounds("Ag").getHeight();
        final double nativeTotalHeight = nativeLineHeight * sampleLabel.length;

        final double nativeLongestLine = Arrays.stream(sampleLabel)
                .mapToDouble(line -> textRenderer.getBounds(line).getWidth())
                .max().orElseThrow();

        // The largest scale at which the label still fits inside the node.
        final float fitScale = (float) Math.min(
                (nodeWidthPx - TEXT_PADDING_INSIDE_NODE) / nativeLongestLine,
                (nodeHeightPx - TEXT_PADDING_INSIDE_NODE) / nativeTotalHeight);

        if (fitScale < MIN_TEXT_SCALE) return false;

        textScale = fitScale * TEXT_FILL_FRACTION;
        lineHeight = nativeLineHeight * textScale;
        totalHeight = nativeTotalHeight * textScale;

        textRenderer.beginRendering(surfaceWidth, surfaceHeight);
        textRenderer.setColor(0f, 0f, 0f, 1f);
        return true;
    }

    /** Draws a multi-line label centered on the node at world position (worldX, worldY). */
    public void drawLabel(final String[] lines, final float worldX, final float worldY) {
        final float screenX = (worldX - viewLeft) / viewWidth * surfaceWidth;
        final float screenY = (worldY - viewBottom) / viewHeight * surfaceHeight;

        final float startY = (float) (screenY + totalHeight / 2 - lineHeight);

        for (int i = 0; i < lines.length; i++) {
            // Unscaled bounds for the whole line.
            final Rectangle2D bounds = textRenderer.getBounds(lines[i]);

            // Distance from the baseline origin to the right-most pixel.
            final double baselineToRight = bounds.getX() + bounds.getWidth();

            final double scaledLineW = bounds.getWidth() * textScale;
            final double scaledRightEdge = baselineToRight * textScale;

            // Centering offset.
            final double offset = (scaledLineW - scaledRightEdge) / 2.0;

            final float drawX = (float) (screenX - scaledLineW / 2.0 + offset);
            final float drawY = (float) (startY - i * lineHeight);

            textRenderer.draw3D(lines[i], drawX, drawY, 0f, textScale);
        }
    }

    public void end() {
        textRenderer.endRendering();
    }

    public void dispose() {
        textRenderer.dispose();
    }
}