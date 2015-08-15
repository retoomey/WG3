package org.wg3.gui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.opengl.GLCanvas;
import org.eclipse.swt.opengl.GLData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.PlatformUI;
import org.wg3.opengl.DonutGL;

/**
 * Put all the __SWT__ gl stuff here, since I want the opengl plugin to only have a jogl dependency.
 * (it's possible to render opengl in different opengl library contexts, for example)
 * 
 * Set up a SWT GLCanvas and call the donut opengl code in the opengl plugin...
 * 
 * @author Robert Toomey
 *
 */
public class SWTDonut {

	private DonutGL myDonut;

	/** Widget that displays OpenGL content. */
	private GLCanvas glSWTCanvas;

	public SWTDonut(){

		// The donut opengl has all the drawing code
		myDonut = new DonutGL();
	}

	public void start(Composite composite){

		// SWT ONLY STUFF, create canvas to draw in
		GLData gldata = new GLData();
		gldata.doubleBuffer = true;
		glSWTCanvas = new GLCanvas( composite, SWT.NO_BACKGROUND, gldata );
		glSWTCanvas.setCurrent();


		// SWT
		glSWTCanvas.addListener(SWT.Resize, new Listener() {
			public void handleEvent(Event event) {

				// SWT
				glSWTCanvas.setCurrent();
				Rectangle bounds = glSWTCanvas.getBounds();

				// Pass rectangle in (gl library doesn't know about SWT rectangles)
				myDonut.handleResize(bounds.x, bounds.y, bounds.width, bounds.height);
			}
		});

		myDonut.initialize();

		// DRAWING THREAD FOREVER.  Pull all the swt opengl stuff out of our opengl library
		(new Thread() {
			public void run() {
				while( (glSWTCanvas != null) && !glSWTCanvas.isDisposed() ) {

					PlatformUI.getWorkbench().getDisplay().syncExec( new Runnable() {
						public void run() {
							if( (glSWTCanvas != null) && !glSWTCanvas.isDisposed()) {
								glSWTCanvas.setCurrent();
								myDonut.render();
								// SWT
								glSWTCanvas.swapBuffers();
							}
						}
					});

					// Then wait...
					try {
						// don't make loop too tight, or not enough time
						// to process window messages properly
						sleep( 1 );
					} catch( InterruptedException interruptedexception ) {
						// we just quit on interrupt, so nothing required here
					}
				}
			}
		}).start();
	}


	public void setBackgroundColor(float r, float g, float b){
		myDonut.setBackgroundColor(r, g, b);
	}

	public void dispose() {
		// SWT
		glSWTCanvas.dispose();
	}
}
