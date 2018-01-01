package org.vudroid.core.models;

import org.vudroid.core.events.BringUpZoomControlsEvent;
import org.vudroid.core.events.CurrentPageListener;
import org.vudroid.core.events.EventDispatcher;

public class CurrentPageModel extends EventDispatcher {
    private int currentPageIndex;
    private int pageCount;

    public void setPageCount(int pageCount) {
        this.pageCount = pageCount;
    }

    public int getPageCount() {
        return pageCount;
    }

    public int getCurrentPageIndex() {
        return currentPageIndex;
    }

    public void setCurrentPage(int currentPageIndex) {
        this.currentPageIndex = currentPageIndex;
    }

    public void setCurrentPageIndex(int currentPageIndex) {
        if (this.currentPageIndex != currentPageIndex) {
            this.currentPageIndex = currentPageIndex;
            dispatch(new CurrentPageListener.CurrentPageChangedEvent(currentPageIndex));
        }
    }

    public void toggleSeekControls() {
        dispatch(new BringUpZoomControlsEvent());
    }

    public void goToPageIndex(int currentPageIndex) {
        this.currentPageIndex = currentPageIndex;
        dispatch(new CurrentPageListener.CurrentPageChangedEvent(currentPageIndex));
    }
}
