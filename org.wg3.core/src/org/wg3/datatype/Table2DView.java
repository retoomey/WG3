package org.wg3.datatype;

import org.wg3.storage.Location;

/** The table 2D interface allows DataType to implement an 'excel' spreadsheet-like 2D table.
 * This is a different 'view' of the data.  The GUI uses this class to display an actual table.
 * 
 *  @author Robert Toomey
 */
public interface Table2DView {

    /** Locations for the getLocation method.  Each value represents the exact area of a data cell outline
     * in relation to the table. For example, TOP_LEFT for a radial set cell would be the range + the gate width
     * of the start azimuth..TOP_RIGHT the range + gate width + end azimuth.
     *
     * FIXME: add 'set' ability
     */
    public enum LocationType {

        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT,
        CENTER
    };

    /** Returned by getCellValue.  Different DataTypes have
     * different stuff.  You can create a subclass to hold
     * more information for a particular data type.
     */
    public static class CellQuery {
        public float value;
        public String text;
        public int row;
        public int col;

	/** Set if query is within range of the number of rows */
	public boolean rowInRange;

	/** Set if query is within range of the number of cols */
	public boolean colInRange;

	/** Was the query inside the table? */
	public boolean isInGrid(){
		return rowInRange && colInRange;
	}
    }
    
    /** Get the number of columns in the table */
    int getNumCols();

    /** Get the number of rows in the table */
    int getNumRows();

    /** Get the row label for given row number */
    String getRowHeader(int row);

    /** Get the col label for given col number */
    String getColHeader(int col);

    /** Get the cell value as a float */
    boolean getCellValue(int row, int col, CellQuery output);

    /** Get the 3D location of the given cell part, if capable.  Return false if unable.
     * The GUI can use this to display a 3D outline in space of a visible table
     */
    public boolean getLocation(LocationType type, int row, int col, Location output);

    /** For a given Location, get the hit row and col. */
    public boolean getCell(Location input, CellQuery output);

}
