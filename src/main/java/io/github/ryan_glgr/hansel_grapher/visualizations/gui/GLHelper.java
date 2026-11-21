package io.github.ryan_glgr.hansel_grapher.visualizations.gui;

import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.common.nio.Buffers;
import io.github.ryan_glgr.hansel_grapher.functionallogic.Node;
import io.github.ryan_glgr.hansel_grapher.functionallogic.lowunits.LowUnit;
import io.github.ryan_glgr.hansel_grapher.visualizations.gui.renderers.hanselchain.HanselChainRenderer;

import java.awt.Color;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class GLHelper {
    private static final float[] CLEAR_COLOR = {0.15f, 0.15f, 0.15f, 1.0f};
    private static final String VERTEX_SHADER_FILE   = "VertexShader.glsl";
    private static final String FRAGMENT_SHADER_FILE = "FragmentShader.glsl";
    public static final int SHADER_LOG_BUFFER_SIZE = 1024;

    // --- Node rendering constants shared by the node-based renderers ---
    public static final int POSITION_COMPONENTS = 2;   // x, y
    public static final int COLOR_COMPONENTS = 4;      // r, g, b, a
    public static final int VERTICES_PER_QUAD = 6;
    public static final int VERTICES_PER_TRIANGLE = 3;

    public static final int VBO_POSITIONS = 0;         // vboIds[0] -> positions
    public static final int VBO_COLORS = 1;            // vboIds[1] -> colors

    // Four quads form the hover border.
    public static final int HOVER_BORDER_QUADS = 4;
    public static final int HOVER_BORDER_VERTICES = HOVER_BORDER_QUADS * VERTICES_PER_QUAD;
    public static final float HOVER_BORDER_THICKNESS = 0.2f;
    public static final Color HOVER_BORDER_COLOR = new Color(220, 70, 220);

    public static GL3 getGl3(final GLAutoDrawable drawable) {
        final GL3 gl = drawable.getGL().getGL3();
        if (gl == null)
            throw new RuntimeException("GL3 context not available — check GLProfile at canvas creation.");

        gl.glClearColor(CLEAR_COLOR[0], CLEAR_COLOR[1], CLEAR_COLOR[2], CLEAR_COLOR[3]);
        gl.glEnable(GL3.GL_BLEND);
        gl.glEnable(GL3.GL_PROGRAM_POINT_SIZE);
        gl.glBlendFunc(GL3.GL_SRC_ALPHA, GL3.GL_ONE_MINUS_SRC_ALPHA);
        return gl;
    }

    public static void emitTriangle(
            final FloatBuffer buffer,
            final float x1, final float y1,
            final float x2, final float y2,
            final float x3, final float y3) {

        buffer.put(x1); buffer.put(y1);
        buffer.put(x2); buffer.put(y2);
        buffer.put(x3); buffer.put(y3);
    }

    public static void emitQuad(final FloatBuffer buffer, final float l, final float b, final float r, final float t) {
        buffer.put(l); buffer.put(b);
        buffer.put(r); buffer.put(b);
        buffer.put(r); buffer.put(t);
        buffer.put(l); buffer.put(b);
        buffer.put(r); buffer.put(t);
        buffer.put(l); buffer.put(t);
    }

    // -----------------------------------------------------------------------
    // Node geometry
    // -----------------------------------------------------------------------

    /** Total vertices used by the nodes (not counting the hover border). */
    public static int nodeVertexCount(final int totalNodes, final int exclusiveLowUnits) {
        return totalNodes * VERTICES_PER_QUAD + exclusiveLowUnits * VERTICES_PER_TRIANGLE;
    }

    /** Float count for a position buffer holding the nodes plus the reserved hover border. */
    public static int positionBufferFloats(final int totalNodes, final int exclusiveLowUnits) {
        return (nodeVertexCount(totalNodes, exclusiveLowUnits) + HOVER_BORDER_VERTICES) * POSITION_COMPONENTS;
    }

    /** Float count for a color buffer holding the nodes plus the reserved hover border. */
    public static int colorBufferFloats(final int totalNodes, final int exclusiveLowUnits) {
        return (nodeVertexCount(totalNodes, exclusiveLowUnits) + HOVER_BORDER_VERTICES) * COLOR_COMPONENTS;
    }

    /** Emits a node's quad, plus the triangle on top when it is an exclusive low unit. */
    public static void emitNode(final FloatBuffer buffer, final float cx, final float cy,
                                final float nodeWidth, final float nodeHeight, final boolean exclusiveLowUnit) {
        final float l = cx - nodeWidth / 2f;
        final float r = cx + nodeWidth / 2f;
        final float b = cy - nodeHeight / 2f;
        final float t = cy + nodeHeight / 2f;

        emitQuad(buffer, l, b, r, t);

        if (exclusiveLowUnit) {
            emitTriangle(buffer, l, t, r, t, (l + r) / 2f, t - 0.5f * nodeHeight);
        }
    }

    /** Reserves zeroed positions for the hover border; they are filled in on hover. */
    public static void reserveHoverBorderPositions(final FloatBuffer buffer) {
        for (int i = 0; i < HOVER_BORDER_VERTICES * POSITION_COMPONENTS; i++) {
            buffer.put(0f);
        }
    }

    // -----------------------------------------------------------------------
    // Node colors
    // -----------------------------------------------------------------------

    /**
     * Emits colors for a node's quad, plus its triangle when it is an exclusive low unit.
     * Must be called in the same order as {@link #emitNode}.
     */
    public static void emitNodeColors(final FloatBuffer buffer, final Node node, final LowUnit.Type lowUnitType,
                                      final int numClasses, final int classificationColorShuffleCounter) {

        final int nodeClassWithColorShuffle = node.classification.equals(Node.IMPOSSIBLE_CLASSIFICATION)
                ? Node.IMPOSSIBLE_CLASSIFICATION
                : (node.classification + classificationColorShuffleCounter) % numClasses;

        final boolean isExclusiveLowUnit = LowUnit.Type.EXCLUSIVE.equals(lowUnitType);
        final boolean colorAsALowUnit = Objects.nonNull(lowUnitType) && !isExclusiveLowUnit;

        populateColorBuffer(nodeClassWithColorShuffle, colorAsALowUnit, VERTICES_PER_QUAD, buffer);

        if (isExclusiveLowUnit) {
            final int exclusiveNodeTargetClass = node.classification + 1 == numClasses
                    ? Node.IMPOSSIBLE_CLASSIFICATION
                    : (node.classification + classificationColorShuffleCounter + 1) % numClasses;

            populateColorBuffer(exclusiveNodeTargetClass, true, VERTICES_PER_TRIANGLE, buffer);
        }
    }

    public static void populateColorBuffer(final int classification, final boolean isLowUnit,
                                           final int vertexCount, final FloatBuffer buffer) {
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

    /** Reserves the magenta color vertices for the hover border. */
    public static void emitHoverBorderColors(final FloatBuffer buffer) {
        final float r = HOVER_BORDER_COLOR.getRed() / 255f;
        final float g = HOVER_BORDER_COLOR.getGreen() / 255f;
        final float b = HOVER_BORDER_COLOR.getBlue() / 255f;

        for (int i = 0; i < HOVER_BORDER_VERTICES; i++) {
            buffer.put(r);
            buffer.put(g);
            buffer.put(b);
            buffer.put(1f);
        }
    }

    // -----------------------------------------------------------------------
    // Buffer objects and hover border
    // -----------------------------------------------------------------------

    /**
     * Creates the VAO and the position/color VBOs. Positions are attribute 0, colors attribute 1.
     * Fills vboIds and returns the VAO id.
     */
    public static int createNodeVao(final GL3 gl, final int[] vboIds,
                                    final FloatBuffer positionBuffer, final FloatBuffer colorBuffer) {
        final int[] vaos = new int[1];
        gl.glGenVertexArrays(1, vaos, 0);

        final int vaoId = vaos[0];
        if (vaoId == 0) {
            throw new RuntimeException("VAO creation failed");
        }

        gl.glBindVertexArray(vaoId);
        gl.glGenBuffers(2, vboIds, 0);

        gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, vboIds[VBO_POSITIONS]);
        gl.glBufferData(GL3.GL_ARRAY_BUFFER, (long) positionBuffer.capacity() * Float.BYTES, positionBuffer, GL3.GL_STATIC_DRAW);
        gl.glEnableVertexAttribArray(0);
        gl.glVertexAttribPointer(0, POSITION_COMPONENTS, GL3.GL_FLOAT, false, 0, 0);

        gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, vboIds[VBO_COLORS]);
        gl.glBufferData(GL3.GL_ARRAY_BUFFER, (long) colorBuffer.capacity() * Float.BYTES, colorBuffer, GL3.GL_DYNAMIC_DRAW);
        gl.glEnableVertexAttribArray(1);
        gl.glVertexAttribPointer(1, COLOR_COMPONENTS, GL3.GL_FLOAT, false, 0, 0);

        gl.glBindVertexArray(0);
        gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, 0);

        return vaoId;
    }

    /** Replaces the whole color VBO, e.g. after classifications change. */
    public static void uploadColors(final GL3 gl, final int[] vboIds, final FloatBuffer colorBuffer) {
        gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, vboIds[VBO_COLORS]);
        gl.glBufferSubData(GL3.GL_ARRAY_BUFFER, 0, (long) colorBuffer.capacity() * Float.BYTES, colorBuffer);
        gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, 0);
    }

    /** Writes the hover border for the node centered at (cx, cy) into the reserved slot of the position VBO. */
    public static void uploadHoverBorder(final GL3 gl, final int[] vboIds, final int nodeVertexCount,
                                         final float cx, final float cy,
                                         final float nodeWidth, final float nodeHeight) {
        final FloatBuffer buffer = Buffers.newDirectFloatBuffer(HOVER_BORDER_VERTICES * POSITION_COMPONENTS);

        final float left = cx - nodeWidth / 2f;
        final float right = cx + nodeWidth / 2f;
        final float bottom = cy - nodeHeight / 2f;
        final float top = cy + nodeHeight / 2f;

        final float thickness = Math.min(nodeWidth, nodeHeight) * HOVER_BORDER_THICKNESS;

        emitQuad(buffer, left, top - thickness, right, top);                           // top
        emitQuad(buffer, left, bottom, right, bottom + thickness);                     // bottom
        emitQuad(buffer, left, bottom + thickness, left + thickness, top - thickness); // left
        emitQuad(buffer, right - thickness, bottom + thickness, right, top - thickness); // right

        buffer.flip();

        final long offset = (long) nodeVertexCount * POSITION_COMPONENTS * Float.BYTES;

        gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, vboIds[VBO_POSITIONS]);
        gl.glBufferSubData(GL3.GL_ARRAY_BUFFER, offset, (long) buffer.remaining() * Float.BYTES, buffer);
        gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, 0);
    }

    /** Frees the VAO and VBOs created by {@link #createNodeVao}, plus the shader program. */
    public static void disposeNodeResources(final GL3 gl, final int vaoId, final int[] vboIds, final int shaderProgram) {
        gl.glDeleteBuffers(2, vboIds, 0);
        gl.glDeleteVertexArrays(1, new int[]{vaoId}, 0);
        gl.glDeleteProgram(shaderProgram);
    }

    // -----------------------------------------------------------------------
    // Shaders
    // -----------------------------------------------------------------------

    public static void checkShaderCompile(final GL3 gl, final int shader, final String type) {
        final int[] status = new int[1];
        gl.glGetShaderiv(shader, GL3.GL_COMPILE_STATUS, status, 0);
        if (status[0] == GL3.GL_FALSE) {
            final byte[] log = new byte[SHADER_LOG_BUFFER_SIZE];
            gl.glGetShaderInfoLog(shader, SHADER_LOG_BUFFER_SIZE, null, 0, log, 0);
            throw new RuntimeException(type + " SHADER COMPILATION FAILED:\n" + new String(log));
        }
    }

    public static int createShaderProgram(final GL3 gl) {
        final String vertexSource = loadShaderSource(VERTEX_SHADER_FILE);
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