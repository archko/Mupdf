package cx.hell.android.pdfviewpro;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.os.Handler;
import android.util.Log;
import android.view.WindowManager;
import com.artifex.mupdfdemo.MuPDFCore;
import cx.hell.android.lib.pagesview.OnImageRenderedListener;
import cx.hell.android.lib.pagesview.PagesProvider;
import cx.hell.android.lib.pagesview.RenderingException;
import cx.hell.android.lib.pagesview.Tile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @description:
 * @author: archko 15-2-4 :下午3:50
 */
public class AKDecodeService extends PagesProvider {

    /**
     * Const used by logging.
     */
    private final static String TAG="cx.hell.android.pdfviewpro";

    private float renderAhead=2.1f;
    private boolean doRenderAhead=true;
    private int extraCache=0;
    private boolean omitImages;
    private static final int MB=1024*1024;

    public void setExtraCache(int extraCache) {
        this.extraCache=extraCache;

        setMaxCacheSize();
    }

    /* also calculates renderAhead */
    private void setMaxCacheSize() {
        long availLong=(long) (Runtime.getRuntime().maxMemory()*3/5-4*MB);

        int avail;
        if (availLong>256*MB)
            avail=256*MB;
        else
            avail=(int) availLong;

        int maxMax=7*MB+this.extraCache; /* at most allocate this much unless absolutely necessary */
        if (maxMax<avail)
            maxMax=avail;
        int minMax=4*MB; /* at least allocate this much */
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

        if (m+20*MB<=maxMax)
            m=maxMax-20*MB;

        if (m<maxMax) {
            m+=this.extraCache;
            if (maxMax<m)
                m=maxMax;
        }

        Log.v(TAG, "Setting cache size="+m+" renderAhead="+renderAhead+" for "+screenWidth+"x"+screenHeight+" (avail="+avail+")");

        this.bitmapCache.setMaxCacheSizeBytes((int) m);
    }

    public void setOmitImages(boolean skipImages) {
        if (this.omitImages==skipImages)
            return;
        this.omitImages=skipImages;

        if (this.bitmapCache!=null) {
            this.bitmapCache.clearCache();
        }
    }

    static final AtomicLong TASK_ID_SEQ = new AtomicLong();
    static Handler mHandler=new Handler();
    final Executor executor;

    /**
     * 资源释放
     */
    public void release() {
        if (null!=bitmapCache) {
            bitmapCache.clearCache();
        }
        bitmapCache=null;
    }

    //====================================================

    private MuPDFCore pdf=null;
    private BitmapCache bitmapCache=null;
    private OnImageRenderedListener onImageRendererListener=null;

    public float getRenderAhead() {
        return this.renderAhead;
    }

    public AKDecodeService(MuPDFCore pdf, boolean skipImages, boolean doRenderAhead) {
        this.pdf=pdf;
        this.omitImages=skipImages;
        this.bitmapCache=new BitmapCache();
        this.doRenderAhead=doRenderAhead;
        setMaxCacheSize();
        init();
        executor = new Executor();
        executor.start();
    }

    private void init() {
    }

    @Override
    public void setRenderAhead(boolean doRenderAhead) {
        this.doRenderAhead=doRenderAhead;
        setMaxCacheSize();
    }

    /**
     * Really render bitmap. Takes time, should be done in background thread.
     * Calls native code (through PDF object).
     */
    private Bitmap renderBitmap(Tile tile) throws RenderingException {
        //synchronized (tile) {
            /* last minute check to make sure some other thread hasn't rendered this tile */
            /*if (this.bitmapCache.contains(tile)) {
                return null;
            }*/

            Bitmap b=Bitmap.createBitmap(tile.getPrefXSize(), tile.getPrefYSize(), Bitmap.Config.ARGB_8888);
            PointF size=pdf.getPageSize(tile.getPage());
            pdf.renderPage(b, tile.getPage(),
                (int) size.x*tile.getZoom()/1000, (int) size.y*tile.getZoom()/1000,
                tile.getX(), tile.getY(),
                tile.getPrefXSize(), tile.getPrefYSize(), pdf.new Cookie());

            /*Bitmap maskBitmap=Bitmap.createBitmap(b.getWidth(), b.getHeight(), Bitmap.Config.RGB_565);
            Canvas c=new Canvas();
            c.setBitmap(maskBitmap);
            Paint p=new Paint();
            //p.setFilterBitmap(true); // possibly not nessecary as there is no scaling
            c.drawBitmap(b, 0, 0, p);
            b.recycle();

            this.bitmapCache.put(tile, maskBitmap);
            return maskBitmap;*/
            this.bitmapCache.put(tile, b);
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
        if (null==bitmapCache) {
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
        int c=this.pdf.countPages();
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
        //Map<Tile, Bitmap> renderedTiles=new HashMap<Tile, Bitmap>();
        for (Tile tile : tiles) {
            if (!bitmapCache.contains(tile)) {
                final DecodeTask decodeTask = new DecodeTask(tile);
                executor.add(decodeTask);
            } /*else {
                Log.d(TAG, "setVisibleTiles:"+tile);
                renderedTiles.put(tile, bitmapCache.get(tile));
            }*/
        }

        /*if (renderedTiles.size()>0) {
            publishBitmaps(renderedTiles);
        }*/
    }

    void performDecode(final DecodeTask task) {
        if (executor.isTaskDead(task)) {
                /*Log.d(TAG, Thread.currentThread().getName() + ": Task " + task.id + ": Skipping dead decode task for "
                    + task.node);*/
            return;
        }

            //Log.d(TAG, Thread.currentThread().getName() + ": Task " + task.id + ": Starting decoding for " + task.node);

        try {
            Bitmap bitmap=this.bitmapCache.get(task.node);
            if (null==bitmap) {
                bitmap=renderBitmap(task.node);
            } else {
                //Log.d(TAG, Thread.currentThread().getName() + ": Task " + task.id + ": decode bitmap for " + task.node);
            }

            if (executor.isTaskDead(task)) {
                   /* Log.d(TAG, Thread.currentThread().getName() + ": Task " + task.id + ": Abort dead decode task for "
                        + task.node);*/
                return;
            }

            finishDecoding(task, bitmap);
        } catch (final OutOfMemoryError ex) {
            Log.e(TAG, Thread.currentThread().getName() + ": Task " + task.id + ": No memory to decode " + task.node);

            abortDecoding(task, null);
        } catch (final Throwable th) {
            Log.e(TAG, Thread.currentThread().getName() + ": Task " + task.id + ": Decoding failed for " + task.node + ": "
                + th.getMessage(), th);
            abortDecoding(task, null);
        } finally {

        }
    }
    
    //====================================================

    private void updateImage(final Tile node, final Bitmap bitmap) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Map<Tile, Bitmap> renderedTiles=new HashMap<Tile, Bitmap>();
                renderedTiles.put(node, bitmap);
                publishBitmaps(renderedTiles);
            }
        });
    }

    void finishDecoding(final DecodeTask currentDecodeTask, final Bitmap bitmap) {
        stopDecoding(currentDecodeTask.node, "complete");
        if (null!=bitmap) {
            updateImage(currentDecodeTask.node, bitmap);
        }
    }

    void abortDecoding(final DecodeTask currentDecodeTask, final Bitmap bitmap) {
        stopDecoding(currentDecodeTask.node, "failed");
        if (null!=bitmap) {
            updateImage(currentDecodeTask.node, bitmap);
        }
    }

    public void stopDecoding(final Tile node, final String reason) {
        executor.stopDecoding(null, node, reason);
    }

    class Executor implements Runnable {

        final Map<Tile, DecodeTask> decodingTasks = new IdentityHashMap<Tile, DecodeTask>();

        final ArrayList<Task> tasks;
        final Thread[] threads;
        final ReentrantLock lock = new ReentrantLock();
        final AtomicBoolean run = new AtomicBoolean(true);

        Executor() {
            tasks = new ArrayList<Task>();
            threads = new Thread[APVApplication.getInstance().getThreadCount()];

            Log.i(TAG, "Number of decoding threads: " + threads.length);

            for (int i = 0; i < threads.length; i++) {
                threads[i] = new Thread(this, "DecodingThread-" + i);
            }
        }

        void start() {
            final int decodingThreadPriority = APVApplication.getInstance().getThreadPriority();
            Log.i(TAG,"Decoding thread priority: " + decodingThreadPriority);

            for (int i = 0; i < threads.length; i++) {
                threads[i].setPriority(decodingThreadPriority);
                threads[i].start();
            }
        }

        @Override
        public void run() {
            try {
                while (run.get()) {
                    final Runnable r = nextTask();
                    if (r != null) {
                        //BitmapManager.release();
                        //ByteBufferManager.release();
                        r.run();
                    }
                }

            } catch (final Throwable th) {
                Log.e(TAG, Thread.currentThread().getName() + ": Decoding service executor failed: " + th.getMessage(), th);
                //LogManager.onUnexpectedError(th);
            } finally {
                //BitmapManager.release();
                if (null!=bitmapCache) {
                    bitmapCache.clearCache();
                    bitmapCache=null;
                }
            }
        }

        Runnable nextTask() {
            //if (vs == null || vs.app == null || vs.app.decodingOnScroll || vs.ctrl.getView().isScrollFinished()) {
                lock.lock();
                try {
                    if (!tasks.isEmpty()) {
                        return selectBestTask();
                    }
                } finally {
                    lock.unlock();
                }
            /*} else {
                Log.d(TAG, Thread.currentThread().getName() + ": view in scrolling");
            }*/
            synchronized (run) {
                try {
                    run.wait(500);
                } catch (final InterruptedException ex) {
                    Thread.interrupted();
                }
            }
            return null;
        }

        private Runnable selectBestTask() {
            //final TaskComparator comp = new TaskComparator(viewState.get());
            Task candidate = null;
            int cindex = 0;

            int index = 0;
            while (index < tasks.size() && candidate == null) {
                candidate = tasks.get(index);
                if (candidate != null && candidate.cancelled.get()) {
                    tasks.set(index, null);
                    candidate = null;
                }
                cindex = index;
                index++;
            }
            if (candidate == null) {
                    Log.d(TAG, Thread.currentThread().getName() + ": No tasks in queue");
                tasks.clear();
            } else {
                while (index < tasks.size()) {
                    final Task next = tasks.get(index);
                    if (next != null) {
                        if (next.cancelled.get()) {
                                Log.d(TAG, "---: " + index + "/" + tasks.size() + " " + next);
                            tasks.set(index, null);
                        } /*else if (comp.compare(next, candidate) < 0) {
                            candidate = next;
                            cindex = index;
                        }*/
                    }
                    index++;
                }
                    Log.d(TAG, Thread.currentThread().getName() + ": <<<: " + cindex + "/" + tasks.size() + ": "
                        + candidate);
                tasks.set(cindex, null);
            }
            return candidate;
        }

        public void add(final SearchTask task) {
                Log.d(TAG, Thread.currentThread().getName() + ": Adding search task: " + task + " for " );

            lock.lock();
            try {
                boolean added = false;
                for (int index = 0; index < tasks.size(); index++) {
                    if (null == tasks.get(index)) {
                        tasks.set(index, task);
                            Log.d(TAG, Thread.currentThread().getName() + ": >>>: " + index + "/" + tasks.size() + ": "
                                + task);
                        added = true;
                        break;
                    }
                }
                if (!added) {
                        Log.d(TAG, Thread.currentThread().getName() + ": +++: " + tasks.size() + "/" + tasks.size() + ": "
                            + task);
                    tasks.add(task);
                }

                synchronized (run) {
                    run.notifyAll();
                }
            } finally {
                lock.unlock();
            }
        }

        public void stopSearch(final String pattern) {
                Log.d(TAG, "Stop search tasks: " + pattern);

            lock.lock();
            /*try {
                for (int index = 0; index < tasks.size(); index++) {
                    final Task task = tasks.get(index);
                    if (task instanceof SearchTask) {
                        final SearchTask st = (SearchTask) task;
                        if (st.pattern.equals(pattern)) {
                            tasks.set(index, null);
                        }
                    }
                }
            } finally {
                lock.unlock();
            }*/
        }

        public void add(final DecodeTask task) {
            //Log.d(TAG, "Adding decoding task: " + task + " for " + task.node);

            lock.lock();
            try {
                final DecodeTask running = decodingTasks.get(task.node);
                //Log.d(TAG, "Adding task: " + running + " for " + task);
                if (running != null && running.equals(task) && !isTaskDead(running)) {
                        //Log.d(TAG, "The similar task is running: " + running.id + " for " + task.node);
                    return;
                } else if (running != null) {
                        //Log.d(TAG, "The another task is running: " + running.id + " for " + task.node);
                }

                decodingTasks.put(task.node, task);

                boolean added = false;
                for (int index = 0; index < tasks.size(); index++) {
                    if (null == tasks.get(index)) {
                        tasks.set(index, task);
                        //Log.d(TAG, ">>>: " + index + "/" + tasks.size() + ": " + task);
                        added = true;
                        break;
                    } else {
                        if (tasks.get(index).equals(task)) {
                            //Log.d(TAG, "The similar task is added: " + " for " + task.node);
                            added=true;
                        }
                    }
                }
                if (!added) {
                        //Log.d(TAG, "+++: " + tasks.size() + "/" + tasks.size() + ": " + task);
                    tasks.add(task);
                }

                synchronized (run) {
                    run.notifyAll();
                }

                if (running != null) {
                    stopDecoding(running, null, "canceled by new one");
                }
            } finally {
                lock.unlock();
            }
        }

        public void stopDecoding(final DecodeTask task, final Tile node, final String reason) {
            lock.lock();
            try {
                final DecodeTask removed = task == null ? decodingTasks.remove(node) : task;

                if (removed != null) {
                    removed.cancelled.set(true);
                        /*Log.d(TAG, Thread.currentThread().getName() + ": Task " + removed.id
                            + ": Stop decoding task with reason: " + reason + " for " + removed.node);*/
                }
            } finally {
                lock.unlock();
            }
        }

        public boolean isTaskDead(final DecodeTask task) {
            return task.cancelled.get();
        }

        public void recycle() {
            lock.lock();
            try {
                for (final DecodeTask task : decodingTasks.values()) {
                    stopDecoding(task, null, "recycling");
                }

                tasks.add(new ShutdownTask());

                synchronized (run) {
                    run.notifyAll();
                }

            } finally {
                lock.unlock();
            }
        }

        void shutdown() {
            /*for (final CodecPageHolder ref : pages.values()) {
                ref.recycle(-3, true);
            }
            pages.clear();
            if (document != null) {
                document.recycle();
            }
            codecContext.recycle();*/
            run.set(false);
        }
    }

    /*class TaskComparator implements Comparator<Task> {

        final PageTreeNodeComparator cmp;

        public TaskComparator(final ViewState viewState) {
            cmp = viewState != null ? new PageTreeNodeComparator(viewState) : null;
        }

        @Override
        public int compare(final Task r1, final Task r2) {
            if (r1.priority < r2.priority) {
                return -1;
            }
            if (r2.priority < r1.priority) {
                return +1;
            }

            if (r1 instanceof DecodeTask && r2 instanceof DecodeTask) {
                final DecodeTask t1 = (DecodeTask) r1;
                final DecodeTask t2 = (DecodeTask) r2;

                if (cmp != null) {
                    return cmp.compare(t1.node, t2.node);
                }
                return 0;
            }

            return CompareUtils.compare(r1.id, r2.id);
        }

    }*/

    abstract class Task implements Runnable {

        final long id = TASK_ID_SEQ.incrementAndGet();
        final AtomicBoolean cancelled = new AtomicBoolean();
        final int priority;

        Task(final int priority) {
            this.priority = priority;
        }

    }

    class ShutdownTask extends Task {

        public ShutdownTask() {
            super(0);
        }

        @Override
        public void run() {
            executor.shutdown();
        }
    }

    class SearchTask extends Task {

        SearchTask(int priority) {
            super(priority);
        }

        /*final Page page;
        final String pattern;
        final SearchCallback callback;

        public SearchTask(final Page page, final String pattern, final SearchCallback callback) {
            super(1);
            this.page = page;
            this.pattern = pattern;
            this.callback = callback;
        }*/

        @Override
        public void run() {
            /*List<? extends RectF> regions = null;
            if (document != null) {
                try {
                    if (codecContext.isFeatureSupported(CodecFeatures.FEATURE_DOCUMENT_TEXT_SEARCH)) {
                        regions = document.searchText(page.index.docIndex, pattern);
                    } else if (codecContext.isFeatureSupported(CodecFeatures.FEATURE_PAGE_TEXT_SEARCH)) {
                        regions = getPage(page.index.docIndex).searchText(pattern);
                    }
                    callback.searchComplete(page, regions);
                } catch (final Throwable th) {
                    LCTX.e("Unexpected error: ", th);
                    callback.searchComplete(page, null);
                }
            }*/
        }
    }

    class DecodeTask extends Task {

        final long id = TASK_ID_SEQ.incrementAndGet();
        final AtomicBoolean cancelled = new AtomicBoolean();

        final Tile node;
        final int pageNumber;

        DecodeTask(final Tile node) {
            super(2);
            this.pageNumber = node.getPage();
            this.node = node;
        }

        @Override
        public void run() {
            performDecode(this);
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof DecodeTask) {
                final DecodeTask that = (DecodeTask) obj;
                /*return this.pageNumber == that.pageNumber
                    &&this.node.getPrefXSize()==that.node.getPrefXSize()
                    &&this.node.getZoom()==that.node.getZoom();*/
                boolean flag= this.node.equals(that.node);

                //Log.d(TAG, flag+": Task "+node+" that: "+that.node);
                return flag;
            }
            return false;
        }

        @Override
        public String toString() {
            final StringBuilder buf = new StringBuilder("DecodeTask");
            buf.append("[");

            buf.append("id").append("=").append(id);
            buf.append(", ");
            buf.append("target").append("=").append(node);
            buf.append(", ");
            //buf.append("width").append("=").append((int) viewState.viewRect.width());
            buf.append(", ");
            //buf.append("zoom").append("=").append(viewState.zoom);

            buf.append("]");
            return buf.toString();
        }
    }
}
