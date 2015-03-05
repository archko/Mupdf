package cx.hell.android.pdfviewpro;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.view.WindowManager;
import com.artifex.mupdfdemo.MuPDFCore;
import cx.hell.android.lib.pagesview.OnImageRenderedListener;
import cx.hell.android.lib.pagesview.PagesProvider;
import cx.hell.android.lib.pagesview.PagesView;
import cx.hell.android.lib.pagesview.RenderingException;
import cx.hell.android.lib.pagesview.Tile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @description:
 * @author: archko 13-10-7 :下午3:50
 */
public class AKPDFPagesProvider extends PagesProvider {

    /**
     * Const used by logging.
     */
    private final static String TAG="cx.hell.android.pdfviewpro";

    private float renderAhead=2.1f;
    private boolean doRenderAhead=true;
    private int extraCache=0;
    private boolean omitImages;
    private static final int MB=1024*1024;
    private Collection<Tile> tiles;
    private static final int cacheSize=16;

    public void setExtraCache(int extraCache) {
        this.extraCache=extraCache;

        setMaxCacheSize();
    }

    /* also calculates renderAhead */
    private int setMaxCacheSize() {
        long availLong=(long) (Runtime.getRuntime().maxMemory()/3-4*MB);

        int avail;
        if (availLong>64*MB)
            avail=64*MB;
        else
            avail=(int) availLong;

        int maxMax=7*MB+this.extraCache; /* at most allocate this much unless absolutely necessary */
        if (maxMax<avail)
            maxMax=avail;
        int minMax=8*MB; /* at least allocate this much */
        if (maxMax<minMax)
            maxMax=minMax;

        WindowManager wm=(WindowManager) APVApplication.getInstance().getSystemService(Context.WINDOW_SERVICE);
        int screenHeight=wm.getDefaultDisplay().getHeight();
        int screenWidth=wm.getDefaultDisplay().getWidth();
        int displaySize=screenWidth*screenHeight;

        if (displaySize<=320*240)
            displaySize=320*240;

        int m=(int) (displaySize*1.25f*1.0001f);

        if (doRenderAhead) {
            if ((int) (m*2.1f)<=maxMax) {
                renderAhead=2.1f;
                m=(int) (m*renderAhead);
            } else {
                renderAhead=1.0001f;
            }
        } else {
            /* The extra little bit is to compensate for round-off */
            renderAhead=1.0001f;
        }

        if (m<minMax)
            m=minMax;

        if (m+10*MB<=maxMax)
            m=maxMax-10*MB;

        if (m<maxMax) {
            m+=this.extraCache;
            if (maxMax<m)
                m=maxMax;
        }

        Log.v(TAG, "Setting cache size="+m+" renderAhead="+renderAhead+" for "+screenWidth+"x"+screenHeight+" (avail="+avail+")"+" extraCache:"+extraCache);

        //this.bitmapCache.setMaxCacheSizeBytes((int) m);
        return m;
    }

    public void setCacheSize(int len) {
        int lruCacheSize=cacheSize;
        if (len>0) {
            lruCacheSize=setMaxCacheSize()/len/4;
        }
        if (lruCacheSize>cacheSize*2) {
            lruCacheSize=cacheSize*2;
        } else if (lruCacheSize<=cacheSize) {
            lruCacheSize=cacheSize;
        }
        Log.v(TAG, "setCacheSize:"+len+" lru:"+lruCacheSize);
        bitmapCache.resize(lruCacheSize*4/3);
    }

    public void setOmitImages(boolean skipImages) {
        if (this.omitImages==skipImages)
            return;
        this.omitImages=skipImages;

        if (this.bitmapCache!=null) {
            this.bitmapCache.evictAll();
        }
    }

    Handler mHandler;

    private void init() {
        initDecodeThread();
        this.mHandler.sendEmptyMessage(0);
        this.mHandler.removeMessages(1);
        this.mHandler.sendEmptyMessage(1);
    }

    private void initDecodeThread() {
        quitLooper();

        Log.d(TAG, "initDecodeThread:");
        synchronized (this) {
            final Thread previewThread=new Thread() {
                @Override
                public void run() {
                    Looper.prepare();
                    mHandler=new Handler() {
                        @Override
                        public void handleMessage(Message msg) {
                            tiles=new ArrayList<Tile>();
                            internalhandleMessage(msg);
                        }
                    };
                    looperPrepared();
                    Looper.loop();
                    Log.d(TAG, "quit.");
                }
            };
            previewThread.start();
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void looperPrepared() {
        synchronized (this) {
            try {
                notify();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void internalhandleMessage(Message msg) {
        //Log.d(TAG, "internalhandleMessage:"+msg.what);
        switch (msg.what) {
            default:
                return;
            case 0:
                internalStart(msg);
                return;
            case 1:
                moniter();
                return;
            case 2:
                internalRelease();
                return;
            case 3:
                return;
            case 4:
                return;
            case 5:
                Map<Tile, Bitmap> renderedTiles=(Map<Tile, Bitmap>) msg.obj;
                publishBitmaps(renderedTiles);
                return;
            case 6:
                RenderingException e=(RenderingException) msg.obj;
                publishRenderingException(e);
                return;
        }
        //internalStop();
        //stop();
    }

    private void moniter() {
        //if ((this.mCurrentState==STATE_PAUSED)||(this.mTargetState==STATE_PAUSED)) {
        //this.mHandler.sendEmptyMessageDelayed(1, 1000L);
        return;
        //}
    }

    private void internalStart(Message msg) {
        //Log.d(TAG, "internalStart:");
        startRender(msg);
    }

    /**
     * 资源释放
     */
    public void release() {
        if (null!=mHandler) {
            mHandler.sendEmptyMessage(2);
        }
    }

    public void internalRelease() {
        if (this.mHandler!=null) {
            this.mHandler.removeCallbacksAndMessages(null);
        }
        quitLooper();
        if (null!=bitmapCache) {
            bitmapCache.evictAll();
        }
        bitmapCache=null;
    }

    private void quitLooper() {
        try {
            synchronized (this) {
                Looper.myLooper().quit();
            }
        } catch (Exception e) {
        }
    }

    public void startRender(Message msg) {
        //Log.d(TAG, "startRender:");

        if (null!=msg.obj) {
            try {
                List<Tile> newtiles=(List<Tile>) msg.obj;
                tiles.addAll(newtiles);
            } catch (Exception e) {
                e.printStackTrace();
            }
            render();
        }
    }

    Collection<Tile> popTiles() {
        if (this.tiles==null||this.tiles.isEmpty()) {
            return null;
        }
        Tile tile=this.tiles.iterator().next();
        this.tiles.remove(tile);
        return Collections.singleton(tile);
    }

    private void render() {
        Collection<Tile> tiles=this.popTiles(); /* this can't block */
        if (tiles==null||tiles.size()==0) return;//break;
        try {
            Map<Tile, Bitmap> renderedTiles=renderTiles(tiles, null);
            if (renderedTiles.size()>0) {
                Message msg=Message.obtain();
                msg.obj=renderedTiles;
                msg.what=5;
                mHandler.sendMessage(msg);
            } else {
                //Log.d(TAG, "drop:"+tiles);
            }
        } catch (RenderingException e) {
            Message msg=Message.obtain();
            msg.obj=e;
            msg.what=6;
            mHandler.sendMessage(msg);
        }
    }

    //====================================================

    private MuPDFCore pdf=null;
    private LruCache<Tile, Bitmap> bitmapCache=null;
    private OnImageRenderedListener onImageRendererListener=null;
    PagesView mPagesView;

    public float getRenderAhead() {
        return this.renderAhead;
    }

    public AKPDFPagesProvider(MuPDFCore pdf, boolean skipImages, boolean doRenderAhead, PagesView pagesView) {
        this.pdf=pdf;
        this.omitImages=skipImages;
        this.bitmapCache=new BCache(cacheSize);
        this.doRenderAhead=doRenderAhead;
        this.mPagesView=pagesView;
        size=0;
        setMaxCacheSize();
        init();
    }

    static int size=0;

    class BCache extends LruCache<Tile, Bitmap> {

        public BCache(int maxSize) {
            super(maxSize);
        }

        @Override
        protected void entryRemoved(boolean evicted, Tile key, Bitmap oldValue, Bitmap newValue) {
            //size+=oldValue.getWidth()*oldValue.getHeight()*4;
            //Log.d(TAG, "entryRemoved:"+maxSize()+" evicted:"+evicted+" ==>"+size+" memsize:"+getAllSize());
            if (evicted&&null!=oldValue) {
                oldValue.recycle();
            }
        }

        int getAllSize() {
            Map<Tile, Bitmap> map=bitmapCache.snapshot();
            Bitmap bitmap;
            int size=0;
            for (Tile tile : map.keySet()) {
                bitmap=map.get(tile);
                size+=bitmap.getWidth()*bitmap.getHeight()*4;
            }
            return size;
        }
    }

    @Override
    public void setRenderAhead(boolean doRenderAhead) {
        this.doRenderAhead=doRenderAhead;
        setMaxCacheSize();
    }

    /**
     * Render tiles.
     * Called by worker, calls PDF's methods that in turn call native code.
     *
     * @param tiles job description - what to render
     * @return mapping of jobs and job results, with job results being Bitmap objects
     */
    public Map<Tile, Bitmap> renderTiles(Collection<Tile> tiles, BitmapCache ignore) throws RenderingException {
        Map<Tile, Bitmap> renderedTiles=new HashMap<Tile, Bitmap>();
        Iterator<Tile> i=tiles.iterator();
        Tile tile=null;

        while (i.hasNext()) {
            tile=i.next();
            Bitmap bitmap=this.renderBitmap(tile);
            if (bitmap!=null)
                renderedTiles.put(tile, bitmap);
        }

        return renderedTiles;
    }

    /**
     * Really render bitmap. Takes time, should be done in background thread.
     * Calls native code (through PDF object).
     */
    private Bitmap renderBitmap(Tile tile) throws RenderingException {
        //synchronized (tile) {
            /* last minute check to make sure some other thread hasn't rendered this tile */
        if (this.bitmapCache.get(tile)!=null&&bitmapCache.get(tile)!=null) {
            return null;
        }
        if (doRenderAhead&&Math.abs(mPagesView.getCurrentPage()-tile.getPage())>1) {
            return null;
        }

        if (!doRenderAhead&&Math.abs(mPagesView.getCurrentPage()-tile.getPage())>0) {
            return null;
        }

        Bitmap b=Bitmap.createBitmap(tile.getPrefXSize(), tile.getPrefYSize(), Bitmap.Config.ARGB_8888);
        PointF size=pdf.getPageSize(tile.getPage());
        pdf.renderPage(b, tile.getPage(),
            (int) size.x*tile.getZoom()/1000, (int) size.y*tile.getZoom()/1000,
            tile.getX(), tile.getY(),
            tile.getPrefXSize(), tile.getPrefYSize(), pdf.new Cookie());
        this.bitmapCache.put(tile, (b));
        return b;
        //}
    }

    /**
     * Called by worker.
     */
    public void publishBitmaps(Map<Tile, Bitmap> renderedTiles) {
        if (this.onImageRendererListener!=null) {
            this.onImageRendererListener.onImagesRendered(renderedTiles);
        } else {
            Log.w(TAG, "we've got new bitmaps, but there's no one to notify about it!");
        }
    }

    /**
     * Called by worker.
     */
    public void publishRenderingException(RenderingException e) {
        if (this.onImageRendererListener!=null) {
            this.onImageRendererListener.onRenderingException(e);
        }
    }

    @Override
    public void setOnImageRenderedListener(OnImageRenderedListener l) {
        this.onImageRendererListener=l;
    }

    /**
     * Get tile bitmap if it's already rendered.
     *
     * @param tile which bitmap
     * @return rendered tile; tile represents rect of TILE_SIZE x TILE_SIZE pixels,
     * but might be of different size (should be scaled when painting)
     */
    @Override
    public Bitmap getPageBitmap(Tile tile) {
        Bitmap b=null;
        if (null==bitmapCache||null==bitmapCache.get(tile)) {
            return b;
        }
        b=this.bitmapCache.get(tile);
        if (b!=null) return b;
        return null;
    }

    /**
     * Get page count.
     *
     * @return number of pages
     */
    @Override
    public int getPageCount() {
        int c=this.pdf.countPages();//getPageCount();
        if (c<=0) throw new RuntimeException("failed to load pdf file: getPageCount returned "+c);
        return c;
    }

    /**
     * Get page sizes from pdf file.
     *
     * @return array of page sizes
     */
    @Override
    public int[][] getPageSizes() {
        int cnt=this.getPageCount();
        int[][] sizes=new int[cnt][];
        PointF pointF;
        for (int i=0; i<cnt; ++i) {
            pointF=pdf.getPageSize2(i);
            sizes[i]=new int[2];
            sizes[i][0]=(int) pointF.x;
            sizes[i][1]=(int) pointF.y;
        }
        return sizes;
    }

    /**
     * View informs provider what's currently visible.
     * Compute what should be rendered and pass that info to renderer worker thread, possibly waking up worker.
     *
     * @param tiles specs of whats currently visible
     */
    public void setVisibleTiles(Collection<Tile> tiles) {
        List<Tile> newtiles=null;
        for (Tile tile : tiles) {
            if (this.bitmapCache.get(tile)==null||bitmapCache.get(tile)==null) {
                if (newtiles==null) newtiles=new LinkedList<Tile>();
                newtiles.add(tile);
            }
        }
        if (newtiles!=null) {
            Message msg=Message.obtain();
            msg.obj=newtiles;
            msg.what=0;
            mHandler.sendMessage(msg);
        }
    }
}
