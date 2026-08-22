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
import io.github.ryan_glgr.hansel_grapher.visualizations.layout.HanselChainLayout;

import java.awt.*;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static io.github.ryan_glgr.hansel_grapher.visualizations.layout.HanselChainLayout.*;

public class HanselChainRenderer extends PanZoomRenderer implements LiveInterviewVisualizer {

    // --- Layout constants pulled from HanselChainLayout via static import: NODE_WIDTH, NODE_HEIGHT,
    // SIDE_SPACING, VERTICAL_SPACING, MARGIN, ROW_STEP, COL_STEP

    private static final int FONT_SIZE = 14;
    private static final float TEXT_PADDING_INSIDE_NODE = 1.5f;
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
    private volatile boolean colorsDirty;
    private TextRenderer textRenderer;

    private final int numClasses;
    private final HanselChainLayout layout;

    public HanselChainRenderer(final Interview interview, final int classificationColorShuffleCounter) {
        super(classificationColorShuffleCounter);
        this.numClasses = interview.numClasses;
        this.layout = new HanselChainLayout(interview.hanselChains, interview.lowUnitsByClass);
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

    // --- Buffer builders ---

    private FloatBuffer buildPositionBuffer() {
        final Node[][] nodeGrid = layout.getNodeGrid();
        final int extraSizeForExclusiveLowUnits = layout.getNumExclusiveLowUnits() * VERTICES_PER_NODE * POSITION_COMPONENTS;
        final int regularSize = layout.getTotalNodes() * VERTICES_PER_NODE * POSITION_COMPONENTS;
        final FloatBuffer buffer = Buffers.newDirectFloatBuffer(regularSize + extraSizeForExclusiveLowUnits);

        final float insetY = NODE_HEIGHT * BORDER_THICKNESS_FRACTION;

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
                    emitQuad(buffer, l, b, r, t);                       // border: full node size
                    emitQuad(buffer, l, b + insetY, r, t - insetY);     // fill: inset
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
        final Node[][] nodeGrid = layout.getNodeGrid();
        final int extraBufferSizeForExclusiveLowUnits = layout.getNumExclusiveLowUnits() * VERTICES_PER_NODE * COLOR_COMPONENTS;
        final int regularBufferSize = layout.getTotalNodes() * VERTICES_PER_NODE * COLOR_COMPONENTS;
        final FloatBuffer buffer = Buffers.newDirectFloatBuffer(regularBufferSize + extraBufferSizeForExclusiveLowUnits);

        for (final Node[] chain : nodeGrid) {
            for (final Node node : chain) {
                if (node == null)
                    continue;

                final LowUnit.Type lowUnitType = layout.getLowUnitType(node);
                final int nodeClassWithColorShuffle = node.classification.equals(Node.IMPOSSIBLE_CLASSIFICATION)
                        ? Node.IMPOSSIBLE_CLASSIFICATION
                        : (node.classification + classificationColorShuffleCounter) % numClasses;

                final boolean isExclusiveLowUnit = LowUnit.Type.EXCLUSIVE.equals(lowUnitType);
                if (isExclusiveLowUnit) {
                    final int exclusiveNodeTargetClass = node.classification + 1 == numClasses
                            ? Node.IMPOSSIBLE_CLASSIFICATION
                            : (node.classification + classificationColorShuffleCounter + 1) % numClasses;
                    populateColorBuffer(exclusiveNodeTargetClass, true, buffer);
                }
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

        shaderProgram = createShaderProgram(gl);
        projectionUniformLocation = gl.glGetUniformLocation(shaderProgram, "uProjection");
        if (projectionUniformLocation == -1)
            throw new RuntimeException("Uniform 'uProjection' not found in shader program.");

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
        if (vaoId == 0)
            throw new RuntimeException("VAO creation failed");

        gl.glBindVertexArray(vaoId);

        gl.glGenBuffers(2, vboIds, 0);

        gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, vboIds[VBO_POSITIONS]);
        final FloatBuffer positionBuffer = buildPositionBuffer();
        gl.glBufferData(GL3.GL_ARRAY_BUFFER, (long) positionBuffer.capacity() * Float.BYTES, positionBuffer, GL3.GL_STATIC_DRAW);
        gl.glEnableVertexAttribArray(0);
        gl.glVertexAttribPointer(0, POSITION_COMPONENTS, GL3.GL_FLOAT, false, 0, 0);

        gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, vboIds[VBO_COLORS]);
        final FloatBuffer colorBuffer = buildColorBuffer();
        gl.glBufferData(GL3.GL_ARRAY_BUFFER, (long) colorBuffer.capacity() * Float.BYTES, colorBuffer, GL3.GL_DYNAMIC_DRAW);
        gl.glEnableVertexAttribArray(1);
        gl.glVertexAttribPointer(1, COLOR_COMPONENTS, GL3.GL_FLOAT, false, 0, 0);

        gl.glBindVertexArray(0);
        gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, 0);

        this.textRenderer = new TextRenderer(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, FONT_SIZE));

        reshape(drawable, 0, 0, drawable.getSurfaceWidth(), drawable.getSurfaceHeight());
        super.init(drawable);   // registers mouse listeners
    }

    private static GL3 getGl3(final GLAutoDrawable drawable) {
        final GL3 gl = drawable.getGL().getGL3();
        if (gl == null)
            throw new RuntimeException("GL3 context not available — check GLProfile at canvas creation.");

        gl.glClearColor(CLEAR_COLOR[0], CLEAR_COLOR[1], CLEAR_COLOR[2], CLEAR_COLOR[3]);
        gl.glEnable(GL3.GL_BLEND);
        gl.glEnable(GL3.GL_PROGRAM_POINT_SIZE);
        gl.glBlendFunc(GL3.GL_SRC_ALPHA, GL3.GL_ONE_MINUS_SRC_ALPHA);
        return gl;
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
        gl.glDrawArrays(GL3.GL_TRIANGLES, 0, (layout.getTotalNodes() + layout.getNumExclusiveLowUnits()) * VERTICES_PER_NODE);
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

        final String[][][] labelGrid = layout.getLabelGrid();
        final float[] columnX = layout.getColumnX();
        final int longestChainHeight = layout.getLongestChainHeight();

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

        final int colMin = clamp((int) Math.floor((getLiveLeft()   - NODE_WIDTH / 2f) / COL_STEP), 0, columnX.length - 1);
        final int colMax = clamp((int) Math.ceil ((getLiveRight()  + NODE_WIDTH / 2f) / COL_STEP), 0, columnX.length - 1);
        final int rowMin = clamp((int) Math.floor((getLiveBottom() - NODE_HEIGHT / 2f - layout.getBaseRowY()) / ROW_STEP), 0, longestChainHeight - 1);
        final int rowMax = clamp((int) Math.ceil ((getLiveTop()    + NODE_HEIGHT / 2f - layout.getBaseRowY()) / ROW_STEP), 0, longestChainHeight - 1);

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
}