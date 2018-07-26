package cn.archko.pdf;

import android.graphics.Bitmap;

/**
 * bitmap cache
 * cache bitmap in memory
 *
 * @author: wushuyong 2018/7/26 :10:49
 */
public class AKBitmapManager {

    private int count = 4;
    private Bitmap[] bitmaps = new Bitmap[count];
    private int[] index = new int[count];
    private int mCurrIndex = 0;

    public Bitmap getBitmap(int pageNumber) {
        for (int i = 0; i < count; i++) {
            if (index[i] == pageNumber) {
                return bitmaps[i];
            }
        }
        return null;
    }

    public void setBitmap(int pageNumber, Bitmap bitmap) {
        boolean hasExist = false;
        for (int i = 0; i < count; i++) {
            if (index[i] == pageNumber) {
                hasExist = true;
                bitmaps[i] = bitmap;
                break;
            }
        }
        if (!hasExist) {
            hasExist = false;
            for (int i = 0; i < count; i++) {
                if (bitmaps[i] == null) {
                    hasExist = true;
                    bitmaps[i] = bitmap;
                    index[i] = pageNumber;
                    return;
                }
            }
            if (!hasExist) {
                bitmaps[mCurrIndex] = bitmap;
                index[mCurrIndex] = pageNumber;
                mCurrIndex++;
                if (mCurrIndex >= count) {
                    mCurrIndex = 0;
                }
            }
        }
    }
}
