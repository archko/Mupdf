package org.ebookdroid.core.crop;

import android.graphics.RectF;

import java.nio.ByteBuffer;

public class PageCropper {

    public static final int BMP_SIZE = 400;

    static {
        System.loadLibrary("crop-lib");
    }

    private PageCropper() {
    }

    public static native ByteBuffer create(int size);

    public static synchronized RectF getCropBounds(final ByteBuffer pixels, int width, int height, final RectF psb) {
        return nativeGetCropBounds(pixels, width, height, psb.left, psb.top, psb.right, psb.bottom);
    }

    /*public static synchronized RectF getColumn(final ByteBufferBitmap bitmap, final float x, final float y) {
        return nativeGetColumn(bitmap.getPixels(), bitmap.getWidth(), bitmap.getHeight(), x, y);
    }*/

    private static native RectF nativeGetCropBounds(ByteBuffer pixels, int width, int height, float left, float top,
                                                    float right, float bottom);

    //private static native RectF nativeGetColumn(ByteBuffer pixels, int width, int height, float x, float y);
}
