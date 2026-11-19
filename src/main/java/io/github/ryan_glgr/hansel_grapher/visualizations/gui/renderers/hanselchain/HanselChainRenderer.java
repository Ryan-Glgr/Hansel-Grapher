package io.github.ryan_glgr.hansel_grapher.visualizations.gui.renderers.hanselchain;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.util.awt.TextRenderer;
import io.github.ryan_glgr.hansel_grapher.functionallogic.Interview.Interview;
import io.github.ryan_glgr.hansel_grapher.functionallogic.Interview.LiveInterviewVisualizer;
import io.github.ryan_glgr.hansel_grapher.functionallogic.Node;
import io.github.ryan_glgr.hansel_grapher.functionallogic.lowunits.LowUnit;
import io.github.ryan_glgr.hansel_grapher.visualizations.gui.GLHelper;
import io.github.ryan_glgr.hansel_grapher.visualizations.gui.GUIHelper;
import io.github.ryan_glgr.hansel_grapher.visualizations.gui.renderers.PanZoomRenderer;
import io.github.ryan_glgr.hansel_grapher.visualizations.gui.screens.MainWindow;
import io.github.ryan_glgr.hansel_grapher.visualizations.gui.screens.NodeExplorer;
import io.github.ryan_glgr.hansel_grapher.visualizations.layout.HanselChainLayout;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.nio.FloatBuffer;
import java.util.*;

import static io.github.ryan_glgr.hansel_grapher.visualizations.gui.GLHelper.SHADER_LOG_BUFFER_SIZE;
import static io.github.ryan_glgr.hansel_grapher.visualizations.gui.screens.MainWindow.EXPANSION_VIEW;
import static io.github.ryan_glgr.hansel_grapher.visualizations.gui.screens.MainWindow.HANSEL_CHAIN_VIEW;
import static io.github.ryan_glgr.hansel_grapher.visualizations.layout.HanselChainLayout.*;

public class HanselChainRenderer extends PanZoomRenderer implements LiveInterviewVisualizer {

    // --- Layout constants pulled from HanselChainLayout via static import:
    // NODE_WIDTH, NODE_HEIGHT, SIDE_SPACING, VERTICAL_SPACING, MARGIN, ROW_STEP, COL_STEP

    private static final int FONT_SIZE = 18;
    private static final float TEXT_PADDING_INSIDE_NODE = 1.5f;

    // Text scales with zoom so it always occupies the same fraction of the node.
    // Labels are only drawn once the text would fit at its native size (scale >= MIN_TEXT_SCALE).
    private static final float MIN_TEXT_SCALE = 1.0f;
    private static final float TEXT_FILL_FRACTION = 0.9f;

    private static final int POSITION_COMPONENTS = 2;   // x, y
    private static final int COLOR_COMPONENTS = 4;      // r, g, b, a
    private static final int VERTICES_PER_QUAD = 6;
    private static final int VERTICES_PER_TRIANGLE = 3;

    private static final int VBO_POSITIONS = 0;         // vboIds[0] -> positions
    private static final int VBO_COLORS = 1;            // vboIds[1] -> colors

    // Four quads form the hover border.
    private static final int HOVER_BORDER_QUADS = 4;
    private static final int HOVER_BORDER_VERTICES = HOVER_BORDER_QUADS * VERTICES_PER_QUAD;

    private static final float HOVER_BORDER_THICKNESS = 0.2f;
    private static final Color HOVER_BORDER_COLOR = new Color(220, 70, 220);

    private int vaoId;
    private int shaderProgram;
    private final int[] vboIds = new int[2];
    private int projectionUniformLocation = -1;
    private volatile boolean colorsDirty;
    private TextRenderer textRenderer;

    private final int numClasses;
    private final HanselChainLayout layout;

    // Desired hover state, potentially updated by the AWT event thread.
    private volatile NodeHit hoveredHit;

    // Last hover state uploaded to the GPU. Accessed only by the GL thread.
    private NodeHit renderedHoveredHit;

    private final MainWindow parentWindow;
    private final Interview interview;

    public HanselChainRenderer(final Interview interview, final int classificationColorShuffleCounter, final MainWindow parentWindow) {

        super(classificationColorShuffleCounter);

        this.numClasses = interview.numClasses;
        this.interview = interview;
        this.layout = new HanselChainLayout(interview.hanselChains, interview.lowUnitsByClass, interview.attributeNames);
        this.parentWindow = parentWindow;
    }

    // Called from the compute thread whenever node classifications change.
    @Override
    public void notifyClassificationsChanged() {
        colorsDirty = true;
    }

    @Override
    protected float[] getWorldBounds() {
        return layout.getWorldBounds();
    }

    // -----------------------------------------------------------------------
    // Buffer builders
    // -----------------------------------------------------------------------

    private FloatBuffer buildPositionBuffer() {
        final Node[][] nodeGrid = layout.getNodeGrid();

        final int regularSize = layout.getTotalNodes() * VERTICES_PER_QUAD * POSITION_COMPONENTS;

        final int extraSizeForExclusiveLowUnits = layout.getNumExclusiveLowUnits() * VERTICES_PER_TRIANGLE * POSITION_COMPONENTS;

        final int hoverBorderSize = HOVER_BORDER_VERTICES * POSITION_COMPONENTS;

        final FloatBuffer buffer = Buffers.newDirectFloatBuffer(regularSize + extraSizeForExclusiveLowUnits + hoverBorderSize);

        for (int c = 0; c < nodeGrid.length; c++) {
            final float cx = layout.getX(c);
            final float l = cx - NODE_WIDTH / 2f;
            final float r = cx + NODE_WIDTH / 2f;

            for (int row = 0; row < layout.getLongestChainHeight(); row++) {
                final Node node = nodeGrid[c][row];
                if (node == null) continue;

                final float cy = layout.getY(row);
                final float b = cy - NODE_HEIGHT / 2f;
                final float t = cy + NODE_HEIGHT / 2f;

                if (LowUnit.Type.EXCLUSIVE.equals(layout.getLowUnitType(node))) {
                    GLHelper.emitQuad(buffer, l, b, r, t);

                    GLHelper.emitTriangle(buffer, l, t, r, t, (l + r) / 2f, t - 0.5f * NODE_HEIGHT);
                } else {
                    GLHelper.emitQuad(buffer, l, b, r, t);
                }
            }
        }

        // Reserve space for the hover border. Its positions are updated dynamically.
        for (int i = 0; i < hoverBorderSize; i++) {
            buffer.put(0f);
        }

        buffer.flip();
        return buffer;
    }

    // [r, g, b, a] per node — rebuilt whenever classifications change.
    private FloatBuffer buildColorBuffer() {
        final Node[][] nodeGrid = layout.getNodeGrid();

        final int extraBufferSizeForExclusiveLowUnits = layout.getNumExclusiveLowUnits() * VERTICES_PER_TRIANGLE * COLOR_COMPONENTS;

        final int regularBufferSize = layout.getTotalNodes() * VERTICES_PER_QUAD * COLOR_COMPONENTS;

        final int hoverBorderSize = HOVER_BORDER_VERTICES * COLOR_COMPONENTS;

        final FloatBuffer buffer = Buffers.newDirectFloatBuffer(regularBufferSize + extraBufferSizeForExclusiveLowUnits + hoverBorderSize);

        for (final Node[] chain : nodeGrid) {
            for (final Node node : chain) {
                if (node == null) continue;

                final LowUnit.Type lowUnitType = layout.getLowUnitType(node);

                final int nodeClassWithColorShuffle = node.classification.equals(Node.IMPOSSIBLE_CLASSIFICATION) ? Node.IMPOSSIBLE_CLASSIFICATION : (node.classification + classificationColorShuffleCounter) % numClasses;

                final boolean isExclusiveLowUnit = LowUnit.Type.EXCLUSIVE.equals(lowUnitType);

                final boolean colorAsALowUnit = Objects.nonNull(lowUnitType) && !isExclusiveLowUnit;

                // Color the regular rectangle.
                populateColorBuffer(nodeClassWithColorShuffle, colorAsALowUnit, VERTICES_PER_QUAD, buffer);

                // Add the triangle to the top for an exclusive low unit.
                if (isExclusiveLowUnit) {
                    final int exclusiveNodeTargetClass = node.classification + 1 == numClasses ? Node.IMPOSSIBLE_CLASSIFICATION : (node.classification + classificationColorShuffleCounter + 1) % numClasses;

                    populateColorBuffer(exclusiveNodeTargetClass, true, VERTICES_PER_TRIANGLE, buffer);
                }
            }
        }

        // Reserve magenta color vertices for the hover border.
        final float r = HOVER_BORDER_COLOR.getRed() / 255f;
        final float g = HOVER_BORDER_COLOR.getGreen() / 255f;
        final float b = HOVER_BORDER_COLOR.getBlue() / 255f;

        for (int i = 0; i < HOVER_BORDER_VERTICES; i++) {
            buffer.put(r);
            buffer.put(g);
            buffer.put(b);
            buffer.put(1f);
        }

        buffer.flip();
        return buffer;
    }

    private void populateColorBuffer(final int classification, final boolean isLowUnit, final int vertexCount, final FloatBuffer buffer) {

        final Color color = GUIHelper.getColorForClass(classification, isLowUnit);

        final float rC = color.getRed() / 255f;
        final float gC = color.getGreen() / 255f;
        final float bC = color.getBlue() / 255f;
        final float aC = color.getAlpha() / 255f;

        for (int i = 0; i < vertexCount; i++) {
            buffer.put(rC);
            buffer.put(gC);
            buffer.put(bC);
            buffer.put(aC);
        }
    }

    // -----------------------------------------------------------------------
    // Hover border
    // -----------------------------------------------------------------------

    private void updateHoverBorderVBO(final GL3 gl, final NodeHit hit) {
        final FloatBuffer buffer = Buffers.newDirectFloatBuffer(HOVER_BORDER_VERTICES * POSITION_COMPONENTS);

        final float cx = layout.getX(hit.column());
        final float cy = layout.getY(hit.row());

        final float left = cx - NODE_WIDTH / 2f;
        final float right = cx + NODE_WIDTH / 2f;
        final float bottom = cy - NODE_HEIGHT / 2f;
        final float top = cy + NODE_HEIGHT / 2f;

        final float thickness = Math.min(NODE_WIDTH, NODE_HEIGHT) * HOVER_BORDER_THICKNESS;

        // Top
        GLHelper.emitQuad(buffer, left, top - thickness, right, top);

        // Bottom
        GLHelper.emitQuad(buffer, left, bottom, right, bottom + thickness);

        // Left
        GLHelper.emitQuad(buffer, left, bottom + thickness, left + thickness, top - thickness);

        // Right
        GLHelper.emitQuad(buffer, right - thickness, bottom + thickness, right, top - thickness);

        buffer.flip();

        final int nodeVertexCount = layout.getTotalNodes() * VERTICES_PER_QUAD + layout.getNumExclusiveLowUnits() * VERTICES_PER_TRIANGLE;

        final long offset = (long) nodeVertexCount * POSITION_COMPONENTS * Float.BYTES;

        gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, vboIds[VBO_POSITIONS]);

        gl.glBufferSubData(GL3.GL_ARRAY_BUFFER, offset, (long) buffer.remaining() * Float.BYTES, buffer);

        gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, 0);
    }

    private static boolean sameHit(final NodeHit a, final NodeHit b) {
        if (a == null || b == null) return false;
        return a.node() == b.node();
    }

    // -----------------------------------------------------------------------
    // GL event listener
    // -----------------------------------------------------------------------
    @Override
    public void init(final GLAutoDrawable drawable) {
        final GL3 gl = GLHelper.getGl3(drawable);

        shaderProgram = GLHelper.createShaderProgram(gl);

        projectionUniformLocation = gl.glGetUniformLocation(shaderProgram, "uProjection");

        if (projectionUniformLocation == -1) {
            throw new RuntimeException("Uniform 'uProjection' not found in shader program.");
        }

        gl.glValidateProgram(shaderProgram);

        final int[] validateStatus = new int[1];
        gl.glGetProgramiv(shaderProgram, GL3.GL_VALIDATE_STATUS, validateStatus, 0);

        if (validateStatus[0] == GL3.GL_FALSE) {
            final byte[] log = new byte[SHADER_LOG_BUFFER_SIZE];
            gl.glGetProgramInfoLog(shaderProgram, SHADER_LOG_BUFFER_SIZE, null, 0, log, 0);
        }

        final int[] vaos = new int[1];
        gl.glGenVertexArrays(1, vaos, 0);

        this.vaoId = vaos[0];

        if (vaoId == 0) {
            throw new RuntimeException("VAO creation failed");
        }

        gl.glBindVertexArray(vaoId);
        gl.glGenBuffers(2, vboIds, 0);

        // Position VBO
        gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, vboIds[VBO_POSITIONS]);

        final FloatBuffer positionBuffer = buildPositionBuffer();

        gl.glBufferData(GL3.GL_ARRAY_BUFFER, (long) positionBuffer.capacity() * Float.BYTES, positionBuffer, GL3.GL_STATIC_DRAW);

        gl.glEnableVertexAttribArray(0);
        gl.glVertexAttribPointer(0, POSITION_COMPONENTS, GL3.GL_FLOAT, false, 0, 0);

        // Color VBO
        gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, vboIds[VBO_COLORS]);

        final FloatBuffer colorBuffer = buildColorBuffer();

        gl.glBufferData(GL3.GL_ARRAY_BUFFER, (long) colorBuffer.capacity() * Float.BYTES, colorBuffer, GL3.GL_DYNAMIC_DRAW);

        gl.glEnableVertexAttribArray(1);
        gl.glVertexAttribPointer(1, COLOR_COMPONENTS, GL3.GL_FLOAT, false, 0, 0);

        gl.glBindVertexArray(0);
        gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, 0);

        this.textRenderer = new TextRenderer(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, FONT_SIZE));

        reshape(drawable, 0, 0, drawable.getSurfaceWidth(), drawable.getSurfaceHeight());

        super.init(drawable); // registers mouse listeners
    }

    private void rebuildColorVBO(final GL3 gl) {
        final FloatBuffer colorBuffer = buildColorBuffer();

        gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, vboIds[VBO_COLORS]);

        gl.glBufferSubData(GL3.GL_ARRAY_BUFFER, 0, (long) colorBuffer.capacity() * Float.BYTES, colorBuffer);

        gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, 0);
    }

    @Override
    public void display(final GLAutoDrawable drawable) {
        final GL3 gl = drawable.getGL().getGL3();

        gl.glClear(GL3.GL_COLOR_BUFFER_BIT);

        if (colorsDirty) {
            rebuildColorVBO(gl);
            colorsDirty = false;
        }

        gl.glUseProgram(shaderProgram);

        if (hasNewProjection()) {
            gl.glUniformMatrix4fv(projectionUniformLocation, 1, false, consumePendingProjection(), 0);
        }

        gl.glBindVertexArray(vaoId);

        final int numberOfVertices = layout.getTotalNodes() * VERTICES_PER_QUAD + layout.getNumExclusiveLowUnits() * VERTICES_PER_TRIANGLE;

        // Draw all node geometry.
        gl.glDrawArrays(GL3.GL_TRIANGLES, 0, numberOfVertices);

        // Update the reserved hover geometry only when the hovered node changes.
        final NodeHit currentHover = hoveredHit;

        if (!sameHit(currentHover, renderedHoveredHit)) {
            if (currentHover != null) {
                updateHoverBorderVBO(gl, currentHover);
            }

            renderedHoveredHit = currentHover;
        }

        // Draw the hover border on top of the nodes.
        if (currentHover != null) {
            gl.glDrawArrays(GL3.GL_TRIANGLES, numberOfVertices, HOVER_BORDER_VERTICES);
        }

        gl.glBindVertexArray(0);
        gl.glUseProgram(0);

        // Labels remain on top of both the nodes and hover border.
        drawLabels();
    }

    // -----------------------------------------------------------------------
    // Labels
    // -----------------------------------------------------------------------

    private void drawLabels() {
        final float viewWidth = getLiveRight() - getLiveLeft();
        final float viewHeight = getLiveTop() - getLiveBottom();

        final float scaleX = surfaceWidth / viewWidth;
        final float scaleY = surfaceHeight / viewHeight;

        final float nodeWidthPx = NODE_WIDTH * scaleX;
        final float nodeHeightPx = NODE_HEIGHT * scaleY;

        final String[][][] labelGrid = layout.getLabelGrid();
        final float[] columnX = layout.getColumnX();
        final int longestChainHeight = layout.getLongestChainHeight();

        final String[] sampleLabel = Arrays.stream(labelGrid).flatMap(Arrays::stream).filter(Objects::nonNull).findFirst().orElseThrow(() -> new IllegalStateException("No node labels exist"));

        // Text metrics at the font's native size.
        final double nativeLineHeight = textRenderer.getBounds("Ag").getHeight();
        final double nativeTotalHeight = nativeLineHeight * sampleLabel.length;

        final double nativeLongestLine = Arrays.stream(sampleLabel).mapToDouble(line -> textRenderer.getBounds(line).getWidth()).max().orElseThrow();

        // The largest scale at which the label still fits inside the node.
        final float fitScale = (float) Math.min((nodeWidthPx - TEXT_PADDING_INSIDE_NODE) / nativeLongestLine, (nodeHeightPx - TEXT_PADDING_INSIDE_NODE) / nativeTotalHeight);

        // Too small to be worth drawing: hide the text entirely.
        if (fitScale < MIN_TEXT_SCALE) return;

        final float textScale = fitScale * TEXT_FILL_FRACTION;

        final double lineHeight = nativeLineHeight * textScale;
        final double totalHeight = nativeTotalHeight * textScale;

        final int colMin = clamp((int) Math.floor((getLiveLeft() - NODE_WIDTH / 2f) / COL_STEP), 0, columnX.length - 1);

        final int colMax = clamp((int) Math.ceil((getLiveRight() + NODE_WIDTH / 2f) / COL_STEP), 0, columnX.length - 1);

        final int rowMin = clamp((int) Math.floor((getLiveBottom() - NODE_HEIGHT / 2f - layout.getBaseRowY()) / ROW_STEP), 0, longestChainHeight - 1);

        final int rowMax = clamp((int) Math.ceil((getLiveTop() + NODE_HEIGHT / 2f - layout.getBaseRowY()) / ROW_STEP), 0, longestChainHeight - 1);

        textRenderer.beginRendering(surfaceWidth, surfaceHeight);
        textRenderer.setColor(0f, 0f, 0f, 1f);

        for (int c = colMin; c <= colMax; c++) {
            final float worldX = columnX[c];
            final float screenX = (worldX - getLiveLeft()) / viewWidth * surfaceWidth;

            for (int r = rowMin; r <= rowMax; r++) {
                final String[] lines = labelGrid[c][r];

                if (lines == null) continue;

                final float worldY = layout.getY(r);
                final float screenY = (worldY - getLiveBottom()) / viewHeight * surfaceHeight;

                final float startY = (float) (screenY + totalHeight / 2 - lineHeight);

                for (int i = 0; i < lines.length; i++) {
                    // Get the unscaled bounds for the whole line.
                    final Rectangle2D bounds = textRenderer.getBounds(lines[i]);

                    // Distance from the baseline to the right-most pixel.
                    final double baselineToRight = bounds.getX() + bounds.getWidth();

                    // Scale everything.
                    final double scaledLineW = bounds.getWidth() * textScale;
                    final double scaledRightEdge = baselineToRight * textScale;

                    // Centering offset.
                    final double offset = (scaledLineW - scaledRightEdge) / 2.0;

                    final float drawX = (float) (screenX - scaledLineW / 2.0 + offset);

                    final float drawY = (float) (startY - i * lineHeight);

                    textRenderer.draw3D(lines[i], drawX, drawY, 0f, textScale);
                }
            }
        }

        textRenderer.endRendering();
    }

    private static int clamp(final int v, final int lo, final int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    // -----------------------------------------------------------------------
    // Disposal
    // -----------------------------------------------------------------------

    @Override
    public void dispose(final GLAutoDrawable drawable) {
        final GL3 gl = drawable.getGL().getGL3();

        gl.glDeleteBuffers(2, vboIds, 0);

        final int[] vaos = {vaoId};
        gl.glDeleteVertexArrays(1, vaos, 0);

        gl.glDeleteProgram(shaderProgram);

        if (textRenderer != null) {
            textRenderer.dispose();
        }
    }

    // -----------------------------------------------------------------------
    // Mouse events
    // -----------------------------------------------------------------------
    @Override
    public void mouseClicked(final MouseEvent e) {
        final NodeHit hit = findNodeAt(e.getX(), e.getY());

        if (hit == null) return;

        System.out.println("Clicked Node: " + hit);
        final NodeExplorer.NodeExplorerResult choice = NodeExplorer.createNodeExplorerWindowAndGetResult();
        System.out.println("User made choice: " + choice);
        switch  (choice) {
            case NodeExplorer.NodeExplorerResult.SEE_EXPANSIONS -> {
                parentWindow.handleVisualizationChange(EXPANSION_VIEW, hit.node());
            }

            // TODO: these two cases need to spawn a little box which asks for the class. then invoke some Node.permeate style method.
            case ADJUST_WITHOUT_EXPANSION -> {
//                hit.node().permeateClassification()
                parentWindow.handleVisualizationChange(HANSEL_CHAIN_VIEW, null);
            } case ADJUST_WITH_EXPANSION -> {
//                hit.node()
                parentWindow.handleVisualizationChange(HANSEL_CHAIN_VIEW, null);
            }
            // do nothing, they just closed the thing.
            case null -> {}
            default -> throw new UnsupportedOperationException("Invalid node explorer choice");
        }
    }

    @Override
    public void mouseExited(final MouseEvent e) {
        hoveredHit = null;
    }

    @Override
    public void mouseMoved(final MouseEvent e) {
        final NodeHit newHit = findNodeAt(e.getX(), e.getY());

        if (sameHit(newHit, hoveredHit)) return;

        hoveredHit = newHit;
    }

    private NodeHit findNodeAt(final int mouseX, final int mouseY) {
        final float worldX = getLiveLeft() + mouseX / (float) surfaceWidth * (getLiveRight() - getLiveLeft());

        final float worldY = getLiveTop() - mouseY / (float) surfaceHeight * (getLiveTop() - getLiveBottom());

        final int column = Math.round((worldX - layout.getX(0)) / COL_STEP);

        final int row = Math.round((worldY - layout.getBaseRowY()) / ROW_STEP);

        final Node[][] nodeGrid = layout.getNodeGrid();

        if (column < 0 || column >= nodeGrid.length || row < 0 || row >= layout.getLongestChainHeight()) {
            return null;
        }

        final Node node = nodeGrid[column][row];

        if (node == null) return null;

        final float dx = Math.abs(worldX - layout.getX(column));
        final float dy = Math.abs(worldY - layout.getY(row));

        return dx <= NODE_WIDTH / 2f && dy <= NODE_HEIGHT / 2f ? new NodeHit(node, column, row) : null;
    }
}
