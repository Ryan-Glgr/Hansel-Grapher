package io.github.ryan_glgr.hansel_grapher.visualizations.gui.renderers;

import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL2;

public class ExpansionRenderer implements GLEventListener {
    @Override
    public void init(final GLAutoDrawable drawable) {
        // Called once when the GL context is created
        // Set up shaders, load textures, initialize buffers here
        final GL2 gl = drawable.getGL().getGL2();
        gl.glClearColor(0.15f, 0.15f, 0.15f, 1.0f);
    }

    @Override
    public void display(final GLAutoDrawable drawable) {
        // Called every frame — this is your render loop
        final GL2 gl = drawable.getGL().getGL2();
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT);
        // Draw your visualization here
    }

    @Override
    public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {
        // Called when the panel is resized
        // Update your projection matrix here
        final GL2 gl = drawable.getGL().getGL2();
        gl.glViewport(0, 0, width, height);
    }

    @Override
    public void dispose(final GLAutoDrawable drawable) {
        // Called when GL context is destroyed
        // Clean up GPU resources (buffers, textures, shaders) here
    }
}
