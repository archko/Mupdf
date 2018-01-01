package org.vudroid.pdfdroid.codec;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;

import com.artifex.mupdf.fitz.Cookie;
import com.artifex.mupdf.viewer.MuPDFCore;

import org.vudroid.core.codec.CodecPage;

public class PdfPage implements CodecPage {

    private long pageHandle;
    MuPDFCore core;
    int pdfPageWidth;
    int pdfPageHeight;

    public PdfPage(MuPDFCore core, long pageHandle) {
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
            pdfPageWidth = (int) (core.getPDFPageWidth());
        }
        return pdfPageWidth;
    }

    public int getHeight() {
        //return (int) getMediaBox().height();
        if (pdfPageHeight == 0) {
            pdfPageHeight = (int) (core.getPDFPageHeight());
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
     * ��Ⱦλͼ
     *
     * @param width           ͼ���
     * @param height          ͼ���
     * @param pageSliceBounds �и�ľ���,�и�4����Ϊ1/4,8��Ϊ1/8,����.
     * @param scale           ���ż���
     * @return λ��
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
        int patchW;
        int patchH;
        pageW = (int) (getWidth() * scale);
        pageH = (int) (getHeight() * scale);

        patchX = (int) (pageSliceBounds.left * pageW);
        patchY = (int) (pageSliceBounds.top * pageH);
        patchW = width;
        patchH = height;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        core.drawPage(bitmap, (int) pageHandle,
                pageW, pageH,
                patchX, patchY,
                patchW, patchH, (Cookie) null);
        return bitmap;
    }

    static PdfPage createPage(MuPDFCore core, int pageno) {
        PdfPage pdfPage = new PdfPage(core, pageno);
        core.gotoPage(pageno);
        return pdfPage;
    }

    @Override
    protected void finalize() throws Throwable {
        recycle();
        super.finalize();
    }

    public synchronized void recycle() {
        if (pageHandle != 0) {
            //core.freePage((int) pageHandle);
            pageHandle = 0;
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
}
