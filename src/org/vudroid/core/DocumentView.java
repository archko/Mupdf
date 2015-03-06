package org.vudroid.core;

import android.content.Context;
import android.graphics.*;
import android.util.Log;
import android.util.SparseArray;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.OverScroller;
import android.widget.Scroller;
import org.vudroid.core.events.CurrentPageListener;
import org.vudroid.core.events.ZoomListener;
import org.vudroid.core.models.CurrentPageModel;
import org.vudroid.core.models.DecodingProgressModel;
import org.vudroid.core.models.ZoomModel;
import org.vudroid.core.multitouch.MultiTouchZoom;

public class DocumentView extends View implements ZoomListener {
    final ZoomModel zoomModel;
    private final CurrentPageModel currentPageModel;
    private CurrentPageModel mPageModel;
    DecodeService decodeService;
    private final SparseArray<Page> pages = new SparseArray<Page>();
    private boolean isInitialized = false;
    private int pageToGoTo;
    private int xToScroll;
    private float lastX;
    private float lastY;
    //private VelocityTracker velocityTracker;
    private final OverScroller scroller;
    DecodingProgressModel progressModel;
    private RectF viewRect;
    private boolean inZoom;
    private long lastDownEventTime;
    private static final int DOUBLE_TAP_TIME = 500;
    private MultiTouchZoom multiTouchZoom;

    private float downX = 0;
    private float downY = 0;
    private float maxExcursionY = 0;
    private boolean verticalScrollLock = true;
    private boolean lockedVertically = true;
    private final GestureDetector mGestureDetector;
    int mMargin=16;

    public void setPageModel(CurrentPageModel mPageModel) {
        this.mPageModel=mPageModel;
    }

    public DocumentView(Context context, final ZoomModel zoomModel, DecodingProgressModel progressModel, CurrentPageModel currentPageModel) {
        super(context);
        this.zoomModel = zoomModel;
        this.progressModel = progressModel;
        this.currentPageModel = currentPageModel;
        //setKeepScreenOn(true);
        scroller = new OverScroller(getContext(), new AccelerateDecelerateInterpolator());
        setFocusable(true);
        setFocusableInTouchMode(true);
        initMultiTouchZoomIfAvailable(zoomModel);
        mGestureDetector=new GestureDetector(new MySimpleOnGestureListener());
    }

    private void initMultiTouchZoomIfAvailable(ZoomModel zoomModel) {
        try {
            multiTouchZoom = (MultiTouchZoom) Class.forName("org.vudroid.core.multitouch.MultiTouchZoomImpl").getConstructor(ZoomModel.class).newInstance(zoomModel);
        } catch (Exception e) {
            System.out.println("Multi touch zoom is not available: " + e);
        }
    }

    public void setDecodeService(DecodeService decodeService) {
        this.decodeService = decodeService;
    }

    private void init() {
        if (isInitialized) {
            return;
        }
        final int width = decodeService.getEffectivePagesWidth();
        final int height = decodeService.getEffectivePagesHeight();
        for (int i = 0; i < decodeService.getPageCount(); i++) {
            pages.put(i, new Page(this, i));
            pages.get(i).setAspectRatio(width, height);
        }
        System.out.println("ViewDroidDecodeService:"+pages.size()+" page:"+pageToGoTo);
        isInitialized = true;
        invalidatePageSizes();
        goToPageImpl(pageToGoTo);
    }

    private void goToPageImpl(final int toPage) {
        int scroll=getScrollX();
        Log.d(VIEW_LOG_TAG, "goToPageImpl:"+xToScroll+" scroll:"+scroll);
        if (xToScroll!=0) {
            scroll=xToScroll;
            xToScroll=0;
        }
        scrollTo(scroll, pages.get(toPage).getTop());
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        // bounds could be not updated
        currentPageChanged();
        if (inZoom) {
            return;
        }
        // on scrollChanged can be called from scrollTo just after new layout applied so we should wait for relayout
        post(new Runnable() {
            public void run() {
                updatePageVisibility();
            }
        });
    }

    private void currentPageChanged() {
        post(new Runnable() {
            public void run() {
                //currentPageModel.setCurrentPageIndex(getCurrentPage());
                currentPageModel.setCurrentPage(getCurrentPage());
            }
        });
    }

    private void updatePageVisibility() {
        //for (Page page : pages.values()) {
        Page page;
        for(int i = 0; i < pages.size(); i++){
            page=pages.valueAt(i);
            page.updateVisibility();
        }
    }

    public void commitZoom() {
        //for (Page page : pages.values()) {
        Page page;
        for(int i = 0; i < pages.size(); i++){
            page=pages.valueAt(i);
            page.invalidate();
        }
        inZoom = false;
    }

    public void showDocument() {
        // use post to ensure that document view has width and height before decoding begin
        post(new Runnable() {
            public void run() {
                init();
                updatePageVisibility();
            }
        });
    }

    public void goToPage(int toPage) {
        if (isInitialized) {
            goToPageImpl(toPage);
        } else {
            pageToGoTo = toPage;
        }
    }

    public void goToPage(int toPage, int scrollX) {
        xToScroll=scrollX;
        if (isInitialized) {
            goToPageImpl(toPage);
        } else {
            pageToGoTo = toPage;
        }
    }

    public int getCurrentPage() {
        //for (Map.Entry<Integer, Page> entry : pages.entrySet()) {
        Page page;
        for(int i = 0; i < pages.size(); i++){
            page=pages.valueAt(i);
            if (page.isVisible()) {
                return pages.keyAt(i);
            }
        }
        return 0;
    }

    public void zoomChanged(float newZoom, float oldZoom) {
        inZoom = true;
        stopScroller();
        final float ratio = newZoom / oldZoom;
        invalidatePageSizes();
        scrollTo((int) ((getScrollX() + getWidth() / 2) * ratio - getWidth() / 2), (int) ((getScrollY() + getHeight() / 2) * ratio - getHeight() / 2));
        postInvalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        super.onTouchEvent(ev);

        if (mGestureDetector.onTouchEvent(ev)) {
            return true;
        }

        if (multiTouchZoom != null) {
            if (multiTouchZoom.onTouchEvent(ev)) {
                return true;
            }

            if (multiTouchZoom.isResetLastPointAfterZoom()) {
                setLastPosition(ev);
                multiTouchZoom.setResetLastPointAfterZoom(false);
            }
        }

        /*if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain();
        }
        velocityTracker.addMovement(ev);*/

        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                lockedVertically = verticalScrollLock;
                downX = ev.getX();
                downY = ev.getY();
                maxExcursionY = 0;
                stopScroller();
                setLastPosition(ev);
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                if (lockedVertically && unlocksVerticalLock(ev)) {
                    lockedVertically = false;
                }

                float excursionY = Math.abs(ev.getY() - downY);

                if (excursionY > maxExcursionY) {
                    maxExcursionY = excursionY;
                }

                int scrollX=(int) (lastX - ev.getX());
                if (lockedVertically) {
                    scrollX=0;
                }
                scrollBy(scrollX, (int) (lastY - ev.getY()));
                setLastPosition(ev);
                break;
            }
            case MotionEvent.ACTION_UP: {
                /*velocityTracker.computeCurrentVelocity(1000);

                float velocityX=velocityTracker.getXVelocity();
                if (lockedVertically) {
                    velocityX=0;
                }
                //final float excursionY=Math.abs(ev.getY()-downY);
                //if (excursionY>getHeight()/10) {
                    //scroller.fling(getScrollX(), getScrollY(), (int) velocityX, (int) -velocityTracker.getYVelocity(), getLeftLimit(), getRightLimit(), getTopLimit(), getBottomLimit());
                //}
                velocityTracker.recycle();
                velocityTracker=null;*/

                break;
            }
        }
        return true;
    }

    private void setLastPosition(MotionEvent ev) {
        lastX = ev.getX();
        lastY = ev.getY();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    lineByLineMoveTo(1);
                    return true;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    lineByLineMoveTo(-1);
                    return true;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    verticalDpadScroll(1);
                    return true;
                case KeyEvent.KEYCODE_DPAD_UP:
                    verticalDpadScroll(-1);
                    return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    private void verticalDpadScroll(int direction) {
        /*scroller.startScroll(getScrollX(), getScrollY(), 0, direction*getHeight()/2);
        invalidate();*/
        mCurrentFlingRunnable=new FlingRunnable(getContext());
        mCurrentFlingRunnable.startScroll(getScrollX(), getScrollY(), 0, direction*getHeight()/2);
        post(mCurrentFlingRunnable);
    }

    private void lineByLineMoveTo(int direction) {
        if (direction == 1 ? getScrollX() == getRightLimit() : getScrollX() == getLeftLimit()) {
            //scroller.startScroll(getScrollX(), getScrollY(), direction * (getLeftLimit() - getRightLimit()), (int) (direction * pages.get(getCurrentPage()).bounds.height() / 50));
            mCurrentFlingRunnable=new FlingRunnable(getContext());
            mCurrentFlingRunnable.startScroll(getScrollX(), getScrollY(), direction * (getLeftLimit() - getRightLimit()), (int) (direction * pages.get(getCurrentPage()).bounds.height() / 50));
            post(mCurrentFlingRunnable);
        } else {
            //scroller.startScroll(getScrollX(), getScrollY(), direction * getWidth() / 2, 0);
            mCurrentFlingRunnable=new FlingRunnable(getContext());
            mCurrentFlingRunnable.startScroll(getScrollX(), getScrollY(), direction * getWidth() / 2, 0);
            post(mCurrentFlingRunnable);
        }
        //invalidate();
    }

    private int getTopLimit() {
        return 0;
    }

    private int getLeftLimit() {
        return 0;
    }

    private int getBottomLimit() {
        return (int) pages.get(pages.size() - 1).bounds.bottom - getHeight();
    }

    private int getRightLimit() {
        return (int) (getWidth() * zoomModel.getZoom()) - getWidth();
    }

    @Override
    public void scrollTo(int x, int y) {
        super.scrollTo(Math.min(Math.max(x, getLeftLimit()), getRightLimit()), Math.min(Math.max(y, getTopLimit()), getBottomLimit()));
        viewRect = null;
    }

    RectF getViewRect() {
        if (viewRect == null) {
            viewRect = new RectF(getScrollX(), getScrollY(), getScrollX() + getWidth(), getScrollY() + getHeight());
        }
        return viewRect;
    }

    /*@Override
    public void computeScroll() {
        if (scroller.computeScrollOffset()) {
            scrollTo(scroller.getCurrX(), scroller.getCurrY());
        }
    }*/

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //for (Page page : pages.values()) {
        Page page;
        for(int i = 0; i < pages.size(); i++){
            page=pages.valueAt(i);
            page.draw(canvas);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        float scrollScaleRatio = getScrollScaleRatio();
        invalidatePageSizes();
        invalidateScroll(scrollScaleRatio);
        commitZoom();
    }

    void invalidatePageSizes() {
        if (!isInitialized) {
            return;
        }
        float heightAccum = 0;
        int width = getWidth();
        float zoom = zoomModel.getZoom();
        for (int i = 0; i < pages.size(); i++) {
            Page page = pages.get(i);
            float pageHeight = page.getPageHeight(width, zoom);
            page.setBounds(new RectF(0, heightAccum, width * zoom, heightAccum + pageHeight));
            heightAccum += pageHeight;
        }
    }

    private void invalidateScroll(float ratio) {
        if (!isInitialized) {
            return;
        }
        stopScroller();
        final Page page = pages.get(0);
        if (page == null || page.bounds == null) {
            return;
        }
        scrollTo((int) (getScrollX()*ratio), (int) (getScrollY()*ratio));
    }

    private float getScrollScaleRatio() {
        final Page page = pages.get(0);
        if (page == null || page.bounds == null) {
            return 0;
        }
        final float v = zoomModel.getZoom();
        return getWidth() * v / page.bounds.width();
    }

    private void stopScroller() {
        /*if (!scroller.isFinished()) {
            scroller.abortAnimation();
        }*/
        cancelFling();
    }

    public ZoomModel getZoomModel() {
        return zoomModel;
    }

    public void setVerticalScrollLock(boolean verticalScrollLock) {
        this.verticalScrollLock = verticalScrollLock;
    }

    private boolean unlocksVerticalLock(MotionEvent e) {
        float dx;
        float dy;

        dx = Math.abs(e.getX()-lastX);
        dy = Math.abs(e.getY()-lastY);

        if (dy > 0.25 * dx || maxExcursionY > 0.8 * dx) {
            return false;
        }

        return dx > getWidth()/10 || dx > getHeight()/10;
    }

    public void setScrollMargin(int margin) {
        mMargin=margin;
    }

    class MySimpleOnGestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            int height=getHeight();
            int top=height/4;
            int bottom=height*3/4;
            //Log.d(VIEW_LOG_TAG, "height:"+height+" y:"+e.getY()+" mMargin:"+mMargin);

            height=height-mMargin;
            if ((int) e.getY()<top) {
                //scroller.startScroll(getScrollX(), getScrollY(), 0, -height, 0);
                //invalidate();
                mCurrentFlingRunnable=new FlingRunnable(getContext());
                mCurrentFlingRunnable.startScroll(getScrollX(), getScrollY(), 0, -height, 0);
                post(mCurrentFlingRunnable);
            } else if ((int) e.getY()>bottom) {
                /*scroller.startScroll(getScrollX(), getScrollY(), 0, height, 0);
                invalidate();*/
                mCurrentFlingRunnable=new FlingRunnable(getContext());
                mCurrentFlingRunnable.startScroll(getScrollX(), getScrollY(), 0,  height, 0);
                post(mCurrentFlingRunnable);
            } else {
                currentPageModel.dispatch(new CurrentPageListener.CurrentPageChangedEvent(getCurrentPage()));
            }
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent ev) {
            return false;
        }

        @Override
        public boolean onDoubleTapEvent(MotionEvent ev) {
            if (ev.getEventTime() - lastDownEventTime < DOUBLE_TAP_TIME) {
                zoomModel.toggleZoomControls();
                if (null!=mPageModel) {
                    mPageModel.setCurrentPage(currentPageModel.getCurrentPageIndex());
                    mPageModel.setPageCount(decodeService.getPageCount());
                    mPageModel.toggleSeekControls();
                }
                return true;
            } else {
                lastDownEventTime = ev.getEventTime();
            }
            return false;
        }

        public boolean onDown(MotionEvent arg0) {
            return false;
        }

        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            //float velocityX=velocityTracker.getXVelocity();
            if (lockedVertically) {
                velocityX=0;
            }

            //scroller.fling(getScrollX(), getScrollY(), (int) velocityX, (int) -velocityY, getLeftLimit(), getRightLimit(), getTopLimit(), getBottomLimit());
            mCurrentFlingRunnable=new FlingRunnable(getContext());
            mCurrentFlingRunnable.fling(getScrollX(), getScrollY(), (int) velocityX, (int) -velocityY, getLeftLimit(), getRightLimit(), getTopLimit(), getBottomLimit());
            post(mCurrentFlingRunnable);
            return true;
        }

        public void onLongPress(MotionEvent e) {
        }

        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return false;
        }

        public void onShowPress(MotionEvent e) {
        }

        public boolean onSingleTapUp(MotionEvent e) {
            return false;
        }
    }
    //--------------------------------

    private FlingRunnable mCurrentFlingRunnable;

    private void cancelFling() {
        if (null!=mCurrentFlingRunnable) {
            mCurrentFlingRunnable.cancelFling();
            mCurrentFlingRunnable=null;
        }
    }

    private class FlingRunnable implements Runnable {

        public FlingRunnable(Context context) {
        }

        public void cancelFling() {
            scroller.forceFinished(true);
        }

        public void fling(int startX, int startY, int velocityX, int velocityY, int minX, int maxX, int minY, int maxY) {
            scroller.fling(startX, startY, velocityX, velocityY, minX, maxX, minY, maxY, 0, 0);
        }

        public void startScroll(int startX, int startY, int dx, int dy) {
            startScroll(startX, startY, dx, dy, 0);
        }

        public void startScroll(int startX, int startY, int dx, int dy, int duration) {
            scroller.startScroll(startX, startY, dx, dy, duration);
        }

        @Override
        public void run() {
            if (scroller.isFinished()) {
                return; // remaining post that should not be handled
            }

            if (scroller.computeScrollOffset()) {
                scrollTo(scroller.getCurrX(), scroller.getCurrY());
                //postInvalidate();

                // Post On animation
                postOnAnimation(this);
            }
        }
    }
}
