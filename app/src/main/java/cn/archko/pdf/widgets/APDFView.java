package cn.archko.pdf.widgets;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.util.Log;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.artifex.mupdf.fitz.Cookie;
import com.artifex.mupdf.fitz.Document;
import com.artifex.mupdf.fitz.Matrix;
import com.artifex.mupdf.fitz.Page;
import com.artifex.mupdf.fitz.android.AndroidDrawDevice;

import org.ebookdroid.core.crop.PageCropper;
import org.vudroid.core.BitmapPool;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import cn.archko.pdf.common.AKBitmapManager;
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
    private AKBitmapManager mBitmapManager;

    public APDFView(Context c, Document core, Point viewSize, AKBitmapManager bitmapManager) {
        super(c);
        mContext = c;
        mCore = core;
        mViewSize = viewSize;
        updateView();
        mBitmapManager = bitmapManager;
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
            //BitmapPool.getInstance().release(mBitmap);
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
        float xr = mViewSize.x / pageSize.x;
        float yr = mViewSize.y / pageSize.y;
        mSourceScale = Math.max(xr, yr) * zoom;
        Point newSize = new Point((int) (pageSize.x * mSourceScale), (int) (pageSize.y * mSourceScale));
        mSize = newSize;
        return mSize;
    }

    public void setPage(int page, PointF pageSize, float zoom) {
        mPageNumber = page;

        boolean refresh = false;
        if (mSize == null || mSize.x != pageSize.x || mSize.y != pageSize.y) {
            refresh = true;
            caculateSize(pageSize, zoom);
        }
        int xOrigin = (int) ((mSize.x - mViewSize.x) / 2);
        Log.d("view", "view:" + mViewSize + " patchX:" + xOrigin + " mss:" + mSourceScale + " mSize:" + mSize + " zoom:" + zoom);

        if (null == mBitmap) {
            mBitmap = mBitmapManager.getBitmap(mPageNumber);
        }
        if (null != mBitmap) {
            android.graphics.Matrix matrix = new android.graphics.Matrix();
            matrix.postTranslate(-xOrigin, 0);
            matrix.postScale(((float) mSize.x) / mBitmap.getWidth(), ((float) mSize.y) / mBitmap.getHeight());
            mEntireView.setImageMatrix(matrix);
            mEntireView.setImageBitmap(mBitmap);
            if (!refresh) {
                return;
            }
        }

        if (mDrawTask != null) {
            mDrawTask.cancel(true);
            mDrawTask = null;
        }

        mDrawTask = getDrawPageTask(mSize.x, mSize.y, xOrigin, 0);
        //mDrawTask.execute();
        Util.execute(true, mDrawTask);
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
                Bitmap bitmap = BitmapPool.getInstance().acquire(sizeX, sizeY);//Bitmap.createBitmap(sizeX, sizeY, Bitmap.Config.ARGB_8888);

                Page page = mCore.loadPage(mPageNumber);
                Matrix ctm = new Matrix(mSourceScale);
                AndroidDrawDevice dev = new AndroidDrawDevice(bitmap, xOrigin, yOrigin, 0, 0, sizeX, sizeY);
                page.run(dev, ctm, (Cookie) null);
                dev.close();
                dev.destroy();
                page.destroy();
                return bitmap;
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                mBusyIndicator.setVisibility(GONE);
                mBitmap = bitmap;
                mBitmapManager.setBitmap(mPageNumber, bitmap);
                mEntireView.getImageMatrix().reset();
                mEntireView.setImageBitmap(mBitmap);
            }

        };
    }

    private RectF getCropRect(Bitmap bitmap) {
        //long start = SystemClock.uptimeMillis();
        //ByteBuffer byteBuffer = PageCropper.create(bitmap.getByteCount()).order(ByteOrder.nativeOrder());
        //bitmap.copyPixelsToBuffer(byteBuffer);
        //RectF rectF = PageCropper.getCropBounds(byteBuffer, bitmap.getWidth(), bitmap.getHeight(), new RectF(0, 0, bitmap.getWidth(), bitmap.getHeight()));
        //Log.d("test", String.format("%s,%s,%s,%s", bitmap.getWidth(), bitmap.getHeight(), (SystemClock.uptimeMillis() - start), rectF));

        //view: view:Point(1920, 1080) patchX:71 mss:6.260591 mSize:Point(2063, 3066) zoom:1.0749608
        //test: 2063,3066,261,RectF(85.0, 320.0, 1743.0, 2736.0)
        return null;
    }

}
