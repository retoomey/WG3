package org.wg3.datatype;

/** A wrapper to a DataType.  This is used by builders to allow lazy create of a DataType
 * in a thread.  isReady() is true once the DataType is completely created/loaded.
 * 
 * For example...Record is clicked in the display...now we have to go to the web, or disk,
 * load the stuff, create data tiles, etc.  This takes time. 
 * 
 * This is pretty much a Future<DataType> replacement, until such time I 
 * figure out the best way to do it in the crazy complicated Eclipse API
 * 
 * FIXME: add error information, etc...so GUI can find out what went wrong if it did.
 * FIXME: merge with Product I'm guessing....
 * @author Robert Toomey
 *
 */
public class DataRequest {

    public DataType myDataType = null;
    public boolean myDataTypeReady = false;
    public static DataRequestGlobalListener myGlobalListener = null;
    
    public static interface DataRequestGlobalListener {
        public void notifyDataRequestDone();
    }
    
    /** Is the DataType object loaded and ready for use? */
    public boolean isReady() {
        return myDataTypeReady;
    }

    /** Ready can only be set true, once the worker thread has completely generated the 
     * DataType object.  Called by builder thread when done.
     * @return
     */
    public void setReady(DataType dt) {
        myDataType = dt;  // Completed datatype
        myDataTypeReady = true; 
        if (myGlobalListener != null){
            //SingletonManager.getInstance().notifyDataRequestDone();   
            myGlobalListener.notifyDataRequestDone();
        }
    }

    public DataType getDataType() {
        return myDataType;
    }
    
    public static void setDataRequestGlobalListener(DataRequestGlobalListener l){
        myGlobalListener = l;
    }
}
