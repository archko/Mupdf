package org.vudroid.core;

import android.graphics.Bitmap;
import android.support.v4.util.Pools;

/**
 * Created by archko on 16/12/24.
 */

public class BitmapPool {

    private static BitmapPool sInstance = new BitmapPool();
    private Pools.SimplePool<Bitmap> simplePool;

    private BitmapPool() {
        simplePool = new Pools.SimplePool<>(16);
    }

    public static BitmapPool getInstance() {
        return sInstance;
    }

    public Bitmap acquire(int width, int height) {
        Bitmap b = simplePool.acquire();
        if (null == b) {
            b = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        } else {
            if (b.getHeight() == height && b.getWidth() == width) {
                b.eraseColor(0);
            } else {
                b = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            }
        }
        return b;
    }

    public void release(Bitmap bitmap) {
        boolean isRelease = simplePool.release(bitmap);
        if (!isRelease) {
            bitmap.recycle();
        }
    }

    public synchronized void clear() {
        if (null == simplePool) {
            return;
        }
        Bitmap bitmap;
        while ((bitmap = simplePool.acquire()) != null) {
            bitmap.recycle();
        }
        //simplePool = null;
    }
}
