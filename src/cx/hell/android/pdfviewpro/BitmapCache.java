package cx.hell.android.pdfviewpro;

import android.graphics.Bitmap;
import android.util.Log;
import cx.hell.android.lib.pagesview.Tile;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Smart page-bitmap cache.
 * Stores up to approx maxCacheSizeBytes of images.
 * Dynamically drops oldest unused bitmaps.
 * TODO: Return high resolution bitmaps if no exact res is available.
 * Bitmap images are tiled - tile size is specified in PagesView.TILE_SIZE.
 *
 * @description:
 * @author: archko 13-10-7 :下午4:53
 */
public class BitmapCache {

    private static final String TAG="BitmapCache";
    /**
     * Stores cached bitmaps.
     */
    private Map<Tile, BitmapCacheValue> bitmaps;

    private int maxCacheSizeBytes=16*1024*1024;

    /**
     * Stats logging - number of cache hits.
     */
    private long hits;

    /**
     * Stats logging - number of misses.
     */
    private long misses;

    BitmapCache() {
        this.bitmaps=new HashMap<Tile, BitmapCacheValue>();
        this.hits=0;
        this.misses=0;
    }

    public void setMaxCacheSizeBytes(int maxCacheSizeBytes) {
        this.maxCacheSizeBytes=maxCacheSizeBytes;
    }

    /**
     * Get cached bitmap. Updates last access timestamp.
     *
     * @param k cache key
     * @return bitmap found in cache or null if there's no matching bitmap
     */
    Bitmap get(Tile k) {
        BitmapCacheValue v=this.bitmaps.get(k);
        Bitmap b=null;
        if (v!=null) {
            // yeah
            b=v.bitmap;
            assert b!=null;
            v.millisAccessed=System.currentTimeMillis();
            this.hits+=1;
        } else {
            // le fu
            this.misses+=1;
        }
        if ((this.hits+this.misses)%100==0&&(this.hits>0||this.misses>0)) {
            Log.d("cx.hell.android.pdfviewpro.pagecache", "hits: "+hits+", misses: "+misses+", hit ratio: "+(float) (hits)/(float) (hits+misses)+
                ", size: "+this.bitmaps.size());
        }
        return b;
    }

    /**
     * Put rendered tile in cache.
     *
     * @param tile   tile definition (page, position etc), cache key
     * @param bitmap rendered tile contents, cache value
     */
    synchronized void put(Tile tile, Bitmap bitmap) {
        while (this.willExceedCacheSize(bitmap)&&!this.bitmaps.isEmpty()) {
            //Log.v(TAG, "Removing oldest");
            this.removeOldest();
        }
        this.bitmaps.put(tile, new BitmapCacheValue(bitmap, System.currentTimeMillis(), 0));
    }

    /**
     * Check if cache contains specified bitmap tile. Doesn't update last-used timestamp.
     *
     * @return true if cache contains specified bitmap tile
     */
    synchronized boolean contains(Tile tile) {
        return this.bitmaps.containsKey(tile);
    }

    /**
     * Estimate bitmap memory size.
     * This is just a guess.
     */
    private static int getBitmapSizeInCache(Bitmap bitmap) {
        int numPixels=bitmap.getWidth()*bitmap.getHeight();
        if (bitmap.getConfig()==Bitmap.Config.RGB_565) {
            return numPixels*2;
        } else if (bitmap.getConfig()==Bitmap.Config.ALPHA_8)
            return numPixels;
        else
            return numPixels*4;
    }

    /**
     * Get estimated sum of byte sizes of bitmaps stored in cache currently.
     */
    private synchronized int getCurrentCacheSize() {
        int size=0;
        Iterator<BitmapCacheValue> it=this.bitmaps.values().iterator();
        while (it.hasNext()) {
            BitmapCacheValue bcv=it.next();
            Bitmap bitmap=bcv.bitmap;
            size+=getBitmapSizeInCache(bitmap);
        }
        //Log.v(TAG, "Cache size: "+size);
        return size;
    }

    /**
     * Determine if adding this bitmap would grow cache size beyond max size.
     */
    private synchronized boolean willExceedCacheSize(Bitmap bitmap) {
        return (this.getCurrentCacheSize()+
            BitmapCache.getBitmapSizeInCache(bitmap)>maxCacheSizeBytes);
    }

    /**
     * Remove oldest bitmap cache value.
     */
    private void removeOldest() {
        Iterator<Tile> i=this.bitmaps.keySet().iterator();
        long minmillis=0;
        Tile oldest=null;
        while (i.hasNext()) {
            Tile k=i.next();
            BitmapCacheValue v=this.bitmaps.get(k);
            if (oldest==null) {
                oldest=k;
                minmillis=v.millisAccessed;
            } else {
                if (minmillis>v.millisAccessed) {
                    minmillis=v.millisAccessed;
                    oldest=k;
                }
            }
        }
        if (oldest==null) throw new RuntimeException("couldnt find oldest");
        BitmapCacheValue v=this.bitmaps.get(oldest);
        v.bitmap.recycle();
        this.bitmaps.remove(oldest);
    }

    synchronized public void clearCache() {
        Iterator<Tile> i=this.bitmaps.keySet().iterator();

        while (i.hasNext()) {
            Tile k=i.next();
            //Log.v("Deleting", k.toString());
            this.bitmaps.get(k).bitmap.recycle();
            i.remove();
        }
    }
}
