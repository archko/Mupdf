package cn.archko.pdf.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.ref.WeakReference;

/**
 * @author: archko 2014/4/17 :15:21
 */
public class Util {

    public static final String getFileSize(final long size) {
        if (size>1073741824) {
            return String.format("%.2f", size/1073741824.0)+" GB";
        } else if (size>1048576) {
            return String.format("%.2f", size/1048576.0)+" MB";
        } else if (size>1024) {
            return String.format("%.2f", size/1024.0)+" KB";
        } else {
            return size+" B";
        }

    }

    /**
     * Execute an {@link android.os.AsyncTask} on a thread pool
     *
     * @param forceSerial True to force the task to run in serial order
     * @param task        Task to execute
     * @param args        Optional arguments to pass to
     *                    {@link android.os.AsyncTask#execute(Object[])}
     * @param <T>         Task argument type
     */
    @SuppressLint("NewApi")
    public static <T> void execute(final boolean forceSerial, final AsyncTask<T, ?, ?> task,
        final T... args) {
        final WeakReference<AsyncTask<T, ?, ?>> taskReference=new WeakReference<AsyncTask<T, ?, ?>>(
            task);
        if (Build.VERSION.SDK_INT<Build.VERSION_CODES.DONUT) {
            throw new UnsupportedOperationException(
                "This class can only be used on API 4 and newer.");
        }
        if (Build.VERSION.SDK_INT<Build.VERSION_CODES.HONEYCOMB||forceSerial) {
            taskReference.get().execute(args);
        } else {
            taskReference.get().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, args);
        }
    }

    public static void serializeObject(Object obj, String filename) {
        // SLLog.d("serialization", "serialize: " + obj.getClass().getSimpleName());
        try {
            File file=new File(filename);
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            ObjectOutputStream out=new ObjectOutputStream(new FileOutputStream(file));
            out.writeObject(obj);
            out.flush();
            out.close();
        } catch (Exception e) {
            File file=new File(filename);
            if (!file.getParentFile().exists()) {
                file.delete();
            }
            e.printStackTrace();
        }
    }

    public static Object deserializeObject(String filename) {
        // SLLog.d("serialization", "deserialize: " + filename);
        try {
            ObjectInputStream in=new ObjectInputStream(new FileInputStream(filename));
            Object object=in.readObject();
            in.close();
            return object;
        } catch (Exception e) {
            File file=new File(filename);
            if (!file.getParentFile().exists()) {
                file.delete();
            }
            e.printStackTrace();
        }
        return null;
    }

    public static Object deserializeObject(byte[] data) {
        // SLLog.d("serialization", "deserialize: " + data);
        if (data!=null&&data.length>0) {
            try {
                ObjectInputStream in=new ObjectInputStream(new ByteArrayInputStream(data));
                Object obj=in.readObject();
                in.close();
                return obj;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * @param context
     * @return
     */
    public static int getScreenWidthPixelWithOrientation(Context context) {
        DisplayMetrics dm = new DisplayMetrics();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(dm);
        int width = (int) dm.widthPixels;
        return width;
    }

    public static int getScreenHeightPixelWithOrientation(Context context) {
        DisplayMetrics dm = new DisplayMetrics();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(dm);
        int width = (int) dm.heightPixels;
        return width;
    }
}
