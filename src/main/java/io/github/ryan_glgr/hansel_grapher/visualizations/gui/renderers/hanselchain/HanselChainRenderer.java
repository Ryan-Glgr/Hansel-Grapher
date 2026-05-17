package io.github.ryan_glgr.hansel_grapher.visualizations.gui.renderers.hanselchain;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.util.awt.TextRenderer;
import io.github.ryan_glgr.hansel_grapher.thehardstuff.Interview.Interview;
import io.github.ryan_glgr.hansel_grapher.thehardstuff.Interview.LiveInterviewVisualizer;
import io.github.ryan_glgr.hansel_grapher.thehardstuff.Node;
import io.github.ryan_glgr.hansel_grapher.visualizations.gui.GUIHelper;
import io.github.ryan_glgr.hansel_grapher.visualizations.gui.renderers.PanZoomRenderer;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;

public class HanselChainRenderer extends PanZoomRenderer implements LiveInterviewVisualizer {

    // --- Layout constants
    private static final float NODE_WIDTH = 4.0f;
    private static final float NODE_HEIGHT = 2.0f;
    private static final float SIDE_SPACING = NODE_WIDTH / 10f;
    private static final float VERTICAL_SPACING = NODE_HEIGHT / 6f;
    private static final float MARGIN = SIDE_SPACING + VERTICAL_SPACING;
    private static final int FONT_SIZE = 14;
    private static final float TEXT_PADDING_INSIDE_NODE = 1.5f;

    private static final int POSITION_COMPONENTS = 2;   // x, y
    private static final int X_POSITION_IN_ARRAY = 0;
    private static final int Y_POSITION_IN_ARRAY = 1;
    private static final int COLOR_COMPONENTS = 4;      // r, g, b, a
    private static final int VERTICES_PER_NODE = 6;

    private static final int VBO_POSITIONS = 0;         // vboIds[0] -> positions, never updated
    private static final int VBO_COLORS    = 1;         // vboIds[1] -> colors, updated on dirty

    private static final String VERTEX_SHADER_FILE   = "VertexShader.glsl";
    private static final String FRAGMENT_SHADER_FILE = "FragmentShader.glsl";
    private static final int SHADER_LOG_BUFFER_SIZE = 1024;

    private static final float[] CLEAR_COLOR = {0.15f, 0.15f, 0.15f, 1.0f};

    private int vaoId;
    private int shaderProgram;
    private final int[] vboIds = new int[2];    // [VBO_POSITIONS, VBO_COLORS]
    private int projectionUniformLocation = -1;
    private float[] worldBounds;  // { minX, maxX, minY, maxY }, set once after layout
    private volatile boolean colorsDirty;
    private TextRenderer textRenderer;

    private final Interview interview;

    // Assigned once on the GL thread in init() and never mutated afterwards.
    // All inter-thread communication goes through colorsDirty.
    private ArrayList<ArrayList<Node>> chains;
    private int totalNodes;
    // Per-node layout: maps Node -> [centerX, centerY]
    private final HashMap<Node, float[]> nodePositions = new HashMap<>();
    private final HashMap<Node, String[]> nodeLabels = new HashMap<>();

    public HanselChainRenderer(final Interview interview) {
        this.interview = interview;
    }

    // Called from the compute thread whenever node classifications change.
    // to be used in the interview each time we have udpated. will provide an interface against which this works.
    @Override
    public void notifyClassificationsChanged() {
        colorsDirty = true;
    }

    // --- Layout ---

    private void computeLayout() {
        nodePositions.clear();

        float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
        float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;

        float chainX = 0.0f;
        for (final ArrayList<Node> chain : chains) {
            final int chainHeight = chain.size();
            final float totalHeight = chainHeight * NODE_HEIGHT + (chainHeight - 1) * VERTICAL_SPACING;
            float nodeY = -(totalHeight / 2.0f) + NODE_HEIGHT / 2.0f;

            for (final Node node : chain) {
                nodePositions.put(node, new float[]{ chainX, nodeY });
                nodeLabels.put(node, GUIHelper.nodeLabelArray(node, isLowUnit(node)));

                minX = Math.min(minX, chainX - NODE_WIDTH  / 2f);
                maxX = Math.max(maxX, chainX + NODE_WIDTH  / 2f);
                minY = Math.min(minY, nodeY  - NODE_HEIGHT / 2f);
                maxY = Math.max(maxY, nodeY  + NODE_HEIGHT / 2f);

                nodeY += NODE_HEIGHT + VERTICAL_SPACING;
            }
            chainX += NODE_WIDTH + SIDE_SPACING;
        }

        worldBounds = new float[]{ minX - MARGIN, maxX + MARGIN, minY - MARGIN, maxY + MARGIN };
    }

    @Override
    protected float[] getWorldBounds() {
        return worldBounds;
    }

    // --- Buffer builders ---

    // [x, y] per node — built once, never rebuilt.
    private FloatBuffer buildPositionBuffer() {
        final FloatBuffer buffer = Buffers.newDirectFloatBuffer(totalNodes * VERTICES_PER_NODE * POSITION_COMPONENTS);

        for (final ArrayList<Node> chain : chains) {
            for (final Node node : chain) {
                final float[] pos = nodePositions.get(node);
                final float cx = pos[X_POSITION_IN_ARRAY];
                final float cy = pos[Y_POSITION_IN_ARRAY];
                final float l = cx - NODE_WIDTH  / 2f;
                final float r = cx + NODE_WIDTH  / 2f;
                final float b = cy - NODE_HEIGHT / 2f;
                final float t = cy + NODE_HEIGHT / 2f;

                // Triangle 1
                buffer.put(l); buffer.put(b);
                buffer.put(r); buffer.put(b);
                buffer.put(r); buffer.put(t);
                // Triangle 2
                buffer.put(l); buffer.put(b);
                buffer.put(r); buffer.put(t);
                buffer.put(l); buffer.put(t);
            }
        }

        buffer.flip();
        return buffer;
    }

    // [r, g, b, a] per node — rebuilt whenever classifications change.
    private FloatBuffer buildColorBuffer() {
        final FloatBuffer buffer = Buffers.newDirectFloatBuffer(totalNodes * VERTICES_PER_NODE * COLOR_COMPONENTS);

        for (final ArrayList<Node> chain : chains) {
            for (final Node node : chain) {
                final boolean isLow = isLowUnit(node);
                final Color c = GUIHelper.getColorForClass(node.classification, isLow);
                final float r = c.getRed()   / 255f;
                final float g = c.getGreen() / 255f;
                final float b = c.getBlue()  / 255f;
                final float a = c.getAlpha() / 255f;
                for (int i = 0; i < VERTICES_PER_NODE; i++) {
                    buffer.put(r);
                    buffer.put(g);
                    buffer.put(b);
                    buffer.put(a);
                }
            }
        }

        buffer.flip();
        return buffer;
    }

    // --- GL event listener ---

    @Override
    public void init(final GLAutoDrawable drawable) {
        final GL3 gl = drawable.getGL().getGL3();
        if (gl == null)
            throw new RuntimeException("GL3 context not available — check GLProfile at canvas creation.");

        // ------------------------------------------------------------
        // 1. Basic GL state
        // ------------------------------------------------------------
        gl.glClearColor(CLEAR_COLOR[0], CLEAR_COLOR[1], CLEAR_COLOR[2], CLEAR_COLOR[3]);
        gl.glEnable(GL3.GL_BLEND);
        gl.glEnable(GL3.GL_PROGRAM_POINT_SIZE);
        gl.glBlendFunc(GL3.GL_SRC_ALPHA, GL3.GL_ONE_MINUS_SRC_ALPHA);

        // ------------------------------------------------------------
        // 2. Snapshot chains, compute static layout
        // ------------------------------------------------------------
        chains = GUIHelper.sortChainsForVisualization(interview.hanselChains);
        totalNodes = chains.stream().mapToInt(List::size).sum();
        computeLayout();

        // ------------------------------------------------------------
        // 3. Shader program
        // ------------------------------------------------------------
        shaderProgram = createShaderProgram(gl);
        projectionUniformLocation = gl.glGetUniformLocation(shaderProgram, "uProjection");
        if (projectionUniformLocation == -1)
            throw new RuntimeException("Uniform 'uProjection' not found in shader program.");

        // can be removed
        gl.glValidateProgram(shaderProgram);
        final int[] validateStatus = new int[1];
        gl.glGetProgramiv(shaderProgram, GL3.GL_VALIDATE_STATUS, validateStatus, 0);
        if (validateStatus[0] == GL3.GL_FALSE) {
            final byte[] log = new byte[SHADER_LOG_BUFFER_SIZE];
            gl.glGetProgramInfoLog(shaderProgram, SHADER_LOG_BUFFER_SIZE, null, 0, log, 0);
        }

        // ------------------------------------------------------------
        // 4. VAO
        // ------------------------------------------------------------
        final int[] vaos = new int[1];
        gl.glGenVertexArrays(1, vaos, 0);
        vaoId = vaos[0];
        if (vaoId == 0)
            throw new RuntimeException("VAO creation failed");

        gl.glBindVertexArray(vaoId);


        // ------------------------------------------------------------
        // 5. Two VBOs
        // ------------------------------------------------------------
        gl.glGenBuffers(2, vboIds, 0);

        // --- Position VBO (static, GL_STATIC_DRAW) ---
        gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, vboIds[VBO_POSITIONS]);

        final FloatBuffer positionBuffer = buildPositionBuffer();
        gl.glBufferData(
                GL3.GL_ARRAY_BUFFER,
                (long) positionBuffer.capacity() * Float.BYTES,
                positionBuffer,
                GL3.GL_STATIC_DRAW
        );

        gl.glEnableVertexAttribArray(0);
        gl.glVertexAttribPointer(0, POSITION_COMPONENTS, GL3.GL_FLOAT, false, 0, 0);

        // --- Color VBO (dynamic, GL_DYNAMIC_DRAW) ---
        gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, vboIds[VBO_COLORS]);

        final FloatBuffer colorBuffer = buildColorBuffer();
        gl.glBufferData(
                GL3.GL_ARRAY_BUFFER,
                (long) colorBuffer.capacity() * Float.BYTES,
                colorBuffer,
                GL3.GL_DYNAMIC_DRAW
        );

        gl.glEnableVertexAttribArray(1);
        gl.glVertexAttribPointer(1, COLOR_COMPONENTS, GL3.GL_FLOAT, false, 0, 0);

        // ------------------------------------------------------------
        // 6. Cleanup bindings
        // ------------------------------------------------------------
        gl.glBindVertexArray(0);
        gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, 0);

        textRenderer = new TextRenderer(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, FONT_SIZE));

        // ------------------------------------------------------------
        // 7. Upload initial projection so first display() is correct
        // ------------------------------------------------------------
        reshape(drawable, 0, 0, drawable.getSurfaceWidth(), drawable.getSurfaceHeight());
        super.init(drawable);   // registers mouse listeners
    }

    // Overwrites the color VBO in-place. Node count is unchanging so SubData is safe.
    private void rebuildColorVBO(final GL3 gl) {
        final FloatBuffer colorBuffer = buildColorBuffer();
        gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, vboIds[VBO_COLORS]);
        gl.glBufferSubData(
                GL3.GL_ARRAY_BUFFER,
                0,
                (long) colorBuffer.capacity() * Float.BYTES,
                colorBuffer
        );
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

        // Replace the old pendingProjection field with the base class version:
        if (hasNewProjection()) {
            gl.glUniformMatrix4fv(projectionUniformLocation, 1, false,
                    consumePendingProjection(), 0);
        }

        gl.glBindVertexArray(vaoId);
        gl.glDrawArrays(GL3.GL_TRIANGLES, 0, totalNodes * VERTICES_PER_NODE);
        gl.glBindVertexArray(0);
        gl.glUseProgram(0);
        drawLabels();
    }

    private void drawLabels() {
        final float viewW = getLiveRight() - getLiveLeft();
        final float viewH = getLiveTop()   - getLiveBottom();
        final float scaleX = surfaceWidth  / viewW;
        final float scaleY = surfaceHeight / viewH;

        final float nodeWidthPx  = NODE_WIDTH  * scaleX;
        final float nodeHeightPx = NODE_HEIGHT * scaleY;

        final String[] sampleNodeLabel = nodeLabels.values().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No node labels exist"));

        final double lineHeight = textRenderer.getBounds("Ag").getHeight();
        final double totalHeight = lineHeight * sampleNodeLabel.length;

        final double longestLine = Arrays.stream(sampleNodeLabel)
                .mapToDouble(line -> textRenderer.getBounds(line).getWidth())
                .max()
                .orElseThrow(() -> new IllegalStateException("No node labels exist"));

        if (longestLine > nodeWidthPx - TEXT_PADDING_INSIDE_NODE) return;
        if (totalHeight > nodeHeightPx - TEXT_PADDING_INSIDE_NODE) return;

        textRenderer.beginRendering(surfaceWidth, surfaceHeight);
        textRenderer.setColor(0f, 0f, 0f, 1f);

        nodeLabels.forEach((key, lines) -> {

            // skip a node if it's not in screen
            final float[] pos = nodePositions.get(key);
            final float screenX = (pos[0] - getLiveLeft()) / viewW * surfaceWidth;
            final float screenY = (pos[1] - getLiveBottom()) / viewH * surfaceHeight;

            // skip nodes outside the screen entirely
            if (screenX < -nodeWidthPx || screenX > surfaceWidth + nodeWidthPx) return;
            if (screenY < -nodeHeightPx || screenY > surfaceHeight + nodeHeightPx) return;


            final float startY = (float) (screenY + totalHeight / 2 - lineHeight);
            for (int i = 0; i < lines.length; i++) {
                final double lineW = textRenderer.getBounds(lines[i]).getWidth();
                final int drawX = (int) (screenX - lineW / 2);
                final int drawY = (int) (startY - i * lineHeight);
                textRenderer.draw(lines[i], drawX, drawY);
            }
        });

        textRenderer.endRendering();
    }

    @Override
    public void dispose(final GLAutoDrawable drawable) {
        final GL3 gl = drawable.getGL().getGL3();
        gl.glDeleteBuffers(2, vboIds, 0);
        final int[] vaos = { vaoId };
        gl.glDeleteVertexArrays(1, vaos, 0);
        gl.glDeleteProgram(shaderProgram);
        if (textRenderer != null) textRenderer.dispose();
    }

    // --- Shader helpers ---
    private int createShaderProgram(final GL3 gl) {
        final String vertexSource   = loadShaderSource(VERTEX_SHADER_FILE);
        final String fragmentSource = loadShaderSource(FRAGMENT_SHADER_FILE);
        // geometry shader removed temporarily

        final int vertexShader = gl.glCreateShader(GL3.GL_VERTEX_SHADER);
        gl.glShaderSource(vertexShader, 1, new String[]{ vertexSource }, null, 0);
        gl.glCompileShader(vertexShader);
        checkShaderCompile(gl, vertexShader, "VERTEX");

        final int fragmentShader = gl.glCreateShader(GL3.GL_FRAGMENT_SHADER);
        gl.glShaderSource(fragmentShader, 1, new String[]{ fragmentSource }, null, 0);
        gl.glCompileShader(fragmentShader);
        checkShaderCompile(gl, fragmentShader,"FRAGMENT");

        final int program = gl.glCreateProgram();
        gl.glAttachShader(program, vertexShader);
        gl.glAttachShader(program, fragmentShader);
        gl.glLinkProgram(program);
        gl.glValidateProgram(program);

        gl.glDeleteShader(vertexShader);
        gl.glDeleteShader(fragmentShader);

        return program;
    }

    private void checkShaderCompile(final GL3 gl, final int shader, final String type) {
        final int[] status = new int[1];
        gl.glGetShaderiv(shader, GL3.GL_COMPILE_STATUS, status, 0);
        if (status[0] == GL3.GL_FALSE) {
            final byte[] log = new byte[SHADER_LOG_BUFFER_SIZE];
            gl.glGetShaderInfoLog(shader, SHADER_LOG_BUFFER_SIZE, null, 0, log, 0);
            throw new RuntimeException(type + " SHADER COMPILATION FAILED:\n" + new String(log));
        }
    }

    private static String loadShaderSource(final String filename) {
        try (final InputStream is = HanselChainRenderer.class.getResourceAsStream(filename)) {
            if (is == null)
                throw new RuntimeException("Shader not found: " + filename);
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (final Exception e) {
            throw new RuntimeException("Failed to load shader: " + filename, e);
        }
    }

    // --- Helpers ---
    private boolean isLowUnit(final Node node) {
        final Map<Integer, Set<Node>> lowUnits = interview.adjustedLowUnitsByClass;
        if (lowUnits == null)
            return false;
        final Set<Node> lowUnitsOfThisClassification = lowUnits.get(node.classification);
        if (lowUnitsOfThisClassification == null)
            return false;
        return lowUnitsOfThisClassification.contains(node);
    }
}