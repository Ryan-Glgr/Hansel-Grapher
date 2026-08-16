package io.github.ryan_glgr.hansel_grapher.visualizations.gui.renderers.hanselchain;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.util.awt.TextRenderer;
import io.github.ryan_glgr.hansel_grapher.functionallogic.Interview.Interview;
import io.github.ryan_glgr.hansel_grapher.functionallogic.Interview.LiveInterviewVisualizer;
import io.github.ryan_glgr.hansel_grapher.functionallogic.Node;
import io.github.ryan_glgr.hansel_grapher.functionallogic.lowunits.LowUnit;
import io.github.ryan_glgr.hansel_grapher.visualizations.gui.GUIHelper;
import io.github.ryan_glgr.hansel_grapher.visualizations.gui.renderers.PanZoomRenderer;

import java.awt.*;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class HanselChainRenderer extends PanZoomRenderer implements LiveInterviewVisualizer {

    // --- Layout constants
    private static final float NODE_WIDTH = 4.0f;
    private static final float NODE_HEIGHT = 2.0f;
    private static final float SIDE_SPACING = NODE_WIDTH / 10f;
    private static final float VERTICAL_SPACING = NODE_HEIGHT / 6f;
    private static final float MARGIN = SIDE_SPACING + VERTICAL_SPACING;
    private static final int FONT_SIZE = 14;
    private static final float TEXT_PADDING_INSIDE_NODE = 1.5f;// add near the other layout constants
    private static final float ROW_STEP = NODE_HEIGHT + VERTICAL_SPACING;
    private static final float COL_STEP = NODE_WIDTH + SIDE_SPACING;
    private static final float BORDER_THICKNESS_FRACTION = 0.15f; // inset of the fill quad within the border quad

    private static final int POSITION_COMPONENTS = 2;   // x, y
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

    private final int numClasses;

    // Assigned once on the GL thread in init() and never mutated afterwards.
    // All inter-thread communication goes through colorsDirty.
    private final ArrayList<ArrayList<Node>> chains;
    private final int totalNodes;
    private final Map<Node, LowUnit> lowUnitNodes;
    private final int numExclusiveLowUnits;
    private final int longestChainHeight;              // tallest chain, used to size the padded grid


    // Per-node layout: maps Node -> [centerX, centerY]
    private Node[][] nodeGrid;        // [chainIndex][rowIndex], null = empty cell
    private String[][][] labelGrid;   // [chainIndex][rowIndex] -> label lines, null = empty
    private float[] columnX;          // columnX[c] = center X of chain c
    private float baseRowY;           // world Y of row 0 (bottom row), global to all chains

    public HanselChainRenderer(final Interview interview, final int classificationColorShuffleCounter) {
        super(classificationColorShuffleCounter);
        this.numClasses = interview.numClasses;

        chains = GUIHelper.sortChainsForVisualization(interview.hanselChains);
        totalNodes = chains.stream().mapToInt(List::size).sum();

        // reverse map all the low unit nodes to their "low unit"
        lowUnitNodes = interview.lowUnitsByClass.values()
                .stream()
                .flatMap(Set::stream)
                .collect(Collectors.toMap(LowUnit::getDatapoint, Function.identity()));

        numExclusiveLowUnits = (int) lowUnitNodes.values().stream()
                .filter(lowUnit -> LowUnit.Type.EXCLUSIVE.equals(lowUnit.getLowUnitType()))
                .count();

        longestChainHeight = chains.stream().mapToInt(List::size).max().orElseThrow();
    }

    // Called from the compute thread whenever node classifications change.
    // to be used in the interview each time we have udpated. will provide an interface against which this works.
    @Override
    public void notifyClassificationsChanged() {
        colorsDirty = true;
    }

    // --- Layout ---

    private void computeLayout() {
        final int numChains = chains.size();

        nodeGrid  = new Node[numChains][longestChainHeight];
        labelGrid = new String[numChains][longestChainHeight][];
        columnX   = new float[numChains];

        final float totalHeightMax = longestChainHeight * NODE_HEIGHT + (longestChainHeight - 1) * VERTICAL_SPACING;
        baseRowY = -(totalHeightMax / 2.0f) + NODE_HEIGHT / 2.0f;

        float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
        float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;

        for (int c = 0; c < numChains; c++) {
            final ArrayList<Node> chain = chains.get(c);
            columnX[c] = c * COL_STEP;

            final int startRow = (longestChainHeight - chain.size()) / 2;  // centers within the padded grid
            for (int i = 0; i < chain.size(); i++) {
                final int row = startRow + i;
                final Node node = chain.get(i);

                nodeGrid[c][row]  = node;
                labelGrid[c][row] = GUIHelper.nodeLabelArray(node, isLowUnit(node));

                final float cx = columnX[c];
                final float cy = baseRowY + row * ROW_STEP;

                minX = Math.min(minX, cx - NODE_WIDTH  / 2f);
                maxX = Math.max(maxX, cx + NODE_WIDTH  / 2f);
                minY = Math.min(minY, cy - NODE_HEIGHT / 2f);
                maxY = Math.max(maxY, cy + NODE_HEIGHT / 2f);
            }
        }

        worldBounds = new float[]{ minX - MARGIN, maxX + MARGIN, minY - MARGIN, maxY + MARGIN };
    }

    @Override
    protected float[] getWorldBounds() {
        return worldBounds;
    }

    // --- Buffer builders ---

    private FloatBuffer buildPositionBuffer() {
        final int extraSizeForExclusiveLowUnits = numExclusiveLowUnits * VERTICES_PER_NODE * POSITION_COMPONENTS;
        final int regularSize = totalNodes * VERTICES_PER_NODE * POSITION_COMPONENTS;
        final FloatBuffer buffer = Buffers.newDirectFloatBuffer(regularSize + extraSizeForExclusiveLowUnits);

        final float insetY = NODE_HEIGHT * BORDER_THICKNESS_FRACTION;

        for (int c = 0; c < nodeGrid.length; c++) {
            final float cx = columnX[c];
            final float l = cx - NODE_WIDTH / 2f;
            final float r = cx + NODE_WIDTH / 2f;

            for (int row = 0; row < longestChainHeight; row++) {
                final Node node = nodeGrid[c][row];
                if (node == null) continue;

                final float cy = baseRowY + row * ROW_STEP;
                final float b = cy - NODE_HEIGHT / 2f;
                final float t = cy + NODE_HEIGHT / 2f;

                if (LowUnit.Type.EXCLUSIVE.equals(isLowUnit(node))) {
                    emitQuad(buffer, l, b, r, t);                                   // border: full node size
                    emitQuad(buffer, l, b + insetY, r, t - insetY); // fill: inset
                } else {
                    emitQuad(buffer, l, b, r, t);
                }
            }
        }

        buffer.flip();
        return buffer;
    }

    private static void emitQuad(final FloatBuffer buffer, final float l, final float b, final float r, final float t) {
        buffer.put(l); buffer.put(b);
        buffer.put(r); buffer.put(b);
        buffer.put(r); buffer.put(t);
        buffer.put(l); buffer.put(b);
        buffer.put(r); buffer.put(t);
        buffer.put(l); buffer.put(t);
    }

    // [r, g, b, a] per node — rebuilt whenever classifications change.
    private FloatBuffer buildColorBuffer() {

        final int extraBufferSizeForExclusiveLowUnits = numExclusiveLowUnits * VERTICES_PER_NODE * COLOR_COMPONENTS;
        final int regularBufferSize = totalNodes * VERTICES_PER_NODE * COLOR_COMPONENTS;
        final FloatBuffer buffer = Buffers.newDirectFloatBuffer(regularBufferSize + extraBufferSizeForExclusiveLowUnits);

        for (final Node[] chain : nodeGrid) {
            for (final Node node : chain) {
                if (node == null)
                    continue;

                final LowUnit.Type lowUnitType = isLowUnit(node);
                final int nodeClassWithColorShuffle = node.classification.equals(Node.IMPOSSIBLE_CLASSIFICATION)
                        ? Node.IMPOSSIBLE_CLASSIFICATION
                        : (node.classification + classificationColorShuffleCounter) % numClasses;

                // exclusive low units get colors drawn twice, once for their border which will be next class, and once for their own class.
                final boolean isExclusiveLowUnit = LowUnit.Type.EXCLUSIVE.equals(lowUnitType);
                if (isExclusiveLowUnit) {
                    // no need to consider whether a node is itself an EXCLUSIVE LOW UNIT of IMPOSSIBLE classification,
                    // since that is not possible. there is no higher class it could be.
                    final int exclusiveNodeTargetClass = (numClasses == (nodeClassWithColorShuffle + nodeClassWithColorShuffle + 1))
                            ? Node.IMPOSSIBLE_CLASSIFICATION
                            : (node.classification + classificationColorShuffleCounter + 1) % numClasses;
                    populateColorBuffer(exclusiveNodeTargetClass, true, buffer);
                }
                // now draw the colors for the node itself. If it is exclusive, we do not want to color it AGAIN with low unit brightness, since it looks strange.
                final boolean colorAsALowUnit = Objects.nonNull(lowUnitType) && !isExclusiveLowUnit;
                populateColorBuffer(nodeClassWithColorShuffle, colorAsALowUnit, buffer);
            }
        }

        buffer.flip();
        return buffer;
    }

    private void populateColorBuffer(final int classification, final boolean isLowUnit, final FloatBuffer buffer) {
        final Color color = GUIHelper.getColorForClass(classification, isLowUnit);
        final float rC = color.getRed() / 255f;
        final float gC = color.getGreen() / 255f;
        final float bC = color.getBlue() / 255f;
        final float aC = color.getAlpha() / 255f;
        for (int i = 0; i < VERTICES_PER_NODE; i++) {
            buffer.put(rC);
            buffer.put(gC);
            buffer.put(bC);
            buffer.put(aC);
        }
    }

    // --- GL event listener ---

    @Override
    public void init(final GLAutoDrawable drawable) {
        final GL3 gl = getGl3(drawable);

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
        this.vaoId = vaos[0];
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

        this.textRenderer = new TextRenderer(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, FONT_SIZE));

        // ------------------------------------------------------------
        // 7. Upload initial projection so first display() is correct
        // ------------------------------------------------------------
        reshape(drawable, 0, 0, drawable.getSurfaceWidth(), drawable.getSurfaceHeight());
        super.init(drawable);   // registers mouse listeners
    }

    private static GL3 getGl3(final GLAutoDrawable drawable) {
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
        return gl;
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
        gl.glDrawArrays(GL3.GL_TRIANGLES, 0, (totalNodes + numExclusiveLowUnits) * VERTICES_PER_NODE);
        gl.glBindVertexArray(0);
        gl.glUseProgram(0);
        drawLabels();
    }

    private void drawLabels() {
        final float viewWidth = getLiveRight() - getLiveLeft();
        final float viewHeight = getLiveTop()   - getLiveBottom();
        final float scaleX = surfaceWidth  / viewWidth;
        final float scaleY = surfaceHeight / viewHeight;

        final float nodeWidthPx  = NODE_WIDTH  * scaleX;
        final float nodeHeightPx = NODE_HEIGHT * scaleY;

        // find any non-null label to measure against (same role as old "sampleNodeLabel")
        final String[] sampleLabel = Arrays.stream(labelGrid)
                .flatMap(Arrays::stream)
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No node labels exist"));


        final double lineHeight  = textRenderer.getBounds("Ag").getHeight();
        final double totalHeight = lineHeight * sampleLabel.length;
        final double longestLine = Arrays.stream(sampleLabel)
                .mapToDouble(line -> textRenderer.getBounds(line).getWidth())
                .max().orElseThrow();

        if (longestLine > nodeWidthPx - TEXT_PADDING_INSIDE_NODE) return;
        if (totalHeight  > nodeHeightPx - TEXT_PADDING_INSIDE_NODE) return;

        // --- arithmetic culling: invert screen bounds -> grid index ranges ---
        final int colMin = clamp((int) Math.floor((getLiveLeft()   - NODE_WIDTH / 2f) / COL_STEP), 0, columnX.length - 1);
        final int colMax = clamp((int) Math.ceil ((getLiveRight()  + NODE_WIDTH / 2f) / COL_STEP), 0, columnX.length - 1);
        final int rowMin = clamp((int) Math.floor((getLiveBottom() - NODE_HEIGHT / 2f - baseRowY) / ROW_STEP), 0, longestChainHeight - 1);
        final int rowMax = clamp((int) Math.ceil ((getLiveTop()    + NODE_HEIGHT / 2f - baseRowY) / ROW_STEP), 0, longestChainHeight - 1);

        textRenderer.beginRendering(surfaceWidth, surfaceHeight);
        textRenderer.setColor(0f, 0f, 0f, 1f);

        for (int c = colMin; c <= colMax; c++) {
            final float worldX = columnX[c];
            final float screenX = (worldX - getLiveLeft()) / viewWidth * surfaceWidth;

            for (int r = rowMin; r <= rowMax; r++) {
                final String[] lines = labelGrid[c][r];
                if (lines == null) continue;

                final float worldY = baseRowY + r * ROW_STEP;
                final float screenY = (worldY - getLiveBottom()) / viewHeight * surfaceHeight;

                final float startY = (float) (screenY + totalHeight / 2 - lineHeight);
                for (int i = 0; i < lines.length; i++) {
                    final double lineW = textRenderer.getBounds(lines[i]).getWidth();
                    final int drawX = (int) (screenX - lineW / 2);
                    final int drawY = (int) (startY - i * lineHeight);
                    textRenderer.draw(lines[i], drawX, drawY);
                }
            }
        }

        textRenderer.endRendering();
    }

    private static int clamp(final int v, final int lo, final int hi) {
        return Math.max(lo, Math.min(hi, v));
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
    private LowUnit.Type isLowUnit(final Node node) {

        if (lowUnitNodes == null)
            return null;

        final LowUnit lowUnit = lowUnitNodes.get(node);
        return lowUnit == null
                ? null
                : lowUnit.getLowUnitType();
    }
}