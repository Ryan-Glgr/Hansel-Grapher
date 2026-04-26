package io.github.ryan_glgr.hansel_grapher.visualizations.gui.renderers;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GL2;
import io.github.ryan_glgr.hansel_grapher.thehardstuff.Interview.Interview;
import io.github.ryan_glgr.hansel_grapher.thehardstuff.Node;
import io.github.ryan_glgr.hansel_grapher.visualizations.gui.GUIHelper;

import java.awt.Color;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class HanselChainRenderer implements GLEventListener {

    // --- Layout constants
    private static final float NODE_WIDTH = 2.0f;
    private static final float NODE_HEIGHT = 1.0f;
    private static final float SIDE_SPACING = 0.75f;  // horizontal gap between chains
    private static final float VERTICAL_SPACING = 0.75f;  // vertical gap between nodes in a chain
    private static final int FLOATS_PER_VERTEX = 6; // 4 floats for color, 2 for xy.
    private static final int VERTICES_PER_NODE = 4; // 4 vertices per node we have to draw.


    private final Interview interview;

    // Per-node layout: maps Node -> [centerX, centerY]
    private final HashMap<Node, float[]> nodePositions = new HashMap<>();
    private ArrayList<ArrayList<Node>> chains;

    // an array of just one, these are the Ids of the VBOs, and we only need one so far.
    private int[] edgeVboIds = new int[1];

    public HanselChainRenderer(final Interview interview) {
        this.interview = interview;
    }

    // --- Layout ---

    private void computeLayout() {
        nodePositions.clear();

        // can be null when we have just started the interview perhaps.
        if (Objects.isNull(chains) || chains.isEmpty()) return;

        // Load chains from interview and compute layout
        chains = GUIHelper.sortChainsForVisualization(interview.hanselChains);

        float chainX = 0.0f;
        for (final ArrayList<Node> chain : chains) {
            final int chainHeight = chain.size();
            // Total height of this chain (centered at y=0)
            final float totalHeight = chainHeight * NODE_HEIGHT + (chainHeight - 1) * VERTICAL_SPACING;
            float nodeY = -(totalHeight / 2.0f) + NODE_HEIGHT / 2.0f; // center of bottom node

            for (final Node node : chain) {
                nodePositions.put(node, new float[]{ chainX, nodeY });
                nodeY += NODE_HEIGHT + VERTICAL_SPACING;
            }
            chainX += NODE_WIDTH + SIDE_SPACING;
        }
    }

    // --- GL helpers ---

    private static void drawFilledRect(final GL2 gl, final float cx, final float cy, final float w, final float h) {
        final float halfW = w / 2f, halfH = h / 2f;
        gl.glBegin(GL2.GL_QUADS);
        gl.glVertex2f(cx - halfW, cy - halfH);
        gl.glVertex2f(cx + halfW, cy - halfH);
        gl.glVertex2f(cx + halfW, cy + halfH);
        gl.glVertex2f(cx - halfW, cy + halfH);
        gl.glEnd();
    }

    private static void drawRectOutline(final GL2 gl, final float cx, final float cy, final float w, final float h) {
        final float halfW = w / 2f, halfH = h / 2f;
        gl.glBegin(GL2.GL_LINE_LOOP);
        gl.glVertex2f(cx - halfW, cy - halfH);
        gl.glVertex2f(cx + halfW, cy - halfH);
        gl.glVertex2f(cx + halfW, cy + halfH);
        gl.glVertex2f(cx - halfW, cy + halfH);
        gl.glEnd();
    }

    private static void drawLine(final GL2 gl, final float x1, final float y1, final float x2, final float y2) {
        gl.glBegin(GL2.GL_LINES);
        gl.glVertex2f(x1, y1);
        gl.glVertex2f(x2, y2);
        gl.glEnd();
    }

    private void uploadNodeVBO(final GL2 gl) {
        final int totalNodes = chains.stream().mapToInt(List::size).sum();

        // 4 vertices per quad, 6 floats per vertex
        final float[] data = new float[totalNodes * VERTICES_PER_NODE * FLOATS_PER_VERTEX];
        int idx = 0;

        for (final ArrayList<Node> chain : chains) {
            for (final Node node : chain) {
                final float[] nodePosition = nodePositions.get(node);

                final boolean isLow = isLowUnit(node);
                final Color c = GUIHelper.getColorForClass(node.classification, isLow);
                final float r = c.getRed()   / 255f;
                final float g = c.getGreen() / 255f;
                final float b = c.getBlue()  / 255f;
                final float a = c.getAlpha() / 255f;

                final float cx = nodePosition[0];
                final float cy = nodePosition[1];
                final float hw = NODE_WIDTH  / 2f;
                final float hh = NODE_HEIGHT / 2f;

                // bottom-left
                data[idx++] = cx - hw;
                data[idx++] = cy - hh;
                data[idx++] = r;
                data[idx++] = g;
                data[idx++] = b;
                data[idx++] = a;
                // bottom-right
                data[idx++] = cx + hw; data[idx++] = cy - hh;
                data[idx++] = r; data[idx++] = g; data[idx++] = b; data[idx++] = a;
                // top-right
                data[idx++] = cx + hw; data[idx++] = cy + hh;
                data[idx++] = r; data[idx++] = g; data[idx++] = b; data[idx++] = a;
                // top-left
                data[idx++] = cx - hw; data[idx++] = cy + hh;
                data[idx++] = r; data[idx++] = g; data[idx++] = b; data[idx++] = a;
            }
        }
        final FloatBuffer buf = Buffers.newDirectFloatBuffer(data);
        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, edgeVboIds[0]);
        gl.glBufferData(GL2.GL_ARRAY_BUFFER, (long) data.length * Float.BYTES, buf, GL2.GL_STATIC_DRAW);
        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);
    }

    // --- GLEventListener ---

    @Override
    public void init(final GLAutoDrawable drawable) {
        final GL2 gl = drawable.getGL().getGL2();
        gl.glClearColor(0.15f, 0.15f, 0.15f, 1.0f);
        gl.glEnable(GL2.GL_BLEND);
        gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
        gl.glLineWidth(2.0f);

        computeLayout();

        // Allocate GPU buffer handles
        gl.glGenBuffers(1, edgeVboIds, 0);

        // Upload geometry now that layout is computed
        uploadEdgeVBO(gl);

    }

    @Override
    public void display(final GLAutoDrawable drawable) {
        final GL2 gl = drawable.getGL().getGL2();
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT);

        if (chains == null) return;

        // --- Draw edges first (under nodes) ---
        gl.glColor4f(1.0f, 1.0f, 1.0f, 0.8f);
        for (final ArrayList<Node> chain : chains) {
            for (int i = 0; i < chain.size() - 1; i++) {
                final float[] posA = nodePositions.get(chain.get(i));
                final float[] posB = nodePositions.get(chain.get(i + 1));
                if (posA == null || posB == null) continue;
                // Connect top edge of lower node to bottom edge of upper node
                drawLine(gl, posA[0], posA[1] + NODE_HEIGHT / 2f,
                        posB[0], posB[1] - NODE_HEIGHT / 2f);
            }
        }

        // --- Draw nodes ---
        for (final ArrayList<Node> chain : chains) {
            for (final Node node : chain) {
                final float[] pos = nodePositions.get(node);
                if (pos == null) continue;

                final boolean isLow = isLowUnit(node);
                final Color color = GUIHelper.getColorForClass(node.classification, isLow);
                gl.glColor4f(color.getRed() / 255f,
                        color.getGreen() / 255f,
                        color.getBlue() / 255f,
                        color.getAlpha() / 255f);

                drawFilledRect(gl, pos[0], pos[1], NODE_WIDTH, NODE_HEIGHT);

                // Outline
                gl.glColor4f(0f, 0f, 0f, 1f);
                drawRectOutline(gl, pos[0], pos[1], NODE_WIDTH, NODE_HEIGHT);
            }
        }
    }

    @Override
    public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {
        final GL2 gl = drawable.getGL().getGL2();
        gl.glViewport(0, 0, width, height);

        // Set up an orthographic projection that shows the whole graph with a small margin
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();

        final float margin = 1.5f;
        final float graphW = chains == null ? 10f : chains.size() * (NODE_WIDTH + SIDE_SPACING);
        final float graphH = chains == null ? 10f : chains.stream().mapToInt(ArrayList::size).max().orElse(1)
                * (NODE_HEIGHT + VERTICAL_SPACING);
        final float aspect = (float) width / height;
        final float halfH   = (graphH / 2f) + margin;
        final float halfW   = halfH * aspect;

        gl.glOrtho(-halfW, halfW, -halfH, halfH, -1, 1);

        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();
    }

    @Override
    public void dispose(final GLAutoDrawable drawable) {
        // Nothing to clean up yet (no GPU buffers allocated)
    }

    // --- Helpers ---

    private boolean isLowUnit(final Node node) {
        final ArrayList<ArrayList<Node>> lowUnits = interview.adjustedLowUnitsByClass;
        if (lowUnits == null)
            return false;
        for (final ArrayList<Node> list : lowUnits)
            if (list != null && list.contains(node))
                return true;
        return false;
    }
}