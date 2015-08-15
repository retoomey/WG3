package org.wg3.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;

// w3 logging wrapper
import org.wg3.log.Logger;
import org.wg3.log.LoggerFactory;

// w3 storage.  We use the grid visible area structure for representing selections
import org.wg3.storage.GridVisibleArea;

/**
 * My version of a JSwing Table that avoids all of the O(n) data structures in
 * order to be true virtual.
 *
 * A virtual table has to have a set width and height per cell, otherwise you
 * end up doing a lot of O(n) updating. A virtual table shouldn't create N
 * objects such as TableColumn for each of its billion columns.
 * 
 * Provide a subclass of SimpleTableModel for data of the table.
 * 
 * @author Robert Toomey
 */
public class SimpleTable extends JComponent
implements Scrollable,
MouseListener, MouseMotionListener {

	private static final long serialVersionUID = 1039280545368897044L;
	@SuppressWarnings("unused")
	private final static Logger LOG = LoggerFactory.getLogger(SimpleTable.class);
	private final JScrollPane myScrollPane;

	/**
	 * Simple table model, unlike the swing counter part, doesn't create
	 * billions of TableColumn objects. Every cell in the table is the same
	 * type, having a set width/height. We also combine the model with the cell
	 * renderer.
	 *
	 * This default creates a checkered grid for testing. FIXME: make a
	 * TestSimpleTableModel instead...
	 *
	 * This model will power the headers, corner and contents of our Table
	 */
	public static class SimpleTableModel {

		public SimpleTable myTable;
		private Font myFont = new Font("SansSerif", Font.PLAIN, 10);

		// We shouldn't be reentrant, sync should be not needed.
		public SimpleTableRenderInfo buffer = new SimpleTableRenderInfo();

		public SimpleTableModel(){

		}
		public SimpleTableModel(int rows, int cols){
			numRows = rows;
			numCols = cols;
		}
		public void handleScrollAdjust(AdjustmentEvent e) {
		}

		/**
		 * Subclasses can create subclasses of this to add information to
		 * render. ???
		 */
		public static class SimpleTableRenderInfo {

			Color background;
			Color foreground;
			String text;
		}
		private int cellWidth = 50;
		/**
		 * cellHeight used when not using font
		 */
		private int cellHeight = 16;
		/**
		 * When true, calculates height of cell from the current font, otherwise
		 * uses the cellHeight value
		 */
		private boolean useFontForHeight = true;
		private int headerWidth = 70;
		/**
		 * headerHeight used when not using font
		 */
		private int headerHeight = 16;
		private int numRows = 10000;
		private int numCols = 10000;
		/**
		 * The visible grid within the table
		 */
		private GridVisibleArea myCurrentGrid;
		/**
		 * The selected grid within the table
		 */
		private GridVisibleArea mySelectionGrid = new GridVisibleArea();

		/**
		 * Get the total width of the cells in the table
		 */
		public int getWidth() {
			return getNumCols() * getCellWidth();
		}

		public int getNumCols() {
			return numCols;
		}

		public int getCellWidth() {
			return cellWidth;
		}

		/**
		 * Get the total height of the cells in the table
		 */
		public int getHeight() {
			return (getNumRows() * getCellHeight());
		}

		public int getNumRows() {
			return numRows;
		}

		public int getCellHeight() {
			int height = cellHeight;
			if (useFontForHeight) {
				FontMetrics fm = getFontMetrics(myFont, null);
				height = fm.getMaxAscent() + fm.getMaxDescent();
			}
			return height;
		}

		/**
		 * Left margin used when cells left aligned
		 */
		public int getLeftMargin() {
			return 2;
		}

		/**
		 * Return width of the header, or -1 if off
		 */
		public int getRowHeaderWidth() {
			return headerWidth;
		}

		public int getColHeaderHeight() {
			int height = headerHeight;
			if (useFontForHeight) {
				FontMetrics fm = getFontMetrics(myFont, null);
				height = fm.getMaxAscent() + fm.getMaxDescent();
			}
			return height;
		}

		/**
		 * Get the row label for given row number
		 */
		public String getRowHeader(int row) {
			return String.format("%d,R", row);

		}

		/**
		 * Get the col label for given col number
		 */
		public String getColHeader(int col) {
			return String.format("H,%d", col);
		}

		// Humm depends upon wdssii..should it? The GridVisibleArea is used
		// by the display for rendering the outline of table in 3D window
		public GridVisibleArea updateVisibleGrid(Rectangle clipBounds) {
			GridVisibleArea a = new GridVisibleArea();

			a.startCol = clipBounds.x / getCellWidth();
			a.clipBounds = clipBounds;
			a.startRow = clipBounds.y / getCellHeight();
			a.lastPartialColumn = (clipBounds.x + clipBounds.width) / getCellWidth();
			a.lastPartialRow = (clipBounds.y + clipBounds.height) / getCellHeight();

			int rows = getNumRows();
			int cols = getNumCols();
			a.numRows = rows;
			a.numCols = cols;

			if (a.lastPartialColumn > cols - 1) {
				a.lastPartialColumn = cols - 1;
			}
			if (a.lastPartialRow > rows - 1) {
				a.lastPartialRow = rows - 1;
			}
			a.lastFullRow = a.lastPartialRow - 1;
			a.lastFullColumn = a.lastPartialColumn - 1;

			//LOG.debug("Rectangle is " + clipBounds + ", " + rows + ", " + cols);
			myCurrentGrid = a;
			return a;

		}

		public GridVisibleArea getCurrentVisibleGrid() {
			return myCurrentGrid;
		}

		// Note we only support a 'block' continuous selection
		public GridVisibleArea getSelectionGrid() {
			return mySelectionGrid;
		}

		// Demo fill the grid...
		public SimpleTableRenderInfo getCellInfo(int row, int col,
				int x, int y, int w, int h) {
			SimpleTableRenderInfo info = new SimpleTableRenderInfo();
			if (row == -1) {
				info.background = Color.YELLOW;
				info.text = String.format("H,%d", col);
			} else if (col == -1) {
				info.background = Color.GREEN;
				info.text = String.format("%d,R", row);
			} else {
				if ((row % 2 == 0)) {
					if ((col % 2 == 0)) {
						info.background = Color.RED;
					} else {
						info.background = Color.BLUE;
					}
				} else {
					if ((col % 2 == 0)) {
						info.background = Color.BLUE;
					} else {
						info.background = Color.RED;
					}
				}
				info.text = String.format("%d", row);
			}
			info.foreground = Color.BLACK;
			return info;
		}

		public boolean valid(Point out) {
			return ((out.x > -1) && (out.y > -1));
		}

		public void invalidate(Point out) {
			out.x = out.y = -1;
		}

		/**
		 * Cell hit test (in table coordinates) Fills point with row, col.
		 */
		public boolean hitTest(int x, int y, Point out) {
			final int w = getCellWidth();
			final int h = getCellHeight();
			int col = x / w;
			int row = y / h;

			// If we pin row and col, then we allow dragging when mouse outside the view window...
			if (row < 0) {
				row = 0;
			}
			if (col < 0) {
				col = 0;
			}
			final int maxRows = getNumRows();
			final int maxCols = getNumCols();
			if (row > maxRows) {
				row = maxRows;
			}
			if (col > maxCols) {
				col = maxCols;
			}
			out.x = row;
			out.y = col;
			return valid(out);
		}

		/**
		 * Select a boxed range of our grid
		 */
		public void selectRange(int srow, int scol, int erow, int ecol) {

			// Pin selection to min/max
			if (srow < 0) {
				srow = 0;
			}
			if (erow < 0) {
				erow = 0;
			}
			if (scol < 0) {
				scol = 0;
			}
			if (ecol < 0) {
				ecol = 0;
			}
			final int maxr = getNumRows() - 1;
			final int maxc = getNumCols() - 1;

			// Value is from 0 to (n-1) pin to (n-1)
			// For example 0 to 256 (257 cols) value >= 256 --> value = 256
			// Easy to goof this thus the comment
			if (srow >= maxr) {
				srow = maxr;
			}
			if (erow >= maxr) {
				erow = maxr;
			}
			if (scol >= maxc) {
				scol = maxc;
			}
			if (ecol >= maxc) {
				ecol = maxc;
			}

			// The primary row/col is not 'grid' sorted
			mySelectionGrid.primaryRow = erow;
			mySelectionGrid.primaryCol = ecol;

			// Sort by 'grid'.  Basically start at top left, end at 
			// bottom right
			if (srow > erow) {
				mySelectionGrid.startRow = erow;
				mySelectionGrid.lastPartialRow = srow;
				mySelectionGrid.lastFullRow = srow;
			} else {
				mySelectionGrid.startRow = srow;
				mySelectionGrid.lastPartialRow = erow;
				mySelectionGrid.lastFullRow = erow;
			}
			if (scol > ecol) {
				mySelectionGrid.startCol = ecol;
				mySelectionGrid.lastFullColumn = scol;
				mySelectionGrid.lastPartialColumn = scol;

			} else {
				mySelectionGrid.startCol = scol;
				mySelectionGrid.lastFullColumn = ecol;
				mySelectionGrid.lastPartialColumn = ecol;
			}
			mySelectionGrid.numRows = erow - srow + 1;
			mySelectionGrid.numCols = ecol - scol + 1;
			mySelectionGrid.active = true;
		}

		/**
		 * Is cell within current selection grid?
		 */
		public boolean isCellSelected(int row, int col) {
			boolean selected;
			GridVisibleArea a = getSelectionGrid();

			if (a.active) {
				selected = ((col >= a.startCol)
						&& (col <= a.lastPartialColumn)
						&& (row >= a.startRow)
						&& (row <= a.lastPartialRow));
			} else {
				selected = false;
			}
			return selected;
		}

		/**
		 * Draw a cell box, we're using our model as a renderer as well, a -1
		 * for row means it's the rowheader, -1 for col that it's the col
		 * header.
		 *
		 * We have a default drawCellBox that just draws a text string with a
		 * background/foreground.
		 */
		public void drawCellBox(Graphics g, int row, int col,
				int x, int y, int w, int h) {

			boolean selected = isCellSelected(row, col);
			SimpleTableRenderInfo info = getCellInfo(row, col, x, y, w, h);
			Graphics2D g2 = (Graphics2D) g;
			if (selected) {
				Color bb = info.background.brighter().brighter();
				g.setColor(bb);
			} else {
				g.setColor(info.background);
			}
			g.fillRect(x, y, w, h);

			g.setColor(info.foreground);
			g.setFont(myFont);
			int left = getLeftMargin();
			FontMetrics fm = getFontMetrics(myFont, g);
			// Don't need to 'clip' since we draw top down, left right
			// the cells below will clip above.
			//int wtxt = fm.stringWidth(info.text);

			// Draw cell string
			g.drawString(info.text, x + left, y + fm.getAscent());

			// Draw the cell border.
			g.setColor(Color.BLACK);
			g.drawRect(x, y, w, h);

			// -------------------------------------------------
			// Cell Selection
			// Could do selection border in a separate pass instead,
			// this would allow fancier/cleaning rendering..
			// We draw a bit strange since future cells will draw
			// over previous ones...
			final boolean l = isCellSelected(row, col - 1);
			final boolean u = isCellSelected(row - 1, col);
			final boolean d = !selected && isCellSelected(row - 1, col - 1);
			final boolean rightEdge = !selected && l;
			final boolean bottomEdge = !selected && u;
			final boolean topEdge = selected && !u;
			final boolean leftEdge = selected && !l;

			g.setColor(Color.RED);
			g2.setStroke(new BasicStroke(
					4.0f, // Width
					BasicStroke.CAP_SQUARE, // End cap
					BasicStroke.JOIN_MITER, // Join style
					10.0f, // Miter limit
					new float[]{10.0f, 10.0f}, // Dash pattern
					0.0f));                   // Dash phase	4
			if (rightEdge || leftEdge) {
				g.drawLine(x, y, x, y + h);
			}
			if (bottomEdge || topEdge) {
				g.drawLine(x, y, x + w, y);
			}
			if (d) {
				g.drawLine(x, y, x, y);
			}
			g2.setStroke(new BasicStroke(1));

		}

		public void setTable(SimpleTable t) {
			myTable = t;
		}

		public void handleDataChanged() {
			if (myTable != null) {
				myTable.handleDataChanged();
			}
		}

		public FontMetrics getFontMetrics(Font forFont, Graphics g) {
			// Humm..do this everytime or cache it?
			FontMetrics fm;
			// No graphics if openGL.  'Could' pass a component down from above
			if (g == null) {
				BufferedImage bi = new BufferedImage(5, 5, BufferedImage.TYPE_INT_RGB);
				fm = bi.getGraphics().getFontMetrics(forFont);
			} else {
				fm = g.getFontMetrics(forFont);
			}
			return fm;
		}
	}

	private static class Rule extends JComponent {

		/**
		 * 
		 */
		private static final long serialVersionUID = 6549336460757390851L;
		public static final int HORIZONTAL = 0;
		public static final int VERTICAL = 1;
		public int orientation;
		private SimpleTableModel myModel;

		public Rule(int o, boolean m) {
			orientation = o;
		}

		public void setModel(SimpleTableModel model, boolean update) {
			myModel = model;
			if (update) {
				handleDataChanged();
			}
		}

		public void handleDataChanged() {
			revalidate();
			repaint();
		}

		@Override
		public Dimension getPreferredSize() {
			int w = 0, h = 0;
			if (myModel != null) {
				if (orientation == HORIZONTAL) {
					w = myModel.getWidth();
					h = myModel.getColHeaderHeight();
				} else {
					w = myModel.getRowHeaderWidth();
					h = myModel.getHeight();
				}
			}
			return new Dimension(w, h);
		}

		@Override
		protected void paintComponent(Graphics g) {
			if (myModel != null) {
				Rectangle drawHere = g.getClipBounds();

				// Fill clipping area with dirty brown/orange.
				g.setColor(new Color(230, 163, 4));
				g.fillRect(drawHere.x, drawHere.y, drawHere.width, drawHere.height);

				GridVisibleArea a = myModel.updateVisibleGrid(getVisibleRect());
				if (orientation == HORIZONTAL) {
					// The header columns....
					int w = myModel.getCellWidth();
					int h = myModel.getColHeaderHeight();
					for (int col = a.startCol; col <= a.lastPartialColumn; col++) {

						int x = myModel.getCellWidth() * col;
						//  int y = drawHere.y;
						int y = 0;

						myModel.drawCellBox(g, -1, col,
								x, y, w, h);
					}
				} else {
					// The header columns....
					int w = myModel.getRowHeaderWidth();
					int h = myModel.getCellHeight();
					for (int row = a.startRow; row <= a.lastPartialRow; row++) {
						//Rectangle cell;

						//   int x = drawHere.x;
						int x = 0;
						int y = myModel.getCellHeight() * row;

						myModel.drawCellBox(g, row, -1,
								x, y, w, h);

					}
				}

			}
		}
	}
	private SimpleTableModel myModel;
	private Rule myRowHeader;
	private Rule myColHeader;

	/** Create a default table */
	public SimpleTable(JScrollPane pane) {
		super();
		myColHeader = new Rule(Rule.HORIZONTAL, true);
		myRowHeader = new Rule(Rule.VERTICAL, true);
		myScrollPane = pane;

		setOpaque(true);
		setBackground(Color.white);

		//Let the user scroll by dragging to outside the window.
		setAutoscrolls(true); //enable synthetic drag events
		addMouseMotionListener(this); //handle mouse drags
		addMouseListener(this);
	}

	public void setupScrollPane(JScrollPane pane) {
		pane.setViewportView(this);
		pane.setColumnHeaderView(myColHeader);
		pane.setRowHeaderView(myRowHeader);

		// Listen for value changes in the scroll pane's scrollbars
		AdjustmentListener listener = new AdjustmentListener() {
			@Override
			public void adjustmentValueChanged(AdjustmentEvent e) {
				if (myModel != null) {
					myModel.handleScrollAdjust(e);
				}
				/*  Adjustable source = e.getAdjustable();

                 /*
                 // getValueIsAdjusting() returns true if the user is currently
                 // dragging the scrollbar's knob and has not picked a final value
                 if (evt.getValueIsAdjusting()) {
                 // The user is dragging the knob
                 return;
                 }

                 // Determine which scrollbar fired the event
                 int orient = source.getOrientation();
                 if (orient == Adjustable.HORIZONTAL) {
                 // Event from horizontal scrollbar
                 } else {
                 // Event from vertical scrollbar
                 }

                 // Determine the type of event
                 int type = evt.getAdjustmentType();
                 switch (type) {
                 case AdjustmentEvent.UNIT_INCREMENT:
                 // Scrollbar was increased by one unit
                 break;
                 case AdjustmentEvent.UNIT_DECREMENT:
                 // Scrollbar was decreased by one unit
                 break;
                 case AdjustmentEvent.BLOCK_INCREMENT:
                 // Scrollbar was increased by one block
                 break;
                 case AdjustmentEvent.BLOCK_DECREMENT:
                 // Scrollbar was decreased by one block
                 break;
                 case AdjustmentEvent.TRACK:
                 // The knob on the scrollbar was dragged
                 break;
                 }

                 // Get current value
                 int value = evt.getValue();*/
			}
		};
		pane.getHorizontalScrollBar().addAdjustmentListener(listener);
		pane.getVerticalScrollBar().addAdjustmentListener(listener);
	}

	public void setModel(SimpleTableModel aModel) {
		myModel = aModel;
		myModel.setTable(this);
		myColHeader.setModel(myModel, false);
		myRowHeader.setModel(myModel, false);
		handleDataChanged();
	}

	public void handleDataChanged() {
		myColHeader.handleDataChanged();
		myRowHeader.handleDataChanged();
		revalidate();
		repaint();
	}

	public void selectNone() {
	}

	public void updateVisibleGrid() {
		myModel.updateVisibleGrid(getVisibleRect());
	}

	public GridVisibleArea getCurrentVisibleGrid() {
		updateVisibleGrid();
		return myModel.getCurrentVisibleGrid();
	}

	@Override
	public void paintComponent(Graphics g) {
		if (myModel != null) {
			GridVisibleArea a = myModel.updateVisibleGrid(getVisibleRect());
			final int w = myModel.getCellWidth();
			final int h = myModel.getCellHeight();
			for (int row = a.startRow; row <= a.lastPartialRow; row++) {
				for (int col = a.startCol; col <= a.lastPartialColumn; col++) {
					myModel.drawCellBox(g, row, col,
							w * col, h * row, w, h);
				}
			}

			// Special drawing for primary edit cell
			GridVisibleArea s = myModel.getSelectionGrid();
			if (s.active) {

				// Draw the cell border.
				g.setColor(Color.WHITE);
				g.drawRect(s.primaryCol * w, s.primaryRow * h, w, h);

			}
		}
	}
	// FIXME: this mouse stuff 'may' move out, since we have several
	// different table types ...
	private ToolbarMode myMode = ToolbarMode.SELECT;

	public enum ToolbarMode {

		SELECT, // Select a table region
		MOVE    // Move entire table
	}

	public void setMode(ToolbarMode mode) { // hack
		myMode = mode;
	}
	private Point start = new Point();
	private Point startCell = new Point(-1, -1);
	private Point endCell = new Point(-1, -1);
	//Methods required by the MouseMotionListener interface:

	@Override
	public void mouseMoved(MouseEvent e) {
	}

	@Override // mouse listener
	public void mouseClicked(MouseEvent e) {
	}

	@Override // mouse listener
	public void mousePressed(MouseEvent e) {
		// Get the start pixel clicked...
		start = e.getPoint();

		// Get the start cell clicked
		if (myMode == ToolbarMode.SELECT) {
			if (myModel != null) {
				Point newCell = new Point();
				if (myModel.hitTest(e.getX(), e.getY(), newCell)) {
					if (e.isShiftDown() && (myModel.valid(startCell))) {
						// This is the same as a drag from the start cell...
						// End cell should always be the dragged cell
						endCell = newCell;
					}
					if (e.isControlDown() && (myModel.valid(startCell))) {
						// Single click with cntl will do a mirror box around
						// original point..
						if (startCell == endCell) {
							int deltaX = newCell.x - startCell.x;
							int deltaY = newCell.y - startCell.y;
							startCell.x -= deltaX;
							startCell.y -= deltaY;
							endCell = newCell;
						}

					} else {
						// Select single cell
						endCell = newCell;
						startCell = newCell;
					}
					myModel.selectRange(startCell.x, startCell.y, endCell.x, endCell.y);
					repaint();
				}
			}
		}
	}

	@Override
	public void mouseDragged(MouseEvent e) {

		if (myMode == ToolbarMode.MOVE) { // move mode
			// ----------------------------------------------
			// Drag the window centered on mouse....
			SimpleTable me = (SimpleTable) e.getSource();
			JViewport vport = myScrollPane.getViewport();
			Point cp = e.getPoint();
			Point vp = vport.getViewPosition();
			vp.translate(start.x - cp.x, start.y - cp.y);
			me.scrollRectToVisible(new Rectangle(vp, vport.getSize()));
			// Since table is source, the point is in the coordinates of the whole table,
			// so we have to move this point the opposite direction
			cp.translate(start.x - cp.x, start.y - cp.y);
			start.setLocation(cp);
		} else { // selection mode...
			if (myModel != null) {
				Point track = new Point(-1, -1);
				if (myModel.hitTest(e.getX(), e.getY(), track)) {
					if (e.isControlDown()) {
						// Do nothing, only the single click works
					} else {
						// End cell should always be the dragged cell
						endCell = track;
						myModel.selectRange(startCell.x, startCell.y, endCell.x, endCell.y);
					}
				}
			}
		}
		Rectangle visible = new Rectangle(e.getX(), e.getY(), 1, 1);
		SimpleTable me = (SimpleTable) e.getSource();
		me.scrollRectToVisible(visible);
		repaint();
	}

	@Override
	public void mouseReleased(MouseEvent me) {
	}

	@Override
	public void mouseEntered(MouseEvent me) {
	}

	@Override
	public void mouseExited(MouseEvent me) {
	}

	@Override
	public Dimension getPreferredSize() {
		int w = myModel.getWidth();
		int h = myModel.getHeight();
		return new Dimension(w, h);
	}

	@Override
	public Dimension getPreferredScrollableViewportSize() {
		return getPreferredSize();
	}

	@Override
	public int getScrollableUnitIncrement(Rectangle visibleRect,
			int orientation,
			int direction) {
		//Get the current position.
		int currentPosition = 0;
		int cellSize;
		if (orientation == SwingConstants.HORIZONTAL) {
			currentPosition = visibleRect.x;
			cellSize = myModel.getCellWidth();
		} else {
			currentPosition = visibleRect.y;
			cellSize = myModel.getCellHeight();
		}

		//Return the number of pixels between currentPosition
		//and the nearest tick mark in the indicated direction.
		if (direction < 0) {
			int newPosition = currentPosition
					- (currentPosition / cellSize)
					* cellSize;
			return (newPosition == 0) ? cellSize : newPosition;
		} else {
			return ((currentPosition / cellSize) + 1)
					* cellSize
					- currentPosition;
		}
	}

	@Override
	public int getScrollableBlockIncrement(Rectangle visibleRect,
			int orientation,
			int direction) {
		if (orientation == SwingConstants.HORIZONTAL) {
			int cellSize = myModel.getCellWidth();
			return visibleRect.width - cellSize;
		} else {
			int cellSize = myModel.getCellWidth();
			return visibleRect.height - cellSize;
		}
	}

	@Override
	public boolean getScrollableTracksViewportWidth() {
		return false;
	}

	@Override
	public boolean getScrollableTracksViewportHeight() {
		return false;
	}

	/**
	 * Calculate the rectangle box of a given row/col
	 */
	public Rectangle getCellRect(int row, int col) {
		final int w = myModel.getCellWidth();
		final int h = myModel.getCellHeight();
		int x = w * col;
		int y = h * row;
		Rectangle r = new Rectangle(x, y, w, h);
		return r;
	}

	/**
	 * Try to scroll to the center of the table a particular row/col
	 */
	public void scrollToCenter(int rowIndex, int vColIndex) {
		if (!(getParent() instanceof JViewport)) {
			return;
		}
		JViewport viewport = (JViewport) getParent();
		// We aren't a JTable, so we have our own routine here...
		Rectangle rect = getCellRect(rowIndex, vColIndex);
		Rectangle viewRect = viewport.getViewRect();
		rect.setLocation(rect.x - viewRect.x, rect.y - viewRect.y);

		int centerX = (viewRect.width - rect.width) / 2;
		int centerY = (viewRect.height - rect.height) / 2;
		if (rect.x < centerX) {
			centerX = -centerX;
		}
		if (rect.y < centerY) {
			centerY = -centerY;
		}
		rect.translate(centerX, centerY);
		viewport.scrollRectToVisible(rect);
		updateVisibleGrid();
	}
}