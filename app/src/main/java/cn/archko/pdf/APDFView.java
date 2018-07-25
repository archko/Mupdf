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
import com.artifex.mupdf.viewer.CancellableAsyncTask;
import com.artifex.mupdf.viewer.CancellableTaskDefinition;
import com.artifex.mupdf.viewer.MuPDFCancellableTaskDefinition;
import com.artifex.mupdf.viewer.MuPDFCore;

import org.vudroid.core.BitmapPool;

/**
 * @author: archko 2018/7/25 :12:43
 */
public class APDFView extends RelativeLayout {

    private final MuPDFCore mCore;

    protected final Context mContext;

    protected int mPageNumber;
    private Point mParentSize;
    protected Point mSize;   // Size of page at minimum zoom
    protected float mSourceScale;

    private ImageView mEntire; // Image rendered at minimum zoom
    private Bitmap mEntireBm;
    private CancellableAsyncTask<Void, Bitmap> mDrawEntire;
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

    public APDFView(Context c, MuPDFCore core, Point parentSize) {
        super(c);
        mContext = c;
        mCore = core;
        mParentSize = parentSize;
        updateView();
    }

    public void updateView() {
        mEntire = new ImageView(mContext);
        mEntire.setScaleType(ImageView.ScaleType.MATRIX);
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        lp.addRule(RelativeLayout.CENTER_IN_PARENT);
        addView(mEntire, lp);
        mBusyIndicator = new ProgressBar(mContext);
        mBusyIndicator.setIndeterminate(true);
        lp = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.CENTER_IN_PARENT);
        addView(mBusyIndicator, lp);
    }

    /*@Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawText("Page " + (mPageNumber + 1), getMeasuredWidth() / 2, getHeight() / 2, textPaint);
    }*/

    public void releaseResources() {
        if (null != mEntireBm) {
            BitmapPool.getInstance().release(mEntireBm);
            mEntireBm = null;
        }
        // Cancel pending render task
        if (mDrawEntire != null) {
            mDrawEntire.cancel();
            mDrawEntire = null;
        }

        mEntire.setImageBitmap(null);
        mBusyIndicator.setVisibility(GONE);
    }

    public void setPage(int page, PointF size) {
        mPageNumber = page;

        // Calculate scaled size that fits within the screen limits
        // This is the size at minimum zoom
        if (mSize == null || mSize.x != size.x || mSize.y != size.y) {
            float xr = mParentSize.x / size.x;
            float yr = mParentSize.y / size.y;
            mSourceScale = Math.max(xr, yr);
            Point newSize = new Point((int) (size.x * mSourceScale), (int) (size.y * mSourceScale));
            mSize = newSize;
        }
        //Log.d("view", "mParentSize:" + mParentSize + " xr:" + xr + " yr:" + yr + " mss:" + mSourceScale + " mSize:" + mSize);

        if (null != mEntireBm) {
            Log.d("view", "mEntireBm:" + mEntireBm + " cp:" + mPageNumber);
            return;
        }

        // Cancel pending render task
        if (mDrawEntire != null) {
            mDrawEntire.cancel();
            mDrawEntire = null;
        }

        // Render the page in the background
        mDrawEntire = new CancellableAsyncTask<Void, Bitmap>(getDrawPageTask(mSize.x, mSize.y, 0, 0, mSize.x, mSize.y)) {

            @Override
            public void onPreExecute() {
                mBusyIndicator.setVisibility(VISIBLE);
                //setBackgroundColor(BACKGROUND_COLOR);
                //mEntire.setImageBitmap(null);
            }

            @Override
            public void onPostExecute(Bitmap bitmap) {
                mBusyIndicator.setVisibility(GONE);
                mEntireBm = bitmap;
                mEntire.setImageBitmap(mEntireBm);
            }
        };

        mDrawEntire.execute();
    }

    public int getPage() {
        return mPageNumber;
    }

    protected CancellableTaskDefinition<Void, Bitmap> getDrawPageTask(final int sizeX, final int sizeY,
                                                                      final int patchX, final int patchY, final int patchWidth, final int patchHeight) {
        return new MuPDFCancellableTaskDefinition<Void, Bitmap>() {
            @Override
            public Bitmap doInBackground(Cookie cookie, Void... params) {
                Bitmap bitmap = BitmapPool.getInstance().acquire(sizeX, sizeY);
                mCore.drawPage(bitmap, mPageNumber, sizeX, sizeY, patchX, patchY, patchWidth, patchHeight, cookie);
                return bitmap;
            }
        };

    }

}
