package cx.hell.android.pdfviewpro;

import android.app.Application;
import android.util.DisplayMetrics;
import android.util.Log;

import cn.archko.pdf.common.CrashHandler;
//import cx.hell.android.lib.pdf.PDF;

public class APVApplication extends Application {

    private final static String TAG = "cx.hell.android.pdfviewpro";

    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static APVApplication mInstance = null;

    int threadCount = 2;
    int threadPriority = 3;
    public int screenHeight=720;
    public int screenWidth=1080;

    public static APVApplication getInstance() {
        return mInstance;
    }

    public int getThreadCount() {
        return threadCount;
    }

    public void setThreadCount(int threadCount) {
        this.threadCount = threadCount;
    }

    public int getThreadPriority() {
        return threadPriority;
    }

    public void setThreadPriority(int threadPriority) {
        this.threadPriority = threadPriority;
    }

    public void onCreate() {
        super.onCreate();
        //PDF.setApplicationContext(this); // PDF class needs application context to load assets
        mInstance = this;
        Thread.setDefaultUncaughtExceptionHandler(new CrashHandler());

        DisplayMetrics displayMetrics=getResources().getDisplayMetrics();
        screenHeight=displayMetrics.heightPixels;
        screenWidth=displayMetrics.widthPixels;
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
