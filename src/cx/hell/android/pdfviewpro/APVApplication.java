package cx.hell.android.pdfviewpro;

import android.app.Application;
import android.util.Log;
//import cx.hell.android.lib.pdf.PDF;

public class APVApplication extends Application {
    
    private final static String TAG = "cx.hell.android.pdfviewpro";
    
    public boolean hasChanged=false;
    private static APVApplication mInstance=null;
    
    public static APVApplication getInstance(){
    	return mInstance;
    }

    public void onCreate() {
        super.onCreate();
        //PDF.setApplicationContext(this); // PDF class needs application context to load assets
        mInstance=this;
    }
    
    /**
     * Called by system when low on memory.
     * Currently only logs.
     */
    public void onLowMemory() {
        super.onLowMemory();
        Log.w(TAG, "onLowMemory"); // TODO: free some memory (caches) in native code
    }

}
