package io.github.ryan_glgr.hansel_grapher.visualizations.gui.renderers;

import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.util.Animator;

import java.awt.event.*;

/**
 * Abstract base for all renderers that support mouse pan and scroll-wheel zoom.
 * Subclasses must implement:
 *   - init, display, dispose  (standard GLEventListener)
 *   - getWorldBounds()        → float[4] { minX, maxX, minY, maxY } in world coords
 * reshape() is handled here; subclasses should call super.reshape() if they override it.
 * The computed projection is exposed via getPendingProjection() / consumePendingProjection().
 */
public abstract class PanZoomRenderer
        implements GLEventListener, MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {

    private static final float PAN_AMOUNT = 0.10f;

    // --- Pan / zoom state (all on the EDT) ---
    private float panX    = 0f;
    private float panY    = 0f;
    private float zoom    = 1f;

    private int lastMouseX;
    private int lastMouseY;

    // track what's currently on the GPU being shown at this moment.
    private float liveLeft, liveRight, liveBottom, liveTop;

    // Pending projection matrix (set on reshape/mouse event, consumed in display)
    private volatile float[] pendingProjection = null;


    // Last known surface size, so mouse events can rebuild the projection
    protected int surfaceWidth  = 1;
    protected int surfaceHeight = 1;

    // -----------------------------------------------------------------------
    // Subclass contract
    // -----------------------------------------------------------------------

    /**
     * Return the natural bounding box of your content in world coordinates,
     * with a margin already applied if desired.
     * { minX, maxX, minY, maxY }
     */
    protected abstract float[] getWorldBounds();

    // -----------------------------------------------------------------------
    // Projection helpers (used by subclasses in display())
    // -----------------------------------------------------------------------

    /** True if a new projection matrix is waiting to be uploaded. */
    protected boolean hasNewProjection() {
        return pendingProjection != null;
    }

    /**
     * Returns the pending projection and clears it.
     * Returns null if nothing new to upload.
     */
    protected float[] consumePendingProjection() {
        final float[] p = pendingProjection;
        pendingProjection = null;
        return p;
    }

    // -----------------------------------------------------------------------
    // Pan / zoom reset (call from subclass if you add a "reset view" button)
    // -----------------------------------------------------------------------

    protected void resetView() {
        panX = 0f;
        panY = 0f;
        zoom = 1f;
        rebuildProjection();
    }

    // -----------------------------------------------------------------------
    // reshape — subclasses should call super.reshape() if they override
    // -----------------------------------------------------------------------

    @Override
    public void reshape(final GLAutoDrawable drawable,
                        final int x, final int y,
                        final int width, final int height) {
        final GL3 gl = drawable.getGL().getGL3();
        gl.glViewport(0, 0, width, height);
        surfaceWidth  = width;
        surfaceHeight = height;
        rebuildProjection();
    }

    // -----------------------------------------------------------------------
    // Core projection computation
    // -----------------------------------------------------------------------

    private void rebuildProjection() {
        final float[] b = getWorldBounds();     // { minX, maxX, minY, maxY }
        float minX = b[0], maxX = b[1];
        float minY = b[2], maxY = b[3];

        // Fit content to aspect ratio first
        final float aspect = (float) surfaceWidth / Math.max(surfaceHeight, 1);
        final float graphW = maxX - minX;
        final float graphH = maxY - minY;

        if (graphW / graphH > aspect) {
            final float cy   = (minY + maxY) / 2f;
            final float half = (graphW / aspect) / 2f;
            minY = cy - half;
            maxY = cy + half;
        } else {
            final float cx   = (minX + maxX) / 2f;
            final float half = (graphH * aspect) / 2f;
            minX = cx - half;
            maxX = cx + half;
        }

        // Apply zoom (scale around center)
        final float cx = (minX + maxX) / 2f;
        final float cy = (minY + maxY) / 2f;
        final float hw = (maxX - minX) / 2f / zoom;
        final float hh = (maxY - minY) / 2f / zoom;

        // Apply pan (in world units)
        final float left   = cx - hw + panX;
        final float right  = cx + hw + panX;
        final float bottom = cy - hh + panY;
        final float top    = cy + hh + panY;

        // track our exact bounds. this helps in text rendering.
        liveLeft = left;
        liveRight = right;
        liveBottom = bottom;
        liveTop = top;

        pendingProjection = orthographicMatrix(left, right, bottom, top, -1f, 1f);
    }

    // -----------------------------------------------------------------------
    // Mouse listeners — pan
    // -----------------------------------------------------------------------

    @Override
    public void mousePressed(final MouseEvent e) {
        lastMouseX = e.getX();
        lastMouseY = e.getY();
    }

    @Override
    public void mouseDragged(final MouseEvent e) {
        final float[] b = getWorldBounds();
        final float worldW = (b[1] - b[0]) / zoom;
        final float pixelToWorld = worldW / Math.max(surfaceWidth, 1);

        final int dx = e.getX() - lastMouseX;
        final int dy = e.getY() - lastMouseY;

        panX -= dx * pixelToWorld;
        panY += dy * pixelToWorld;

        lastMouseX = e.getX();
        lastMouseY = e.getY();
        rebuildProjection();
    }

    // -----------------------------------------------------------------------
    // Mouse listener — zoom (scroll wheel)
    // -----------------------------------------------------------------------

    @Override
    public void mouseWheelMoved(final MouseWheelEvent e) {
        final float factor = e.getWheelRotation() < 0 ? 1.1f : 0.9f;

        // Convert mouse screen position to world coords BEFORE zooming
        final float[] b = getWorldBounds();
        final float worldW = (b[1] - b[0]) / zoom;
        final float worldH = (b[3] - b[2]) / zoom;
        final float mouseWorldX = (e.getX() / (float) surfaceWidth  - 0.5f) * worldW + panX;
        final float mouseWorldY = (0.5f - e.getY() / (float) surfaceHeight) * worldH + panY;

        zoom *= factor;

        // Recompute world size at new zoom and shift pan so mouse world point stays fixed
        final float newWorldW = (b[1] - b[0]) / zoom;
        final float newWorldH = (b[3] - b[2]) / zoom;
        panX = mouseWorldX - (e.getX() / (float) surfaceWidth  - 0.5f) * newWorldW;
        panY = mouseWorldY - (0.5f - e.getY() / (float) surfaceHeight) * newWorldH;

        rebuildProjection();
    }

    @Override
    public void keyPressed(final KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_LEFT  -> panX -= PAN_AMOUNT * zoom;
            case KeyEvent.VK_RIGHT -> panX += PAN_AMOUNT * zoom;
            case KeyEvent.VK_UP    -> panY += PAN_AMOUNT * zoom;
            case KeyEvent.VK_DOWN  -> panY -= PAN_AMOUNT * zoom;
        }
        rebuildProjection();
    }

    @Override
    public void init(final GLAutoDrawable drawable) {
        if (drawable instanceof final java.awt.Component comp) {
            comp.addMouseListener(this);
            comp.addMouseMotionListener(this);
            comp.addMouseWheelListener(this);
            comp.addKeyListener(this);
            comp.setFocusable(true);
            comp.requestFocusInWindow();
        } else {
            throw new RuntimeException("PanZoomRenderer requires an AWT-backed drawable.");
        }
    }


    // -----------------------------------------------------------------------
    // Unused mouse events — override in subclass if needed
    // -----------------------------------------------------------------------
    @Override public void mouseClicked(final MouseEvent e)  {}
    @Override public void mouseReleased(final MouseEvent e) {}
    @Override public void mouseEntered(final MouseEvent e)  {}
    @Override public void mouseExited(final MouseEvent e)   {}
    @Override public void mouseMoved(final MouseEvent e)    {}


    @Override public void keyTyped(final KeyEvent e)    {}
    @Override public void keyReleased(final KeyEvent e) {}

    // -----------------------------------------------------------------------
    // Shared math utility
    // -----------------------------------------------------------------------

    protected static float[] orthographicMatrix(
            final float left, final float right,
            final float bottom, final float top,
            final float near, final float far) {
        return new float[]{
                2f / (right - left), 0,  0, 0,
                0, 2f / (top - bottom),  0, 0,
                0, 0, -2f / (far - near),0,
                -(right + left) / (right - left),
                -(top + bottom) / (top - bottom),
                -(far  + near)  / (far  - near),
                1
        };
    }

    protected float getLiveLeft()   {
        return liveLeft;
    }

    protected float getLiveRight()  {
        return liveRight;
    }

    protected float getLiveBottom() {
        return liveBottom;
    }

    protected float getLiveTop()    {
        return liveTop;
    }
}