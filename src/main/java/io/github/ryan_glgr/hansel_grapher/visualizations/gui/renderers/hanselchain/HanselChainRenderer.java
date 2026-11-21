package io.github.ryan_glgr.hansel_grapher.visualizations.gui.renderers.hanselchain;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;
import io.github.ryan_glgr.hansel_grapher.functionallogic.Interview.Interview;
import io.github.ryan_glgr.hansel_grapher.functionallogic.Interview.LiveInterviewVisualizer;
import io.github.ryan_glgr.hansel_grapher.functionallogic.Node;
import io.github.ryan_glgr.hansel_grapher.functionallogic.lowunits.LowUnit;
import io.github.ryan_glgr.hansel_grapher.visualizations.gui.GLHelper;
import io.github.ryan_glgr.hansel_grapher.visualizations.gui.LabelPainter;
import io.github.ryan_glgr.hansel_grapher.visualizations.gui.renderers.PanZoomRenderer;
import io.github.ryan_glgr.hansel_grapher.visualizations.gui.screens.MainWindow;
import io.github.ryan_glgr.hansel_grapher.visualizations.gui.screens.NodeExplorer;
import io.github.ryan_glgr.hansel_grapher.visualizations.layout.HanselChainLayout;

import java.awt.event.MouseEvent;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.Objects;

import static io.github.ryan_glgr.hansel_grapher.visualizations.gui.screens.MainWindow.EXPANSION_VIEW;
import static io.github.ryan_glgr.hansel_grapher.visualizations.gui.screens.MainWindow.HANSEL_CHAIN_VIEW;
import static io.github.ryan_glgr.hansel_grapher.visualizations.layout.HanselChainLayout.*;

public class HanselChainRenderer extends PanZoomRenderer implements LiveInterviewVisualizer {

    // --- Layout constants pulled from HanselChainLayout via static import:
    // NODE_WIDTH, NODE_HEIGHT, SIDE_SPACING, VERTICAL_SPACING, MARGIN, ROW_STEP, COL_STEP

    private int vaoId;
    private int shaderProgram;
    private final int[] vboIds = new int[2];
    private int projectionUniformLocation = -1;
    private volatile boolean colorsDirty;
    private LabelPainter labelPainter;

    private final int numClasses;
    private final HanselChainLayout layout;

    // Desired hover state, potentially updated by the AWT event thread.
    private volatile NodeHit hoveredHit;

    // Last hover state uploaded to the GPU. Accessed only by the GL thread.
    private NodeHit renderedHoveredHit;

    private final MainWindow parentWindow;

    public HanselChainRenderer(final Interview interview, final int classificationColorShuffleCounter, final MainWindow parentWindow) {

        super(classificationColorShuffleCounter);

        this.numClasses = interview.numClasses;
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

        final FloatBuffer buffer = Buffers.newDirectFloatBuffer(
                GLHelper.positionBufferFloats(layout.getTotalNodes(), layout.getNumExclusiveLowUnits()));

        for (int c = 0; c < nodeGrid.length; c++) {
            final float cx = layout.getX(c);

            for (int row = 0; row < layout.getLongestChainHeight(); row++) {
                final Node node = nodeGrid[c][row];
                if (node == null) continue;

                final boolean exclusive = LowUnit.Type.EXCLUSIVE.equals(layout.getLowUnitType(node));

                GLHelper.emitNode(buffer, cx, layout.getY(row), NODE_WIDTH, NODE_HEIGHT, exclusive);
            }
        }

        // Reserve space for the hover border. Its positions are updated dynamically.
        GLHelper.reserveHoverBorderPositions(buffer);

        buffer.flip();
        return buffer;
    }

    // [r, g, b, a] per vertex — rebuilt whenever classifications change.
    // Must iterate in the same order as buildPositionBuffer.
    private FloatBuffer buildColorBuffer() {
        final Node[][] nodeGrid = layout.getNodeGrid();

        final FloatBuffer buffer = Buffers.newDirectFloatBuffer(
                GLHelper.colorBufferFloats(layout.getTotalNodes(), layout.getNumExclusiveLowUnits()));

        for (final Node[] chain : nodeGrid) {
            for (final Node node : chain) {
                if (node == null) continue;

                GLHelper.emitNodeColors(buffer, node, layout.getLowUnitType(node), numClasses, classificationColorShuffleCounter);
            }
        }

        GLHelper.emitHoverBorderColors(buffer);

        buffer.flip();
        return buffer;
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

        vaoId = GLHelper.createNodeVao(gl, vboIds, buildPositionBuffer(), buildColorBuffer());

        labelPainter = new LabelPainter();

        reshape(drawable, 0, 0, drawable.getSurfaceWidth(), drawable.getSurfaceHeight());

        super.init(drawable); // registers mouse listeners
    }

    @Override
    public void display(final GLAutoDrawable drawable) {
        final GL3 gl = drawable.getGL().getGL3();

        gl.glClear(GL3.GL_COLOR_BUFFER_BIT);

        if (colorsDirty) {
            GLHelper.uploadColors(gl, vboIds, buildColorBuffer());
            colorsDirty = false;
        }

        gl.glUseProgram(shaderProgram);

        if (hasNewProjection()) {
            gl.glUniformMatrix4fv(projectionUniformLocation, 1, false, consumePendingProjection(), 0);
        }

        gl.glBindVertexArray(vaoId);

        final int numberOfVertices = GLHelper.nodeVertexCount(layout.getTotalNodes(), layout.getNumExclusiveLowUnits());

        // Draw all node geometry.
        gl.glDrawArrays(GL3.GL_TRIANGLES, 0, numberOfVertices);

        // Update the reserved hover geometry only when the hovered node changes.
        final NodeHit currentHover = hoveredHit;

        if (!sameHit(currentHover, renderedHoveredHit)) {
            if (currentHover != null) {
                GLHelper.uploadHoverBorder(gl, vboIds, numberOfVertices,
                        layout.getX(currentHover.column()), layout.getY(currentHover.row()),
                        NODE_WIDTH, NODE_HEIGHT);
            }

            renderedHoveredHit = currentHover;
        }

        // Draw the hover border on top of the nodes.
        if (currentHover != null) {
            gl.glDrawArrays(GL3.GL_TRIANGLES, numberOfVertices, GLHelper.HOVER_BORDER_VERTICES);
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
        final String[][][] labelGrid = layout.getLabelGrid();
        final float[] columnX = layout.getColumnX();
        final int longestChainHeight = layout.getLongestChainHeight();

        final String[] sampleLabel = Arrays.stream(labelGrid)
                .flatMap(Arrays::stream)
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No node labels exist"));

        // Returns false when the text would be too small to be worth drawing.
        if (!labelPainter.begin(surfaceWidth, surfaceHeight,
                getLiveLeft(), getLiveRight(), getLiveBottom(), getLiveTop(),
                NODE_WIDTH, NODE_HEIGHT, sampleLabel)) {
            return;
        }

        // Only draw labels for nodes that are currently on screen.
        final int colMin = clamp((int) Math.floor((getLiveLeft() - NODE_WIDTH / 2f) / COL_STEP), 0, columnX.length - 1);

        final int colMax = clamp((int) Math.ceil((getLiveRight() + NODE_WIDTH / 2f) / COL_STEP), 0, columnX.length - 1);

        final int rowMin = clamp((int) Math.floor((getLiveBottom() - NODE_HEIGHT / 2f - layout.getBaseRowY()) / ROW_STEP), 0, longestChainHeight - 1);

        final int rowMax = clamp((int) Math.ceil((getLiveTop() + NODE_HEIGHT / 2f - layout.getBaseRowY()) / ROW_STEP), 0, longestChainHeight - 1);

        for (int c = colMin; c <= colMax; c++) {
            for (int r = rowMin; r <= rowMax; r++) {
                final String[] lines = labelGrid[c][r];

                if (lines == null) continue;

                labelPainter.drawLabel(lines, columnX[c], layout.getY(r));
            }
        }

        labelPainter.end();
    }

    private static int clamp(final int v, final int lo, final int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    // -----------------------------------------------------------------------
    // Disposal
    // -----------------------------------------------------------------------

    @Override
    public void dispose(final GLAutoDrawable drawable) {
        GLHelper.disposeNodeResources(drawable.getGL().getGL3(), vaoId, vboIds, shaderProgram);

        if (labelPainter != null) {
            labelPainter.dispose();
        }
        super.dispose(drawable);
    }

    // -----------------------------------------------------------------------
    // Mouse events
    // -----------------------------------------------------------------------
    @Override
    public void mouseClicked(final MouseEvent e) {
        final NodeHit hit = findNodeAt(e.getX(), e.getY());

        if (hit == null) return;

        final NodeExplorer.NodeExplorerResult choice = NodeExplorer.createNodeExplorerWindowAndGetResult();

        switch (choice) {
            case NodeExplorer.NodeExplorerResult.SEE_EXPANSIONS -> {
                parentWindow.handleVisualizationChange(EXPANSION_VIEW, hit.node());
            }

            // TODO: these two cases need to spawn a little box which asks for the class. then invoke some Node.permeate style method.
            case ADJUST_WITHOUT_EXPANSION -> {
//                hit.node().permeateClassification()
                parentWindow.handleVisualizationChange(HANSEL_CHAIN_VIEW, null);
            }
            case ADJUST_WITH_EXPANSION -> {
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
