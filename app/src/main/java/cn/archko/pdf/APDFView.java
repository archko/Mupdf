package cn.archko.pdf;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.PointF;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.artifex.mupdf.fitz.Cookie;
import com.artifex.mupdf.fitz.Document;
import com.artifex.mupdf.fitz.Matrix;
import com.artifex.mupdf.fitz.Page;
import com.artifex.mupdf.fitz.android.AndroidDrawDevice;

import org.vudroid.core.BitmapPool;

import cn.archko.pdf.utils.Util;

/**
 * @author: archko 2018/7/25 :12:43
 */
public class APDFView extends RelativeLayout {

    protected final Context mContext;
    private final Document mCore;

    private int mPageNumber;
    private Point mViewSize;    //size of visible view
    private Point mSize;   // Size of page at minimum zoom
    private float mSourceScale = 1f;

    private ImageView mEntireView; // Image rendered at minimum zoom
    private Bitmap mBitmap;
    private AsyncTask<Void, Void, Bitmap> mDrawTask;
    private ProgressBar mBusyIndicator;
    //private AKBitmapManager mBitmapManager;

    public APDFView(Context c, Document core, Point viewSize/*, AKBitmapManager bitmapManager*/) {
        super(c);
        mContext = c;
        mCore = core;
        mViewSize = viewSize;
        updateView();
        //mBitmapManager = bitmapManager;
    }

    public void updateView() {
        mEntireView = new ImageView(mContext);
        mEntireView.setScaleType(ImageView.ScaleType.MATRIX);
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        lp.addRule(RelativeLayout.CENTER_IN_PARENT);
        addView(mEntireView, lp);
        mBusyIndicator = new ProgressBar(mContext);
        mBusyIndicator.setIndeterminate(true);
        lp = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.CENTER_IN_PARENT);
        addView(mBusyIndicator, lp);
    }

    public int getPage() {
        return mPageNumber;
    }

    public void releaseResources() {
        if (null != mBitmap) {
            BitmapPool.getInstance().release(mBitmap);
            mBitmap = null;
        }

        if (mDrawTask != null) {
            mDrawTask.cancel(true);
            mDrawTask = null;
        }

        mEntireView.setImageBitmap(null);
        mBusyIndicator.setVisibility(GONE);
    }

    public Point caculateSize(PointF pageSize, float zoom) {
        float xr = mViewSize.x * zoom / pageSize.x;
        float yr = mViewSize.y * zoom / pageSize.y;
        mSourceScale = Math.max(xr, yr);
        Point newSize = new Point((int) (pageSize.x * mSourceScale), (int) (pageSize.y * mSourceScale));
        mSize = newSize;
        return mSize;
    }

    public void setPage(int page, PointF pageSize, float zoom) {
        mPageNumber = page;
        zoom = 1f;

        // Calculate scaled size that fits within the screen limits
        // This is the size at minimum zoom
        if (mSize == null || mSize.x != pageSize.x || mSize.y != pageSize.y) {
            caculateSize(pageSize, zoom);
        }
        int xOrigin = (int) (mSize.x * (zoom - 1f) / 2);
        Log.d("view", "view:" + mViewSize + " patchX:" + xOrigin + " mss:" + mSourceScale + " mSize:" + mSize + " zoom:" + zoom);

        /*if (null == mBitmap) {
            mBitmap = mBitmapManager.getBitmap(mPageNumber);
        }*/
        if (null != mBitmap) {
            mEntireView.setImageBitmap(mBitmap);
            //android.graphics.Matrix matrix = new android.graphics.Matrix();
            //matrix.setTranslate(-xOrigin / 2, 0);
            //matrix.postScale(((float) mSize.x) / mBitmap.getWidth(), ((float) mSize.y) / mBitmap.getHeight());
            //mEntireView.setImageMatrix(matrix);
            return;
        }

        if (mDrawTask != null) {
            mDrawTask.cancel(true);
            mDrawTask = null;
        }

        mDrawTask = getDrawPageTask(mSize.x, mSize.y, xOrigin, 0);
        //mDrawTask.execute();
        Util.execute(false, mDrawTask);
    }

    @SuppressLint("StaticFieldLeak")
    protected AsyncTask<Void, Void, Bitmap> getDrawPageTask(final int sizeX, final int sizeY,
                                                            final int xOrigin, final int yOrigin) {
        return new AsyncTask<Void, Void, Bitmap>() {

            @Override
            public void onPreExecute() {
                mBusyIndicator.setVisibility(VISIBLE);
            }

            @Override
            protected Bitmap doInBackground(Void... params) {
                Bitmap bitmap = BitmapPool.getInstance().acquire(sizeX,sizeY);//Bitmap.createBitmap(sizeX, sizeY, Bitmap.Config.ARGB_8888);

                Page page = mCore.loadPage(mPageNumber);
                Matrix ctm = new Matrix(mSourceScale);
                AndroidDrawDevice dev = new AndroidDrawDevice(bitmap, xOrigin, yOrigin, 0, 0, sizeX, sizeY);
                page.run(dev, ctm, (Cookie) null);
                dev.close();
                dev.destroy();
                return bitmap;
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                mBusyIndicator.setVisibility(GONE);
                mBitmap = bitmap;
                //mBitmapManager.setBitmap(mPageNumber, bitmap);
                mEntireView.setImageBitmap(mBitmap);
            }

        };
    }

}
