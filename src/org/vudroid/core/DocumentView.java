package org.vudroid.core;

import android.content.Context;
import android.graphics.*;
import android.net.Uri;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.widget.Scroller;
import cx.hell.android.pdfviewpro.Bookmark;
import cx.hell.android.pdfviewpro.BookmarkEntry;
import org.vudroid.core.events.ZoomListener;
import org.vudroid.core.models.CurrentPageModel;
import org.vudroid.core.models.DecodingProgressModel;
import org.vudroid.core.models.ZoomModel;
import org.vudroid.core.multitouch.MultiTouchZoom;
import org.vudroid.core.multitouch.MultiTouchZoomImpl;

import java.util.HashMap;
import java.util.Map;

public class DocumentView extends View implements ZoomListener {
    final ZoomModel zoomModel;
    private final CurrentPageModel currentPageModel;
    DecodeService decodeService;
    private final HashMap<Integer, Page> pages = new HashMap<Integer, Page>();
    private boolean isInitialized = false;
    private int pageToGoTo;
    private float lastX;
    private float lastY;
    private VelocityTracker velocityTracker;
    private final Scroller scroller;
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

    public DocumentView(Context context, final ZoomModel zoomModel, DecodingProgressModel progressModel, CurrentPageModel currentPageModel) {
        super(context);
        this.zoomModel = zoomModel;
        this.progressModel = progressModel;
        this.currentPageModel = currentPageModel;
        setKeepScreenOn(true);
        scroller = new Scroller(getContext());
        setFocusable(true);
        setFocusableInTouchMode(true);
        initMultiTouchZoomIfAvailable(zoomModel);
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
        scrollTo(0, pages.get(toPage).getTop());
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        // bounds could be not updated
        post(new Runnable() {
            public void run() {
                currentPageModel.setCurrentPageIndex(getCurrentPage());
            }
        });
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

    private void updatePageVisibility() {
        for (Page page : pages.values()) {
            page.updateVisibility();
        }
    }

    public void commitZoom() {
        for (Page page : pages.values()) {
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

    public int getCurrentPage() {
        for (Map.Entry<Integer, Page> entry : pages.entrySet()) {
            if (entry.getValue().isVisible()) {
                return entry.getKey();
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

        if (multiTouchZoom != null) {
            if (multiTouchZoom.onTouchEvent(ev)) {
                return true;
            }

            if (multiTouchZoom.isResetLastPointAfterZoom()) {
                setLastPosition(ev);
                multiTouchZoom.setResetLastPointAfterZoom(false);
            }
        }

        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain();
        }
        velocityTracker.addMovement(ev);

        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                lockedVertically = verticalScrollLock;
                downX = ev.getX();
                downY = ev.getY();
                maxExcursionY = 0;
                stopScroller();
                setLastPosition(ev);
                if (ev.getEventTime() - lastDownEventTime < DOUBLE_TAP_TIME) {
                    zoomModel.toggleZoomControls();
                } else {
                    lastDownEventTime = ev.getEventTime();
                }
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
                velocityTracker.computeCurrentVelocity(1000);

                float velocityX=velocityTracker.getXVelocity();
                if (lockedVertically) {
                    velocityX=0;
                }
                //final float excursionY=Math.abs(ev.getY()-downY);
                //if (excursionY>getHeight()/10) {
                    scroller.fling(getScrollX(), getScrollY(), (int) velocityX, (int) -velocityTracker.getYVelocity(), getLeftLimit(), getRightLimit(), getTopLimit(), getBottomLimit());
                //}
                velocityTracker.recycle();
                velocityTracker=null;

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
        scroller.startScroll(getScrollX(), getScrollY(), 0, direction * getHeight() / 2);
        invalidate();
    }

    private void lineByLineMoveTo(int direction) {
        if (direction == 1 ? getScrollX() == getRightLimit() : getScrollX() == getLeftLimit()) {
            scroller.startScroll(getScrollX(), getScrollY(), direction * (getLeftLimit() - getRightLimit()), (int) (direction * pages.get(getCurrentPage()).bounds.height() / 50));
        } else {
            scroller.startScroll(getScrollX(), getScrollY(), direction * getWidth() / 2, 0);
        }
        invalidate();
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

    @Override
    public void computeScroll() {
        if (scroller.computeScrollOffset()) {
            scrollTo(scroller.getCurrX(), scroller.getCurrY());
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (Page page : pages.values()) {
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
        scrollTo((int) (getScrollX() * ratio), (int) (getScrollY() * ratio));
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
        if (!scroller.isFinished()) {
            scroller.abortAnimation();
        }
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

    /*@Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (oldw == 0 && oldh == 0) {
            goToBookmark();
        }
    }*/


    private BookmarkEntry bookmarkToRestore = null;

    public void setBookmarkToRestore(BookmarkEntry bookmarkToRestore) {
        this.bookmarkToRestore=bookmarkToRestore;
    }

    public void goToBookmark() {
        if (bookmarkToRestore == null || bookmarkToRestore.absoluteZoomLevel == 0
            || bookmarkToRestore.page < 0
            || bookmarkToRestore.page >= decodeService.getPageCount()) {
            //int top  = height / 2;
            //int left = width / 2;
        }
        else {
            float zoomLevel = bookmarkToRestore.absoluteZoomLevel;
            zoomModel.setZoom(zoomLevel/1000);
            int rotation = bookmarkToRestore.rotation;
            int currentPage = bookmarkToRestore.page;
            int top = bookmarkToRestore.offsetY;
            int left = bookmarkToRestore.offsetX;
        }
    }
}
