package cn.archko.pdf;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.text.TextPaint;
import android.util.Log;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.artifex.mupdf.fitz.Cookie;
import com.artifex.mupdf.fitz.Document;
import com.artifex.mupdf.fitz.Page;
import com.artifex.mupdf.fitz.android.AndroidDrawDevice;
import com.artifex.mupdf.viewer.CancellableAsyncTask;
import com.artifex.mupdf.viewer.CancellableTaskDefinition;
import com.artifex.mupdf.viewer.MuPDFCancellableTaskDefinition;

import org.vudroid.core.BitmapPool;

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
    private CancellableAsyncTask<Void, Bitmap> mDrawTask;
    private ProgressBar mBusyIndicator;
    private final TextPaint textPaint = textPaint();

    private TextPaint textPaint() {
        final TextPaint paint = new TextPaint();
        paint.setColor(Color.BLACK);
        paint.setAntiAlias(true);
        paint.setTextSize(32);
        paint.setTextAlign(Paint.Align.CENTER);
        return paint;
    }

    public APDFView(Context c, Document core, Point viewSize) {
        super(c);
        mContext = c;
        mCore = core;
        mViewSize = viewSize;
        updateView();
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

    /*@Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawText("Page " + (mPageNumber + 1), getMeasuredWidth() / 2, getHeight() / 2, textPaint);
    }*/

    public void releaseResources() {
        if (null != mBitmap) {
            BitmapPool.getInstance().release(mBitmap);
            mBitmap = null;
        }
        // Cancel pending render task
        if (mDrawTask != null) {
            mDrawTask.cancel();
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

        // Calculate scaled size that fits within the screen limits
        // This is the size at minimum zoom
        if (mSize == null || mSize.x != pageSize.x || mSize.y != pageSize.y) {
            caculateSize(pageSize, zoom);
        }
        int xOrigin = (int) (mSize.x * (zoom - 1f) / 2);
        Log.d("view", "view:" + mViewSize + " patchX:" + xOrigin + " mss:" + mSourceScale + " mSize:" + mSize + " zoom:" + zoom);

        if (null != mBitmap) {
            //Log.d("view", "mBitmap:" + mBitmap + " cp:" + mPageNumber);
            return;
        }

        // Cancel pending render task
        if (mDrawTask != null) {
            mDrawTask.cancel();
            mDrawTask = null;
        }

        // Render the page in the background
        mDrawTask = new CancellableAsyncTask<Void, Bitmap>(getDrawPageTask(mSize.x, mSize.y, xOrigin, 0)) {

            @Override
            public void onPreExecute() {
                mBusyIndicator.setVisibility(VISIBLE);
                //setBackgroundColor(BACKGROUND_COLOR);
                //mEntireView.setImageBitmap(null);
            }

            @Override
            public void onPostExecute(Bitmap bitmap) {
                mBusyIndicator.setVisibility(GONE);
                mBitmap = bitmap;
                mEntireView.setImageBitmap(mBitmap);
            }
        };

        mDrawTask.execute();
    }

    protected CancellableTaskDefinition<Void, Bitmap> getDrawPageTask(final int sizeX, final int sizeY,
                                                                      final int xOrigin, final int yOrigin) {
        return new MuPDFCancellableTaskDefinition<Void, Bitmap>() {
            @Override
            public Bitmap doInBackground(Cookie cookie, Void... params) {
                Bitmap bitmap = BitmapPool.getInstance().acquire(sizeX, sizeY);
                //mCore.drawPage(bitmap, mPageNumber, sizeX, sizeY, patchX, patchY, patchWidth, patchHeight, cookie);

                Page page = mCore.loadPage(mPageNumber);
                com.artifex.mupdf.fitz.Matrix ctm = new com.artifex.mupdf.fitz.Matrix(mSourceScale);
                AndroidDrawDevice dev = new AndroidDrawDevice(bitmap, xOrigin, yOrigin, 0, 0, sizeX, sizeY);
                page.run(dev, ctm, (Cookie) null);
                dev.close();
                dev.destroy();
                return bitmap;
            }
        };

    }

}
