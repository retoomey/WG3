package org.wg3.opengl;

import com.jogamp.common.nio.Buffers;
// JOGL 1 import com.sun.opengl.util.BufferUtil;
import java.awt.Point;
import java.awt.Rectangle;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Iterator;


// JOGL 1 import javax.media.opengl.GL;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import org.wg3.log.Logger;
import org.wg3.log.LoggerFactory;
import org.wg3.storage.Array1DOpenGL;
import org.wg3.storage.GrowList;

/**
 * Quad Strip renders quads 'squares' as a series of strips. It also keeps two
 * different color values, one for 'display' and another for 'readout'.
 *
 * @author Robert Toomey
 */
public class QuadStripRenderer {

    @SuppressWarnings("unused")
	private final static Logger LOG = LoggerFactory.getLogger(QuadStripRenderer.class);
    /**
     * Offsets for the quad strips
     */
    protected GrowList<Integer> myOffsets;
    /**
     * Verts for the quads
     */
    protected Array1DOpenGL verts;
    /**
     * Corresponding colors
     */
    protected Array1DOpenGL colors;
    /**
     * Colors as readout information
     */
    protected Array1DOpenGL readout;
    /**
     * Set to true once enabled
     */
    protected boolean canDraw = false;
    /**
     * The mode..
     */
    protected int myGLMode = GL2.GL_QUAD_STRIP;
    /**
     * Set if batched. (multiple calls to draw but a single opengl setup) If
     * true, caller must call beginBatch, endBatch around draw. Basically for
     * rendering tons of the same stuff we only want to setup and strip down
     * once (such as tons of tiles)
     */
    protected boolean isBatched = false;

    public QuadStripRenderer() {
    }

    /**
     *
     * @param dc Draw context in opengl for drawing our radial set
     */
    public void draw(GL gl) {
        drawData(gl, false);
    }

    public void setBatched(boolean flag) {
        isBatched = flag;
    }

    /**
     * Return true when capable of drawing
     */
    public boolean canDraw() {
        return canDraw && (verts != null) && (colors != null);
    }

    public void setCanDraw(boolean flag) {
        canDraw = flag;
    }

    /**
     * Begin the readout scissor...static method to wrap drawing lots of stuff
     */
    public static void beginReadout(Point p, Rectangle view, GL gl) {
        // The GLDrawable height isn't always the height of the VISIBLE
        // opengl window.  When using a lightweight widget it's usually
        // bigger.  Heavyweight you could just use the dc.getDrawableHeight
        int fullH = (int) (view.getHeight());
        int y = fullH - p.y - 1;  // Invert Y for openGL...

        int boxWidth = 1;
        int xbox = p.x - (boxWidth / 2);
        if (xbox < 0) {
            xbox = 0;
        }
        int ybox = y - (boxWidth / 2);
        if (ybox < 0) {
            ybox = 0;
        }
        gl.glScissor(xbox, ybox, boxWidth, boxWidth);
        gl.glEnable(GL.GL_SCISSOR_TEST);
        gl.glClearColor(0, 0, 0, 0);  // FIXME pop?
        gl.glClear(GL.GL_COLOR_BUFFER_BIT);
    }

    /**
     * End scissor and get the ByteBuffer. Done after a batch of drawing
     */
    public static ByteBuffer endReadout(Point p, Rectangle view, GL gl) {
        int fullH = (int) (view.getHeight());
        int y = fullH - p.y - 1;  // Invert Y for openGL...
        // JOGL 1 ByteBuffer data = BufferUtil.newByteBuffer(4);
        ByteBuffer data = Buffers.newDirectByteBuffer(4);
        gl.glDisable(GL.GL_SCISSOR_TEST);
        gl.glReadPixels(p.x, y, 1, 1, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, data);

        return data;
    }

    /**
     * Get the bytes for a point...used for mouse based readout This uses a
     * trick of drawing the point under the mouse with a data color, allowing us
     * to read only it back with read pixels.
     *
     * This works when readout is just one quadstriprenderer...otherwise need to
     * use begin and endreadout..
     */
    public ByteBuffer getReadoutBytes(Point p, Rectangle view, GL gl) {
        ByteBuffer b = null;
        if (canDraw()) {
            beginReadout(p, view, gl);
            drawData(gl, true);
            b = endReadout(p, view, gl);
        }
        return b;
    }

    

    //public static class ReadoutInfo {
    /**
     * Was the readout information missing?
     */
    //  public boolean missing = true;
    /**
     * The 4 bytes as a float value
     */
    //  public float asFloat;
    /**
     * The first two bytes as a short -32768 to 32768
     */
    // public short asX;
    /**
     * The second two bytes as a short
     */
    // public short asY;
    /**
     * Get unsigned int range 0 to 65535
     */
    // public int getUnsignedXInt() {
    //   return ((int) asX) + 32768;
    //  }
    /**
     * Get unsigned int range 0 to 65535
     */
    //public int getUnsignedYInt() {
    //    return ((int) asY) + 32768;
    // }
    //}
    /**
     * Get readout as a ReadoutInfo back from colors.
     */
    /*
     public ReadoutInfo getReadout(Point p, Rectangle view, GL gl) {
     ReadoutInfo r = new ReadoutInfo();
     if (canDraw()) {
     ByteBuffer data = getReadoutBytes(p, view, gl);

     byte d0 = data.get(0);
     byte d1 = data.get(1);
     byte d2 = data.get(2);
     byte d3 = data.get(3);

     if ((d0 == 0) && (d1 == 0) && (d2 == 0) && (d3 == 0)) {
     //out = "N/A";
     //out.setValue(-1);
     } else {
     float readoutValue = bytesToFloat(data.get(0), data.get(1), data.get(2), data.get(3));
     r.missing = false;
     }
     }
     return r;
     }
     */
    /**
     * Get readout as a float value back from colors.
     */
   /* public float getReadout(Point p, Rectangle view, GL gl, float missingValue) {
        float readoutValue = missingValue;
        if (canDraw()) {
            ByteBuffer data = getReadoutBytes(p, view, gl);

            byte d0 = data.get(0);
            byte d1 = data.get(1);
            byte d2 = data.get(2);
            byte d3 = data.get(3);

            if ((d0 == 0) && (d1 == 0) && (d2 == 0) && (d3 == 0)) {
                //out = "N/A";
                //out.setValue(-1);
            } else {
                readoutValue = bytesToFloat(data.get(0), data.get(1), data.get(2), data.get(3));
                //LOG.debug("Float back "+readoutValue);
            }
        }
        return readoutValue;
    }
*/
    /**
     *
     * @param dc Draw context in opengl for drawing our radial set
     */
    public void drawData(GL glin, boolean readoutMode) {
    	GL2 gl = glin.getGL2();
        if (canDraw()) {
            final boolean b = isBatched;
            try {
                Object lock1 = verts.getBufferLock();
                Object lock2 = readoutMode ? readout.getBufferLock() : colors.getBufferLock();
                // Nest ok, we always lock data before colors...
                synchronized (lock1) {
                    synchronized (lock2) {

                        if (!b) {
                            beginBatch(gl);
                        }
                        if (readoutMode) {
                            gl.glDisable(GL.GL_DEPTH_TEST);

                        } else {
                            gl.glEnable(GL.GL_DEPTH_TEST);
                        }

                        FloatBuffer z = verts.getRawBuffer();
                        FloatBuffer c = readoutMode ? readout.getRawBuffer() : colors.getRawBuffer();

                        // Only render if there is data to render
                        if ((z != null) && (z.capacity() > 0)) {
                            gl.glVertexPointer(3, GL.GL_FLOAT, 0, z.rewind());
          
                            // Isn't this color kinda wasteful really?  We have 4 floats per color,
                            // or 4 bytes * 4 = 16 bytes, when GL only stores 4 bytes per color lol
                            // We should use GL.GL_BYTE and convert ourselves to it, will save memory...
                            gl.glColorPointer(4, GL.GL_UNSIGNED_BYTE, 0, c.rewind());

                            Iterator<Integer> itr = myOffsets.iterator();
                            int counter = 0;
                            if (itr.hasNext()) {
                                Integer now = itr.next();
                                while (itr.hasNext()) {
                                    Integer plus1 = itr.next();
                                    if (plus1 != null) {
                                        int start_index = now;
                                        int end_index = plus1;
                                        int run_indices = end_index - start_index;
                                        int start_vertex = start_index / 3;
                                        int run_vertices = run_indices / 3;
                                        gl.glDrawArrays(myGLMode, start_vertex,
                                                run_vertices);
                                        now = plus1;
                                    }
                                    counter++;

                                }
                            }
                        }
                    }
                }
            } finally {
                if (!b) {
                    endBatch(gl);
                }
            }
        }
    }

    /**
     * Using a beginBatch/endBatch. This will only work when rendering a group
     * of common tiles.. if we eventually 'mix' tile types it could fail
     *
     * @param dc
     * @return
     */
    public static boolean beginBatch(GL glin) {

    	GL2 gl = glin.getGL2();
        gl.glPushAttrib(GL.GL_DEPTH_BUFFER_BIT | GL2.GL_LIGHTING_BIT
                | GL.GL_COLOR_BUFFER_BIT
                | GL2.GL_ENABLE_BIT
                | GL2.GL_TEXTURE_BIT | GL2.GL_TRANSFORM_BIT
                | GL2.GL_VIEWPORT_BIT | GL2.GL_CURRENT_BIT);

        gl.glDisable(GL2.GL_LIGHTING);
        gl.glDisable(GL.GL_TEXTURE_2D); // no textures

        gl.glDisable(GL.GL_DEPTH_TEST);
        gl.glShadeModel(GL2.GL_FLAT);
        //		gl.glShadeModel(GL.GL_SMOOTH);

        gl.glPushClientAttrib(GL2.GL_CLIENT_VERTEX_ARRAY_BIT
                | GL2.GL_CLIENT_PIXEL_STORE_BIT);
        gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL2.GL_COLOR_ARRAY);

        return true;
    }

    public static void endBatch(GL glin) {
    	GL2 gl = glin.getGL2();
        gl.glPopClientAttrib();
        gl.glPopAttrib();
    }

    /**
     * Preallocate the memory required for data. It's a bit of a pain in the ass
     * for subclasses to pre-calculate the data needed, but speeds up our
     * generation a lot
     */
    public boolean allocate(int counter, int ccounter) {
        // The opengl thread can draw anytime..
        verts = new Array1DOpenGL(counter, 0.0f);
        colors = new Array1DOpenGL(ccounter / 4, 0.0f); // use one 'float' per color...

        // READOUT
        readout = new Array1DOpenGL(ccounter / 4, 0.0f);  // use one 'float' per color...

        myOffsets = new GrowList<Integer>();
        return true;
    }

    /**
     * Called before beginning modification. Used by cache to optimize data
     * loading
     */
    public void begin() {
        verts.begin();
        colors.begin();
        readout.begin();
    }

    /**
     * Called after modification. Used by cache to optimize data loading
     */
    public void end() {
        verts.end();
        colors.end();
        readout.end();
    }

    // Not sure I should do it this way, might be better to not expose
    // the enternal fields...
    public Array1DOpenGL getVerts() {
        return verts;
    }

    public Array1DOpenGL getColors() {
        return colors;
    }

    public Array1DOpenGL getReadout() {
        return readout;
    }

    public GrowList<Integer> getOffsets() {
        return myOffsets;
    }
}
