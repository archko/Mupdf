package cn.archko.pdf;

import android.net.Uri;
import android.util.Log;

import cx.hell.android.pdfviewpro.APVApplication;
import cx.hell.android.pdfviewpro.Bookmark;
import cx.hell.android.pdfviewpro.BookmarkEntry;

import static cn.archko.pdf.AKRecent.TAG;

/**
 * @author: archko 2018/7/22 :12:43
 */
public class PDFBookmarkManager {

    private BookmarkEntry bookmarkToRestore = null;

    public BookmarkEntry getBookmarkToRestore() {
        return bookmarkToRestore;
    }

    public void setStartBookmark(String mPath) {
        AKProgress progress = AKRecent.getInstance(APVApplication.getInstance()).readRecentFromDb(mPath);
        if (null != progress) {
            BookmarkEntry entry = new BookmarkEntry(progress.bookmarkEntry);
            bookmarkToRestore = entry;
        }
    }

    public int restoreBookmark(int pageCount) {
        if (bookmarkToRestore == null) {
            return 0;
        }
        int currentPage = 0;

        if (bookmarkToRestore.numberOfPages != pageCount || bookmarkToRestore.page > pageCount) {
            bookmarkToRestore = null;
            return currentPage;
        }

        if (0 < bookmarkToRestore.page) {
            currentPage = bookmarkToRestore.page;
        }
        return currentPage;
    }

    public void saveCurrentPage(String path, int pageCount, int currentPage, float zoom, int scrollX, int scrollY) {
        String filePath = Uri.decode(path);
        BookmarkEntry entry = toBookmarkEntry(pageCount, currentPage, zoom, scrollX, scrollY);
        Bookmark b = new Bookmark(APVApplication.getInstance()).open();
        b.setLast(filePath, entry);
        b.close();
        Log.i(TAG, String.format("last page saved for %s entry:%s", filePath, entry));
        AKRecent.getInstance(APVApplication.getInstance()).addAsyncToDB(filePath, entry.page, entry.numberOfPages, entry.toString(),
                new DataListener() {
                    @Override
                    public void onSuccess(Object... args) {
                        Log.i(TAG, "onSuccess");
                    }

                    @Override
                    public void onFailed(Object... args) {
                        Log.i(TAG, "onFailed");
                    }
                });
    }

    BookmarkEntry toBookmarkEntry(int pageCount, int currentPage, float zoom, int scrollX, int scrollY) {
        if (null != bookmarkToRestore) {
            bookmarkToRestore.page = currentPage;
            bookmarkToRestore.numberOfPages = pageCount;
            bookmarkToRestore.absoluteZoomLevel = zoom;
            if (zoom != 1000f) {
                bookmarkToRestore.absoluteZoomLevel = zoom;
            }

            if (scrollX >= 0) { //for mupdfrecycleractivity,don't modify scrollx
                bookmarkToRestore.offsetX = scrollX;
            }
            bookmarkToRestore.offsetY = scrollY;

            return bookmarkToRestore;
        }
        return new BookmarkEntry(pageCount, currentPage, zoom, 0, scrollX, scrollY);
    }
}
