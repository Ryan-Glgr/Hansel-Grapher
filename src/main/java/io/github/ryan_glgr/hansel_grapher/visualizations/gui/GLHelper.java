package io.github.ryan_glgr.hansel_grapher.visualizations.gui;

import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;
import io.github.ryan_glgr.hansel_grapher.visualizations.gui.renderers.hanselchain.HanselChainRenderer;

import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;

public class GLHelper {
    private static final float[] CLEAR_COLOR = {0.15f, 0.15f, 0.15f, 1.0f};
    private static final String VERTEX_SHADER_FILE   = "VertexShader.glsl";
    private static final String FRAGMENT_SHADER_FILE = "FragmentShader.glsl";
    public static final int SHADER_LOG_BUFFER_SIZE = 1024;

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

    public static void checkShaderCompile(final GL3 gl, final int shader, final String type) {
        final int[] status = new int[1];
        gl.glGetShaderiv(shader, GL3.GL_COMPILE_STATUS, status, 0);
        if (status[0] == GL3.GL_FALSE) {
            final byte[] log = new byte[SHADER_LOG_BUFFER_SIZE];
            gl.glGetShaderInfoLog(shader, SHADER_LOG_BUFFER_SIZE, null, 0, log, 0);
            throw new RuntimeException(type + " SHADER COMPILATION FAILED:\n" + new String(log));
        }
    }

    // --- Shader helpers ---
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
