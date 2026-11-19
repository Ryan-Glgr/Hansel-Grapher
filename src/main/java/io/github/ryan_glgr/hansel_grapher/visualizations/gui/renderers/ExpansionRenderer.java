package io.github.ryan_glgr.hansel_grapher.visualizations.gui.renderers;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;
import io.github.ryan_glgr.hansel_grapher.functionallogic.Interview.Interview;
import io.github.ryan_glgr.hansel_grapher.functionallogic.Node;
import io.github.ryan_glgr.hansel_grapher.visualizations.gui.GLHelper;
import io.github.ryan_glgr.hansel_grapher.visualizations.gui.GUIHelper;
import io.github.ryan_glgr.hansel_grapher.visualizations.gui.renderers.hanselchain.NodeHit;
import io.github.ryan_glgr.hansel_grapher.visualizations.gui.screens.MainWindow;
import io.github.ryan_glgr.hansel_grapher.visualizations.gui.screens.NodeExplorer;

import java.awt.Color;
import java.awt.event.MouseEvent;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static io.github.ryan_glgr.hansel_grapher.visualizations.gui.screens.MainWindow.EXPANSION_VIEW;
import static io.github.ryan_glgr.hansel_grapher.visualizations.gui.screens.MainWindow.HANSEL_CHAIN_VIEW;
import static io.github.ryan_glgr.hansel_grapher.visualizations.layout.HanselChainLayout.COL_STEP;
import static io.github.ryan_glgr.hansel_grapher.visualizations.layout.HanselChainLayout.NODE_HEIGHT;
import static io.github.ryan_glgr.hansel_grapher.visualizations.layout.HanselChainLayout.NODE_WIDTH;
import static io.github.ryan_glgr.hansel_grapher.visualizations.layout.HanselChainLayout.ROW_STEP;

/**
 * Shows the selected node's up expansions (top row), the selected node (middle row),
 * and its down expansions (bottom row). Hover and click behavior match HanselChainRenderer.
 */
public class ExpansionRenderer extends PanZoomRenderer {

    private static final int POSITION_COMPONENTS = 2;
    private static final int COLOR_COMPONENTS = 4;
    private static final int VERTICES_PER_QUAD = 6;

    private static final int VBO_POSITIONS = 0;
    private static final int VBO_COLORS = 1;

    private static final int HOVER_BORDER_QUADS = 4;
    private static final int HOVER_BORDER_VERTICES = HOVER_BORDER_QUADS * VERTICES_PER_QUAD;
    private static final float HOVER_BORDER_THICKNESS = 0.2f;
    private static final Color HOVER_BORDER_COLOR = new Color(220, 70, 220);

    private static final float WORLD_MARGIN = 2f;

    // Grid rows, bottom to top (row index * ROW_STEP = world y).
    private static final int ROW_DOWN = 0;
    private static final int ROW_SELECTED = 1;
    private static final int ROW_UP = 2;
    private static final int NUM_ROWS = 3;

    // Identifies both the node and its position in the grid.

    private final Interview interview;
    private final Node selectedNode;
    private final MainWindow parentWindow;

    // nodeGrid[row][column]; rows are ragged and each row is centered around x = 0.
    private final Node[][] nodeGrid = new Node[NUM_ROWS][];
    private final float[] rowStartX = new float[NUM_ROWS];
    private int totalNodes;
    private float[] worldBounds;

    private int vaoId;
    private int shaderProgram;
    private final int[] vboIds = new int[2];
    private int projectionUniformLocation = -1;

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
        buildGrid();
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
                (totalNodes * VERTICES_PER_QUAD + HOVER_BORDER_VERTICES) * POSITION_COMPONENTS);

        for (int row = 0; row < NUM_ROWS; row++) {
            for (int c = 0; c < nodeGrid[row].length; c++) {
                final float cx = getX(row, c);
                final float cy = getY(row);
                GLHelper.emitQuad(buffer,
                        cx - NODE_WIDTH / 2f, cy - NODE_HEIGHT / 2f,
                        cx + NODE_WIDTH / 2f, cy + NODE_HEIGHT / 2f);
            }
        }

        // Reserved for the hover border; filled in dynamically.
        for (int i = 0; i < HOVER_BORDER_VERTICES * POSITION_COMPONENTS; i++) {
            buffer.put(0f);
        }

        buffer.flip();
        return buffer;
    }

    // Must iterate in the same order as buildPositionBuffer.
    private FloatBuffer buildColorBuffer() {
        final FloatBuffer buffer = Buffers.newDirectFloatBuffer(
                (totalNodes * VERTICES_PER_QUAD + HOVER_BORDER_VERTICES) * COLOR_COMPONENTS);

        for (final Node[] row : nodeGrid) {
            for (final Node node : row) {
                final int cls = node.classification.equals(Node.IMPOSSIBLE_CLASSIFICATION)
                        ? Node.IMPOSSIBLE_CLASSIFICATION
                        : (node.classification + classificationColorShuffleCounter) % interview.numClasses;

                final Color c = GUIHelper.getColorForClass(cls, false);
                for (int i = 0; i < VERTICES_PER_QUAD; i++) {
                    buffer.put(c.getRed() / 255f);
                    buffer.put(c.getGreen() / 255f);
                    buffer.put(c.getBlue() / 255f);
                    buffer.put(c.getAlpha() / 255f);
                }
            }
        }

        for (int i = 0; i < HOVER_BORDER_VERTICES; i++) {
            buffer.put(HOVER_BORDER_COLOR.getRed() / 255f);
            buffer.put(HOVER_BORDER_COLOR.getGreen() / 255f);
            buffer.put(HOVER_BORDER_COLOR.getBlue() / 255f);
            buffer.put(1f);
        }

        buffer.flip();
        return buffer;
    }

    private void updateHoverBorderVBO(final GL3 gl, final NodeHit hit) {
        final FloatBuffer buffer = Buffers.newDirectFloatBuffer(HOVER_BORDER_VERTICES * POSITION_COMPONENTS);

        final float cx = getX(hit.row(), hit.column());
        final float cy = getY(hit.row());

        final float left = cx - NODE_WIDTH / 2f;
        final float right = cx + NODE_WIDTH / 2f;
        final float bottom = cy - NODE_HEIGHT / 2f;
        final float top = cy + NODE_HEIGHT / 2f;
        final float t = Math.min(NODE_WIDTH, NODE_HEIGHT) * HOVER_BORDER_THICKNESS;

        GLHelper.emitQuad(buffer, left, top - t, right, top);             // top
        GLHelper.emitQuad(buffer, left, bottom, right, bottom + t);       // bottom
        GLHelper.emitQuad(buffer, left, bottom + t, left + t, top - t);   // left
        GLHelper.emitQuad(buffer, right - t, bottom + t, right, top - t); // right

        buffer.flip();

        final long offset = (long) totalNodes * VERTICES_PER_QUAD * POSITION_COMPONENTS * Float.BYTES;

        gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, vboIds[VBO_POSITIONS]);
        gl.glBufferSubData(GL3.GL_ARRAY_BUFFER, offset, (long) buffer.remaining() * Float.BYTES, buffer);
        gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, 0);
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

        gl.glClearColor(0.15f, 0.15f, 0.15f, 1.0f);

        shaderProgram = GLHelper.createShaderProgram(gl);
        projectionUniformLocation = gl.glGetUniformLocation(shaderProgram, "uProjection");
        if (projectionUniformLocation == -1) {
            throw new RuntimeException("Uniform 'uProjection' not found in shader program.");
        }

        final int[] vaos = new int[1];
        gl.glGenVertexArrays(1, vaos, 0);
        vaoId = vaos[0];
        if (vaoId == 0) {
            throw new RuntimeException("VAO creation failed");
        }

        gl.glBindVertexArray(vaoId);
        gl.glGenBuffers(2, vboIds, 0);

        gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, vboIds[VBO_POSITIONS]);
        final FloatBuffer positions = buildPositionBuffer();
        gl.glBufferData(GL3.GL_ARRAY_BUFFER, (long) positions.capacity() * Float.BYTES, positions, GL3.GL_DYNAMIC_DRAW);
        gl.glEnableVertexAttribArray(0);
        gl.glVertexAttribPointer(0, POSITION_COMPONENTS, GL3.GL_FLOAT, false, 0, 0);

        gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, vboIds[VBO_COLORS]);
        final FloatBuffer colors = buildColorBuffer();
        gl.glBufferData(GL3.GL_ARRAY_BUFFER, (long) colors.capacity() * Float.BYTES, colors, GL3.GL_STATIC_DRAW);
        gl.glEnableVertexAttribArray(1);
        gl.glVertexAttribPointer(1, COLOR_COMPONENTS, GL3.GL_FLOAT, false, 0, 0);

        gl.glBindVertexArray(0);
        gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, 0);

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

        final int nodeVertices = totalNodes * VERTICES_PER_QUAD;
        gl.glDrawArrays(GL3.GL_TRIANGLES, 0, nodeVertices);

        // Update the reserved hover geometry only when the hovered node changes.
        final NodeHit currentHover = hoveredHit;

        if (!sameHit(currentHover, renderedHoveredHit)) {
            if (currentHover != null) {
                updateHoverBorderVBO(gl, currentHover);
            }
            renderedHoveredHit = currentHover;
        }

        if (currentHover != null) {
            gl.glDrawArrays(GL3.GL_TRIANGLES, nodeVertices, HOVER_BORDER_VERTICES);
        }

        gl.glBindVertexArray(0);
        gl.glUseProgram(0);
    }

    @Override
    public void dispose(final GLAutoDrawable drawable) {
        final GL3 gl = drawable.getGL().getGL3();
        gl.glDeleteBuffers(2, vboIds, 0);
        gl.glDeleteVertexArrays(1, new int[]{vaoId}, 0);
        gl.glDeleteProgram(shaderProgram);
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