package cn.archko.pdf;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Handler;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.artifex.mupdf.fitz.Cookie;
import com.artifex.mupdf.fitz.Link;
import com.artifex.mupdf.viewer.CancellableAsyncTask;
import com.artifex.mupdf.viewer.CancellableTaskDefinition;
import com.artifex.mupdf.viewer.MuPDFCancellableTaskDefinition;
import com.artifex.mupdf.viewer.MuPDFCore;

import org.vudroid.core.BitmapPool;

// Make our ImageViews opaque to optimize redraw
class OpaqueImageView extends ImageView {

	public OpaqueImageView(Context context) {
		super(context);
	}

	@Override
	public boolean isOpaque() {
		return true;
	}
}

public class PDFView extends ViewGroup {
	private final MuPDFCore mCore;

	private static final int HIGHLIGHT_COLOR = 0x80cc6600;
	private static final int LINK_COLOR = 0x800066cc;
	private static final int BOX_COLOR = 0xFF4444FF;
	//private static final int BACKGROUND_COLOR = 0xFFFFFFFF;
	private static final int PROGRESS_DIALOG_DELAY = 200;

	protected final Context mContext;

	protected     int       mPageNumber;
	private       Point     mParentSize;
	protected     Point     mSize;   // Size of page at minimum zoom
	protected     float     mSourceScale;

	private       ImageView mEntire; // Image rendered at minimum zoom
	private       Bitmap    mEntireBm;
	private       Matrix    mEntireMat;
	//private       AsyncTask<Void,Void,Link[]> mGetLinkInfo;
	private       CancellableAsyncTask<Void, Bitmap> mDrawEntire;

	private       RectF     mSearchBoxes[];
	protected     Link      mLinks[];
	//private       View      mSearchView;
	private       boolean   mIsBlank;
	private       boolean   mHighlightLinks;

	//private       ProgressBar mBusyIndicator;
	private final Handler   mHandler = new Handler();
	private int currentPage;
	private Paint mSearchPaint;

	public PDFView(Context c, MuPDFCore core, Point parentSize) {
		super(c);
		mContext = c;
		mCore = core;
		mParentSize = parentSize;
		//setBackgroundColor(BACKGROUND_COLOR);
		//mEntireBm = Bitmap.createBitmap(parentSize.x, parentSize.y, Config.ARGB_8888);
		mEntireMat = new Matrix();
		mSearchPaint = new Paint();
		mEntire = new OpaqueImageView(mContext);
		mEntire.setScaleType(ImageView.ScaleType.MATRIX);
		addView(mEntire);
	}

	private void reinit() {
		Log.d("view", "reinit oldmEntireBm:" + mEntireBm+" cp:"+currentPage);
		if (null != mEntireBm) {
			BitmapPool.getInstance().release(mEntireBm);
			mEntireBm = null;
		}
		// Cancel pending render task
		if (mDrawEntire != null) {
			mDrawEntire.cancel();
			mDrawEntire = null;
		}

		/*if (mGetLinkInfo != null) {
			mGetLinkInfo.cancel(true);
			mGetLinkInfo = null;
		}*/

		mIsBlank = true;
		mPageNumber = 0;

		if (mSize == null) {
			mSize = mParentSize;
		}

		if (mEntire != null) {
			mEntire.setImageBitmap(null);
		}

		mSearchBoxes = null;
		mLinks = null;
	}

	public void releaseResources() {
		reinit();

		/*if (mBusyIndicator != null) {
			removeView(mBusyIndicator);
			mBusyIndicator = null;
		}*/
	}

	public void setPage(int page, PointF size) {
		mIsBlank = false;
		// Highlights may be missing because mIsBlank was true on last draw
		//if (mSearchView != null) {
		//    mSearchView.invalidate();
        //}

		mPageNumber = page;

		// Calculate scaled size that fits within the screen limits
		// This is the size at minimum zoom
		float xr = mParentSize.x / size.x;
		float yr = mParentSize.y / size.y;
		mSourceScale = Math.max(xr, yr);
		Point newSize = new Point((int)(size.x*mSourceScale), (int)(size.y*mSourceScale));
		mSize = newSize;
		//Log.d("view", "mParentSize:" + mParentSize + " xr:" + xr + " yr:" + yr + " mss:" + mSourceScale + " mSize:" + mSize);

		currentPage = page;

		// Get the link info in the background
		/*mGetLinkInfo = new AsyncTask<Void,Void,Link[]>() {
			protected Link[] doInBackground(Void... v) {
				return getLinkInfo();
			}

			protected void onPostExecute(Link[] v) {
				mLinks = v;
				if (mSearchView != null)
					mSearchView.invalidate();
			}
		};

		mGetLinkInfo.execute();*/

		if (null != mEntireBm) {
			Log.d("view", "mEntireBm:" + mEntireBm+" cp:"+currentPage);
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
				//setBackgroundColor(BACKGROUND_COLOR);
				mEntire.setImageBitmap(null);

				/*if (mBusyIndicator == null) {
					mBusyIndicator = new ProgressBar(mContext);
					mBusyIndicator.setIndeterminate(true);
					addView(mBusyIndicator);
					mBusyIndicator.setVisibility(INVISIBLE);
					mHandler.postDelayed(new Runnable() {
						public void run() {
							if (mBusyIndicator != null)
								mBusyIndicator.setVisibility(VISIBLE);
						}
					}, PROGRESS_DIALOG_DELAY);
				}*/
			}

            @Override
            public void onPostExecute(Bitmap bitmap) {
                //removeView(mBusyIndicator);
                //mBusyIndicator = null;
                //setBackgroundColor(Color.TRANSPARENT);
                mEntireBm = bitmap;
                mEntire.setImageBitmap(mEntireBm);
            }
        };

		mDrawEntire.execute();

		/*if (mSearchView == null) {
			mSearchView = new View(mContext) {
				@Override
				protected void onDraw(final Canvas canvas) {
					super.onDraw(canvas);
					// Work out current total scale factor
					// from source to view
					final float scale = mSourceScale*(float)getWidth()/(float)mSize.x;

					if (!mIsBlank && mSearchBoxes != null) {
						mSearchPaint.setColor(HIGHLIGHT_COLOR);
						for (RectF rect : mSearchBoxes)
							canvas.drawRect(rect.left*scale, rect.top*scale,
									rect.right*scale, rect.bottom*scale,
									mSearchPaint);
					}

					if (!mIsBlank && mLinks != null && mHighlightLinks) {
						mSearchPaint.setColor(LINK_COLOR);
						for (Link link : mLinks)
							canvas.drawRect(link.bounds.x0*scale, link.bounds.y0*scale,
									link.bounds.x1*scale, link.bounds.y1*scale,
									mSearchPaint);
					}
				}
			};

			addView(mSearchView);
		}*/
		//requestLayout();
	}

	public void setSearchBoxes(RectF searchBoxes[]) {
		mSearchBoxes = searchBoxes;
		/*if (mSearchView != null) {
		    mSearchView.invalidate();
        }*/
	}

	public void setLinkHighlighting(boolean f) {
		mHighlightLinks = f;
		/*if (mSearchView != null) {
		    mSearchView.invalidate();
        }*/
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int x, y;
		switch(MeasureSpec.getMode(widthMeasureSpec)) {
		case MeasureSpec.UNSPECIFIED:
			x = mSize.x;
			break;
		default:
			x = MeasureSpec.getSize(widthMeasureSpec);
		}
		switch(MeasureSpec.getMode(heightMeasureSpec)) {
		case MeasureSpec.UNSPECIFIED:
			y = mSize.y;
			break;
		default:
			y = MeasureSpec.getSize(heightMeasureSpec);
		}

		setMeasuredDimension(x, y);

		/*if (mBusyIndicator != null) {
			int limit = Math.min(mParentSize.x, mParentSize.y)/2;
			mBusyIndicator.measure(MeasureSpec.AT_MOST | limit, MeasureSpec.AT_MOST | limit);
		}*/
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		int w = right-left;
		int h = bottom-top;

		if (mEntire != null) {
			if (mEntire.getWidth() != w || mEntire.getHeight() != h) {
				mEntireMat.setScale(w/(float)mSize.x, h/(float)mSize.y);
				mEntire.setImageMatrix(mEntireMat);
				mEntire.invalidate();
			}
			mEntire.layout(0, 0, w, h);
		}

		//if (mSearchView != null) {
		//	mSearchView.layout(0, 0, w, h);
		//}

		/*if (mBusyIndicator != null && mBusyIndicator.getVisibility() == VISIBLE) {
			int bw = mBusyIndicator.getMeasuredWidth();
			int bh = mBusyIndicator.getMeasuredHeight();

			mBusyIndicator.layout((w-bw)/2, (h-bh)/2, (w+bw)/2, (h+bh)/2);
		}*/
	}

	public int getPage() {
		return mPageNumber;
	}

	@Override
	public boolean isOpaque() {
		return true;
	}

	public Link hitLink(float x, float y) {
		// Since link highlighting was implemented, the super class
		// PageView has had sufficient information to be able to
		// perform this method directly. Making that change would
		// make MuPDFCore.hitLinkPage superfluous.
		float scale = mSourceScale*(float)getWidth()/(float)mSize.x;
		float docRelX = (x - getLeft())/scale;
		float docRelY = (y - getTop())/scale;

		if (mLinks != null)
			for (Link l: mLinks)
				if (l.bounds.contains(docRelX, docRelY))
					return l;
		return null;
	}

	protected CancellableTaskDefinition<Void, Bitmap> getDrawPageTask(final int sizeX, final int sizeY,
			final int patchX, final int patchY, final int patchWidth, final int patchHeight) {
		return new MuPDFCancellableTaskDefinition<Void, Bitmap>() {
			@Override
			public Bitmap doInBackground(Cookie cookie, Void ... params) {
                Bitmap bitmap = BitmapPool.getInstance().acquire(sizeX, sizeY);
                mCore.drawPage(bitmap, mPageNumber, sizeX, sizeY, patchX, patchY, patchWidth, patchHeight, cookie);
                return bitmap;
			}
		};

	}

	protected Link[] getLinkInfo() {
		return mCore.getPageLinks(mPageNumber);
	}
}
