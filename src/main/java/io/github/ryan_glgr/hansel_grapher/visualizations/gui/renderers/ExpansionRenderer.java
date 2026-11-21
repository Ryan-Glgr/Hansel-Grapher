package io.github.ryan_glgr.hansel_grapher.visualizations.gui.renderers;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;
import io.github.ryan_glgr.hansel_grapher.functionallogic.Interview.Interview;
import io.github.ryan_glgr.hansel_grapher.functionallogic.Node;
import io.github.ryan_glgr.hansel_grapher.functionallogic.lowunits.LowUnit;
import io.github.ryan_glgr.hansel_grapher.visualizations.gui.GLHelper;
import io.github.ryan_glgr.hansel_grapher.visualizations.gui.GUIHelper;
import io.github.ryan_glgr.hansel_grapher.visualizations.gui.LabelPainter;
import io.github.ryan_glgr.hansel_grapher.visualizations.gui.renderers.hanselchain.NodeHit;
import io.github.ryan_glgr.hansel_grapher.visualizations.gui.screens.MainWindow;
import io.github.ryan_glgr.hansel_grapher.visualizations.gui.screens.NodeExplorer;

import java.awt.event.MouseEvent;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.github.ryan_glgr.hansel_grapher.visualizations.gui.screens.MainWindow.EXPANSION_VIEW;
import static io.github.ryan_glgr.hansel_grapher.visualizations.gui.screens.MainWindow.HANSEL_CHAIN_VIEW;
import static io.github.ryan_glgr.hansel_grapher.visualizations.layout.HanselChainLayout.COL_STEP;
import static io.github.ryan_glgr.hansel_grapher.visualizations.layout.HanselChainLayout.NODE_HEIGHT;
import static io.github.ryan_glgr.hansel_grapher.visualizations.layout.HanselChainLayout.NODE_WIDTH;
import static io.github.ryan_glgr.hansel_grapher.visualizations.layout.HanselChainLayout.ROW_STEP;

/**
 * Shows the selected node's up expansions (top row), the selected node (middle row),
 * and its down expansions (bottom row). Looks and behaves like HanselChainRenderer;
 * only the layout differs.
 */
public class ExpansionRenderer extends PanZoomRenderer {

    private static final float WORLD_MARGIN = 2f;

    // Grid rows, bottom to top (row index * ROW_STEP = world y).
    private static final int ROW_DOWN = 0;
    private static final int ROW_SELECTED = 1;
    private static final int ROW_UP = 2;
    private static final int NUM_ROWS = 3;

    private final Interview interview;
    private final Node selectedNode;
    private final MainWindow parentWindow;

    // Node -> low unit, built the same way HanselChainLayout builds it.
    private final Map<Node, LowUnit> lowUnitNodes;

    // nodeGrid[row][column]; rows are ragged and each row is centered around x = 0.
    private final Node[][] nodeGrid = new Node[NUM_ROWS][];
    private final LowUnit.Type[][] lowUnitTypeGrid = new LowUnit.Type[NUM_ROWS][];
    private final String[][][] labelGrid = new String[NUM_ROWS][][];
    private final float[] rowStartX = new float[NUM_ROWS];
    private int totalNodes;
    private int numExclusiveLowUnits;
    private float[] worldBounds;

    private int vaoId;
    private int shaderProgram;
    private final int[] vboIds = new int[2];
    private int projectionUniformLocation = -1;
    private LabelPainter labelPainter;

    // Written by the AWT thread, read by the GL thread.
    private volatile NodeHit hoveredHit;
    // GL thread only.
    private NodeHit renderedHoveredHit;

    public ExpansionRenderer(final Interview interview,
                             final Node selectedNode,
                             final int classificationColorShuffleCounter,
                             final MainWindow parentWindow) {
        super(classificationColorShuffleCounter);
        this.interview = interview;
        this.selectedNode = selectedNode;
        this.parentWindow = parentWindow;
        this.lowUnitNodes = interview.lowUnitsByClass.values()
                .stream()
                .flatMap(Set::stream)
                .collect(Collectors.toMap(LowUnit::getDatapoint, Function.identity()));
        buildGrid();
    }

    // -----------------------------------------------------------------------
    // Per-node info, computed the same way HanselChainLayout does it.
    // -----------------------------------------------------------------------

    /** Low unit type of this node, or null if it is not a low unit. */
    private LowUnit.Type lowUnitTypeOf(final Node node) {
        final LowUnit lowUnit = lowUnitNodes.get(node);
        return lowUnit == null ? null : lowUnit.getLowUnitType();
    }

    /** Label lines for this node. */
    private String[] labelOf(final Node node) {
        return GUIHelper.nodeLabelArray(node, lowUnitTypeOf(node), interview.attributeNames);
    }

    // -----------------------------------------------------------------------
    // Layout
    // -----------------------------------------------------------------------

    private void buildGrid() {
        final List<Node> ups = Arrays.stream(selectedNode.upExpansions).filter(Objects::nonNull).toList();
        final List<Node> downs = Arrays.stream(selectedNode.downExpansions).filter(Objects::nonNull).toList();

        nodeGrid[ROW_UP] = ups.toArray(new Node[0]);
        nodeGrid[ROW_SELECTED] = new Node[]{selectedNode};
        nodeGrid[ROW_DOWN] = downs.toArray(new Node[0]);

        int widest = 1;
        for (int row = 0; row < NUM_ROWS; row++) {
            final int n = nodeGrid[row].length;
            rowStartX[row] = -(n - 1) / 2f * COL_STEP;
            totalNodes += n;
            widest = Math.max(widest, n);

            lowUnitTypeGrid[row] = new LowUnit.Type[n];
            labelGrid[row] = new String[n][];

            for (int c = 0; c < n; c++) {
                final Node node = nodeGrid[row][c];
                lowUnitTypeGrid[row][c] = lowUnitTypeOf(node);
                labelGrid[row][c] = labelOf(node);

                if (LowUnit.Type.EXCLUSIVE.equals(lowUnitTypeGrid[row][c])) {
                    numExclusiveLowUnits++;
                }
            }
        }

        final float halfWidth = (widest - 1) * COL_STEP / 2f + NODE_WIDTH / 2f + WORLD_MARGIN;

        // Assumed order: left, right, bottom, top. Reorder if PanZoomRenderer expects otherwise.
        worldBounds = new float[]{
                -halfWidth,
                halfWidth,
                getY(0) - NODE_HEIGHT / 2f - WORLD_MARGIN,
                getY(NUM_ROWS - 1) + NODE_HEIGHT / 2f + WORLD_MARGIN
        };
    }

    private float getX(final int row, final int column) {
        return rowStartX[row] + column * COL_STEP;
    }

    private float getY(final int row) {
        return row * ROW_STEP;
    }

    @Override
    protected float[] getWorldBounds() {
        return worldBounds;
    }

    // -----------------------------------------------------------------------
    // Buffers
    // -----------------------------------------------------------------------

    private FloatBuffer buildPositionBuffer() {
        final FloatBuffer buffer = Buffers.newDirectFloatBuffer(
                GLHelper.positionBufferFloats(totalNodes, numExclusiveLowUnits));

        for (int row = 0; row < NUM_ROWS; row++) {
            for (int c = 0; c < nodeGrid[row].length; c++) {
                final boolean exclusive = LowUnit.Type.EXCLUSIVE.equals(lowUnitTypeGrid[row][c]);
                GLHelper.emitNode(buffer, getX(row, c), getY(row), NODE_WIDTH, NODE_HEIGHT, exclusive);
            }
        }

        GLHelper.reserveHoverBorderPositions(buffer);

        buffer.flip();
        return buffer;
    }

    // Must iterate in the same order as buildPositionBuffer.
    private FloatBuffer buildColorBuffer() {
        final FloatBuffer buffer = Buffers.newDirectFloatBuffer(
                GLHelper.colorBufferFloats(totalNodes, numExclusiveLowUnits));

        for (int row = 0; row < NUM_ROWS; row++) {
            for (int c = 0; c < nodeGrid[row].length; c++) {
                GLHelper.emitNodeColors(buffer, nodeGrid[row][c], lowUnitTypeGrid[row][c],
                        interview.numClasses, classificationColorShuffleCounter);
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
    // GL lifecycle
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

        gl.glUseProgram(shaderProgram);

        if (hasNewProjection()) {
            gl.glUniformMatrix4fv(projectionUniformLocation, 1, false, consumePendingProjection(), 0);
        }

        gl.glBindVertexArray(vaoId);

        final int nodeVertices = GLHelper.nodeVertexCount(totalNodes, numExclusiveLowUnits);
        gl.glDrawArrays(GL3.GL_TRIANGLES, 0, nodeVertices);

        // Update the reserved hover geometry only when the hovered node changes.
        final NodeHit currentHover = hoveredHit;

        if (!sameHit(currentHover, renderedHoveredHit)) {
            if (currentHover != null) {
                GLHelper.uploadHoverBorder(gl, vboIds, nodeVertices,
                        getX(currentHover.row(), currentHover.column()), getY(currentHover.row()),
                        NODE_WIDTH, NODE_HEIGHT);
            }
            renderedHoveredHit = currentHover;
        }

        // Hover border goes on top of the nodes.
        if (currentHover != null) {
            gl.glDrawArrays(GL3.GL_TRIANGLES, nodeVertices, GLHelper.HOVER_BORDER_VERTICES);
        }

        gl.glBindVertexArray(0);
        gl.glUseProgram(0);

        // Labels remain on top of both the nodes and hover border.
        drawLabels();
    }

    private void drawLabels() {
        final String[] sampleLabel = Arrays.stream(labelGrid)
                .flatMap(Arrays::stream)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);

        if (sampleLabel == null) return;

        if (!labelPainter.begin(surfaceWidth, surfaceHeight,
                getLiveLeft(), getLiveRight(), getLiveBottom(), getLiveTop(),
                NODE_WIDTH, NODE_HEIGHT, sampleLabel)) {
            return;
        }

        // Only a handful of nodes, so no culling needed.
        for (int row = 0; row < NUM_ROWS; row++) {
            for (int c = 0; c < nodeGrid[row].length; c++) {
                final String[] lines = labelGrid[row][c];
                if (lines == null) continue;

                labelPainter.drawLabel(lines, getX(row, c), getY(row));
            }
        }

        labelPainter.end();
    }

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
    public void mouseMoved(final MouseEvent e) {
        final NodeHit newHit = findNodeAt(e.getX(), e.getY());

        if (sameHit(newHit, hoveredHit)) return;

        hoveredHit = newHit;
    }

    @Override
    public void mouseExited(final MouseEvent e) {
        hoveredHit = null;
    }

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

    private NodeHit findNodeAt(final int mouseX, final int mouseY) {
        final float worldX = getLiveLeft() + mouseX / (float) surfaceWidth * (getLiveRight() - getLiveLeft());

        final float worldY = getLiveTop() - mouseY / (float) surfaceHeight * (getLiveTop() - getLiveBottom());

        final int row = Math.round(worldY / ROW_STEP);

        if (row < 0 || row >= NUM_ROWS) return null;

        final Node[] nodesInRow = nodeGrid[row];

        final int column = Math.round((worldX - rowStartX[row]) / COL_STEP);

        if (column < 0 || column >= nodesInRow.length) return null;

        final float dx = Math.abs(worldX - getX(row, column));
        final float dy = Math.abs(worldY - getY(row));

        return dx <= NODE_WIDTH / 2f && dy <= NODE_HEIGHT / 2f
                ? new NodeHit(nodesInRow[column], column, row)
                : null;
    }
}