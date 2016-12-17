package cn.archko.pdf;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.PointF;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.artifex.mupdfdemo.CancellableAsyncTask;
import com.artifex.mupdfdemo.CancellableTaskDefinition;
import com.artifex.mupdfdemo.MuPDFCancellableTaskDefinition;
import com.artifex.mupdfdemo.MuPDFCore;
import com.artifex.mupdfdemo.MuPDFReaderView;

/**
 * @author: archko 2016/5/13 :13:05
 */
public class PDFView extends ViewGroup implements ScaleGestureDetector.OnScaleGestureListener {

    class OpaqueImageView extends ImageView {

        public OpaqueImageView(Context context) {
            super(context);
        }

        @Override
        public boolean isOpaque() {
            return true;
        }
    }

    private static final float MIN_SCALE = 1.0f;
    private static final float MAX_SCALE = 5.0f;
    private static final float REFLOW_SCALE_FACTOR = 0.5f;

    private static final int BACKGROUND_COLOR = 0xFFFFFFFF;
    private static final int PROGRESS_DIALOG_DELAY = 200;

    private ScaleGestureDetector mScaleGestureDetector;
    private boolean mScaling;    // Whether the user is currently pinch zooming
    private float mScale = 1.0f;
    protected final Context mContext;

    protected int mPageNumber;
    private Point mParentSize;
    protected Point mSize;   // Size of page at minimum zoom
    private ImageView mEntire; // Image rendered at minimum zoom
    private Bitmap mEntireBm;
    private Matrix mEntireMat;
    private final MuPDFCore mCore;
    private CancellableAsyncTask<Void, Void> mDrawEntire;
    private Handler mHandler = new Handler();

    public PDFView(Context c, MuPDFCore core) {
        super(c);
        this.mCore = core;
        mContext = c;
        setBackgroundColor(BACKGROUND_COLOR);
        mEntireMat = new Matrix();
    }

    public void setMode(MuPDFReaderView.Mode mode) {
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        float previousScale = mScale;
        float scale_factor = REFLOW_SCALE_FACTOR;
        float min_scale = MIN_SCALE * scale_factor;
        float max_scale = MAX_SCALE * scale_factor;
        mScale = Math.min(Math.max(mScale * detector.getScaleFactor(), min_scale), max_scale);

        return true;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        return false;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {

    }

    /*@Override
    public boolean onTouchEvent(MotionEvent event) {
        mScaleGestureDetector.onTouchEvent(event);
        return false;
    }*/

    protected CancellableTaskDefinition<Void, Void> getDrawPageTask(final Bitmap bm, final int sizeX, final int sizeY,
                                                                    final int patchX, final int patchY, final int patchWidth, final int patchHeight) {
        return new MuPDFCancellableTaskDefinition<Void, Void>(mCore) {
            @Override
            public Void doInBackground(MuPDFCore.Cookie cookie, Void... params) {
                // Workaround bug in Android Honeycomb 3.x, where the bitmap generation count
                // is not incremented when drawing.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB &&
                        Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH)
                    bm.eraseColor(0);
                mCore.drawPage(bm, mPageNumber, sizeX, sizeY, patchX, patchY, patchWidth, patchHeight, cookie);
                return null;
            }
        };

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int x, y;
        switch (View.MeasureSpec.getMode(widthMeasureSpec)) {
            case View.MeasureSpec.UNSPECIFIED:
                x = mSize.x;
                break;
            default:
                x = View.MeasureSpec.getSize(widthMeasureSpec);
        }
        /*switch(View.MeasureSpec.getMode(heightMeasureSpec)) {
        case View.MeasureSpec.UNSPECIFIED:
			y = mSize.y;
			break;
		default:
			y = View.MeasureSpec.getSize(heightMeasureSpec);
		}*/
        y = x / mSize.x * mSize.y;
        Log.d(VIEW_LOG_TAG, "measure:" + x + " y:" + y);

        setMeasuredDimension(x, y);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int w = right - left;
        int h = bottom - top;

        if (mEntire != null) {
            if (mEntire.getWidth() != w || mEntire.getHeight() != h) {
                mEntireMat.setScale(w / (float) mSize.x, h / (float) mSize.y);
                mEntire.setImageMatrix(mEntireMat);
                mEntire.invalidate();
            }
            mEntire.layout(0, 0, w, h);
        }
    }

    private void reinit() {
        // Cancel pending render task
        if (mDrawEntire != null) {
            mDrawEntire.cancel();
            mDrawEntire = null;
        }

        //mPageNumber = 0;

        if (mSize == null)
            mSize = mParentSize;

        if (mEntire != null) {
            mEntire.setImageBitmap(null);
            mEntire.invalidate();
        }
    }

    public void releaseResources() {
        reinit();
    }

    public void releaseBitmaps() {
        //reinit();

        //  recycle bitmaps before releasing them.
        /*if (mEntireBm != null)
            mEntireBm.recycle();*/
        mEntireBm = null;
        if (mEntire != null) {
            mEntire.setImageBitmap(null);
            mEntire.invalidate();
        }
    }

    public void setPage(int page, PointF size) {
        Log.d(VIEW_LOG_TAG, "setPage:" + page + " size:" + size);
        if (null == mParentSize) {
            mParentSize = new Point((int) size.x, (int) size.y);
        } else {
            mParentSize.x = (int) size.x;
            mParentSize.y = (int) size.y;
        }

        if (mEntire == null) {
            mEntire = new OpaqueImageView(mContext);
            mEntire.setScaleType(ImageView.ScaleType.MATRIX);
            mEntire.setAdjustViewBounds(true);
            addView(mEntire);
        }

        if (mPageNumber == page && null != mEntireBm) {
            mEntire.setImageBitmap(mEntireBm);
            mEntire.invalidate();
            return;
        }

        // Cancel pending render task
        if (mDrawEntire != null) {
            mDrawEntire.cancel();
            mDrawEntire = null;
        }

        mPageNumber = page;

        // Calculate scaled size that fits within the screen limits
        // This is the size at minimum zoom
        /*mSourceScale = Math.min(mParentSize.x / size.x, mParentSize.y / size.y);
        Point newSize = new Point((int) (size.x * mSourceScale), (int) (size.y * mSourceScale));
        mSize = newSize;*/
        mSize = mParentSize;

        if (null == mEntireBm) {
            mEntireBm = Bitmap.createBitmap(mSize.x, mSize.y, Bitmap.Config.ARGB_8888);
        }
        //mEntireBm.eraseColor(0);
        mEntire.invalidate();

        // Render the page in the background
        mDrawEntire = new CancellableAsyncTask<Void, Void>(getDrawPageTask(mEntireBm, mSize.x, mSize.y, 0, 0, mSize.x, mSize.y)) {

            @Override
            public void onPreExecute() {
                //setBackgroundColor(BACKGROUND_COLOR);
            }

            @Override
            public void onPostExecute(Void result) {
                mEntire.setImageBitmap(mEntireBm);
                mEntire.invalidate();
                //setBackgroundColor(Color.TRANSPARENT);
            }
        };

        mDrawEntire.execute();

        requestLayout();
    }
}
