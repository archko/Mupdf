package org.vudroid.core.events;

/**
 * @author: archko 2016/12/20 :09:35
 */
public interface PageViewPresenter {

    int getPageCount();

    int getCurrentPageIndex();

    void goToPageIndex(int page);

    void showOutline();

    void back();

    String getTitle();

    void reflow();
}
