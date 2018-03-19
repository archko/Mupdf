package org.vudroid.pdfdroid.codec;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;

import com.artifex.mupdf.fitz.Cookie;
import com.artifex.mupdf.fitz.Document;
import com.artifex.mupdf.fitz.Page;
import com.artifex.mupdf.fitz.android.AndroidDrawDevice;

import org.vudroid.core.BitmapPool;
import org.vudroid.core.codec.CodecPage;

public class PdfPage implements CodecPage {

    private long pageHandle;
    Document core;
    Page page;
    int pdfPageWidth;
    int pdfPageHeight;

    public PdfPage(Document core, long pageHandle) {
        this.core = core;
        this.pageHandle = pageHandle;
    }

    public boolean isDecoding() {
        return false;  //TODO
    }

    public void waitForDecode() {
        //TODO
    }

    public int getWidth() {
        //return (int) getMediaBox().width();
        if (pdfPageWidth == 0) {
            pdfPageWidth = (int) (page.getBounds().x1 - page.getBounds().x0);
        }
        return pdfPageWidth;
    }

    public int getHeight() {
        //return (int) getMediaBox().height();
        if (pdfPageHeight == 0) {
            pdfPageHeight = (int) (page.getBounds().y1 - page.getBounds().y0);
        }
        return pdfPageHeight;
    }

    public Bitmap renderBitmap(int width, int height, RectF pageSliceBounds) {
        Matrix matrix = new Matrix();
        //matrix.postScale(width/getMediaBox().width(), -height/getMediaBox().height());
        matrix.postTranslate(0, height);
        matrix.postTranslate(-pageSliceBounds.left * width, -pageSliceBounds.top * height);
        matrix.postScale(1 / pageSliceBounds.width(), 1 / pageSliceBounds.height());
        return render(new Rect(0, 0, width, height), matrix);
    }

    /**
     * 解码
     *
     * @param width           一个页面的宽
     * @param height          一个页面的高
     * @param pageSliceBounds 每个页面的边框
     * @param scale           缩放级别
     * @return 位图
     */
    public Bitmap renderBitmap(int width, int height, RectF pageSliceBounds, float scale) {
        //Matrix matrix=new Matrix();
        //matrix.postScale(width/getWidth(), -height/getHeight());
        //matrix.postTranslate(0, height);
        //matrix.postTranslate(-pageSliceBounds.left*width, -pageSliceBounds.top*height);
        //matrix.postScale(1/pageSliceBounds.width(), 1/pageSliceBounds.height());

        int pageW;
        int pageH;
        int patchX;
        int patchY;
        pageW = (int) (getWidth() * scale);
        pageH = (int) (getHeight() * scale);

        patchX = (int) (pageSliceBounds.left * pageW);
        patchY = (int) (pageSliceBounds.top * pageH);
        //Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Bitmap bitmap = BitmapPool.getInstance().acquire(width, height);

        com.artifex.mupdf.fitz.Matrix ctm = new com.artifex.mupdf.fitz.Matrix(scale);
        AndroidDrawDevice dev = new AndroidDrawDevice(bitmap, patchX, patchY, 0, 0, width, height);
        page.run(dev, ctm, (Cookie) null);
        dev.close();
        dev.destroy();

        return bitmap;
    }

    static PdfPage createPage(Document core, int pageno) {
        PdfPage pdfPage = new PdfPage(core, pageno);
        pdfPage.page = core.loadPage(pageno);
        return pdfPage;
    }

    @Override
    protected void finalize() throws Throwable {
        recycle();
        super.finalize();
    }

    public synchronized void recycle() {
        if (pageHandle != 0) {
            pageHandle = 0;
            page.destroy();
        }
    }

    public Bitmap render(Rect viewbox, Matrix matrix) {
        int[] mRect = new int[4];
        mRect[0] = viewbox.left;
        mRect[1] = viewbox.top;
        mRect[2] = viewbox.right;
        mRect[3] = viewbox.bottom;

        float[] matrixSource = new float[9];
        float[] matrixArray = new float[6];
        matrix.getValues(matrixSource);
        matrixArray[0] = matrixSource[0];
        matrixArray[1] = matrixSource[3];
        matrixArray[2] = matrixSource[1];
        matrixArray[3] = matrixSource[4];
        matrixArray[4] = matrixSource[2];
        matrixArray[5] = matrixSource[5];

        int width = viewbox.width();
        int height = viewbox.height();
        int[] bufferarray = new int[width * height];
        //nativeCreateView(docHandle, pageHandle, mRect, matrixArray, bufferarray);
        return Bitmap.createBitmap(bufferarray, width, height, Bitmap.Config.RGB_565);
    }

    public byte[] asHtml(int pageno) {
        if (page == null) {
            page = core.loadPage(pageno);
        }
        return page.textAsHtml();
    }
}
