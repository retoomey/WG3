package org.wg3.gui;

import java.awt.Frame;

import javax.swing.JApplet;
import javax.swing.JScrollPane;

import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.part.ViewPart;

public class View extends ViewPart {

	public static final String ID = "org.wg3.gui.view";
	/** Holds the OpenGL canvas. */
	private Composite composite;

	private SWTDonut d;
	private SWTDonut d2;
	
	private final boolean useGL = true;
	private final boolean useSwing = true;

	public View() {
	}

	@Override
	public void createPartControl( Composite parent ) {
		parent.setLayout(new GridLayout(1, false));

		// Create the row of buttons
		Composite buttonBar = new Composite(parent, SWT.NONE);
		buttonBar.setLayout(new RowLayout());
		Button flip = new Button(buttonBar, SWT.PUSH);
		flip.setText("Switch Orientation");
		Button weights = new Button(buttonBar, SWT.PUSH);
		weights.setText("Restore Weights");

		// Create the SashForm
		Composite sash = new Composite(parent, SWT.NONE);
		sash.setLayout(new FillLayout());
		sash.setLayoutData(new GridData(GridData.FILL_BOTH));
		final SashForm sashForm = new SashForm(sash, SWT.HORIZONTAL);

		// Change the width of the sashes
		sashForm.SASH_WIDTH = 5;

		// Change the color used to paint the sashes
		sashForm.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_RED));

		if (useGL){
			// View one, donut1
			composite = new Composite( sashForm, SWT.NONE );
			composite.setLayout( new FillLayout() );
			d = new SWTDonut();
			d.start(composite); 

			// View two, donut2
			composite = new Composite( sashForm, SWT.NONE );
			composite.setLayout( new FillLayout() );
			d2 = new SWTDonut();
			d2.setBackgroundColor(0, 0, 0);
			d2.start(composite); 
		}else{
			// View one
			final Button one = new Button(sashForm, SWT.PUSH);
			one.setText("One");
			one.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent event) {
					maximizeHelper(one, sashForm);
				}
			});
			// View two
			final Button two = new Button(sashForm, SWT.PUSH);
			two.setText("Two");
			two.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent event) {
					maximizeHelper(two, sashForm);
				}
			});

		}

		if (useSwing){

			// Standard SWT to Swing Wrap
			composite = new Composite(sashForm, SWT.EMBEDDED | SWT.NO_BACKGROUND);
			composite.setLayout(new GridLayout());
			composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
					true));
			Frame frame = SWT_AWT.new_Frame(composite);
			JApplet applet = new JApplet();
			applet.setLayout(new java.awt.GridLayout(0, 1));
			frame.add(applet);

			// FIXME: Could possibly rewrite as a SWT widget..
			// If I remember correctly, SWT is more limited in drawing options since
			// it is limited to lcd of all platforms.
			
			// Data model
			SimpleTable.SimpleTableModel m = new SimpleTable.SimpleTableModel(100000, 100000);
			
			JScrollPane scrollPane = new JScrollPane();
			SimpleTable t = new SimpleTable(scrollPane);
			t.setModel(m);
			t.setupScrollPane(scrollPane);
			
			// Add table to scroll pane
			scrollPane.getViewport().add(t);
			scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
			scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

			applet.add(scrollPane);
			frame.validate();
		}else{
			// View three
			final Button three = new Button(sashForm, SWT.PUSH);
			three.setText("Three");
			three.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent event) {
					maximizeHelper(three, sashForm);
				}
			});
		}

		// Set the relative weights for the panels
		sashForm.setWeights(new int[] { 1, 2, 3});

		// Add the Switch Orientation functionality
		flip.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				switch (sashForm.getOrientation()) {
				case SWT.HORIZONTAL:
					sashForm.setOrientation(SWT.VERTICAL);
					break;
				case SWT.VERTICAL:
					sashForm.setOrientation(SWT.HORIZONTAL);
					break;
				}
			}
		});

		// Add the Restore Weights functionality
		weights.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				sashForm.setWeights(new int[] { 1, 2, 3});
			}
		});    
	}

	private void maximizeHelper(Control control, SashForm sashForm) {
		// See if the control is already maximized
		if (control == sashForm.getMaximizedControl()) {
			// Already maximized; restore it
			sashForm.setMaximizedControl(null);
		} else {
			// Not yet maximized, so maximize it
			sashForm.setMaximizedControl(control);
		}
	}

	@Override
	public void setFocus() {
	}

	@Override
	public void dispose() {
		if (useGL){
			d.dispose();
			d2.dispose();
		}
		super.dispose();
	}

}
