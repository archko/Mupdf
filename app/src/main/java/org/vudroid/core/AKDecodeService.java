package org.vudroid.core;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;

import org.vudroid.core.codec.CodecContext;
import org.vudroid.core.codec.CodecDocument;
import org.vudroid.core.codec.CodecPage;
import org.vudroid.core.utils.PathFromUri;
import org.vudroid.pdfdroid.codec.PdfPage;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class AKDecodeService implements DecodeService {
    private static final int PAGE_POOL_SIZE = 8;
    private static final int MSG_DECODE_START = 0;
    private static final int MSG_DECODE_FINISH = 4;
    private final CodecContext codecContext;

    private View containerView;
    private CodecDocument document;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    public static final String DECODE_SERVICE = "ViewDroidDecodeService";
    private final Map<Object, Future<?>> decodingFutures = new ConcurrentHashMap<Object, Future<?>>();
    private final SparseArray<SoftReference<CodecPage>> pages = new SparseArray<SoftReference<CodecPage>>();
    private ContentResolver contentResolver;
    private Queue<Integer> pageEvictionQueue = new LinkedList<Integer>();
    private boolean isRecycled;
    Handler mHandler;
    private Handler.Callback mCallback = new Handler.Callback() {
        public boolean handleMessage(Message msg) {
            int what = msg.what;
            if (what == MSG_DECODE_START) {
                final DecodeTask decodeTask = (DecodeTask) msg.obj;
                if (null != decodeTask) {
                    synchronized (decodingFutures) {
                        if (isRecycled) {
                            return true;
                        }
                        final Future<?> future = executorService.submit(new Runnable() {
                            public void run() {
                                try {
                                    Thread.currentThread().setPriority(Thread.NORM_PRIORITY - 1);
                                    performDecode(decodeTask);
                                } catch (IOException e) {
                                    Log.e(DECODE_SERVICE, "Decode fail", e);
                                }
                            }
                        });
                        final Future<?> removed = decodingFutures.put(decodeTask.decodeKey, future);
                        if (removed != null) {
                            Log.e(DECODE_SERVICE, "cancel Decode" + decodeTask);
                            removed.cancel(false);
                        }
                    }
                }
            } else if (what == MSG_DECODE_FINISH) {

            }
            return true;
        }
    };

    public AKDecodeService(CodecContext codecContext) {
        this.codecContext = codecContext;
        initDecodeThread();
    }

    private void initDecodeThread() {
        HandlerThread handlerThread = new HandlerThread("decodeThread");
        handlerThread.start();
        // mHandler = new Handler(handlerThread.getLooper());
        mHandler = new Handler(handlerThread.getLooper(), mCallback);
    }

    public void setContentResolver(ContentResolver contentResolver) {
        this.contentResolver = contentResolver;
        codecContext.setContentResolver(contentResolver);
    }

    public void setContainerView(View containerView) {
        this.containerView = containerView;
    }

    public void open(Uri fileUri) {
        document = codecContext.openDocument(PathFromUri.retrieve(contentResolver, fileUri));
    }

    public CodecDocument getDocument() {
        return document;
    }

    public void decodePage(Object decodeKey, int pageNum, final DecodeCallback decodeCallback, float zoom, RectF pageSliceBounds) {
        final DecodeTask decodeTask = new DecodeTask(pageNum, decodeCallback, zoom, decodeKey, pageSliceBounds);
        /*synchronized (decodingFutures)
        {
            if (isRecycled) {
                return;
            }
            final Future<?> future = executorService.submit(new Runnable()
            {
                public void run()
                {
                    try
                    {
                        Thread.currentThread().setPriority(Thread.NORM_PRIORITY-1);
                        performDecode(decodeTask);
                    }
                    catch (IOException e)
                    {
                        Log.e(DECODE_SERVICE, "Decode fail", e);
                    }
                }
            });
            final Future<?> removed = decodingFutures.put(decodeKey, future);
            if (removed != null)
            {
                removed.cancel(false);
            }
        }*/
        Message message = Message.obtain();
        message.obj = decodeTask;
        message.what = MSG_DECODE_START;
        mHandler.sendMessage(message);
    }

    public void stopDecoding(Object decodeKey) {
        final Future<?> future = decodingFutures.remove(decodeKey);
        if (future != null) {
            future.cancel(false);
        }
    }

    private void performDecode(DecodeTask currentDecodeTask)
            throws IOException {
        if (isTaskDead(currentDecodeTask)) {
            //Log.d(DECODE_SERVICE, "Skipping decode task for page " + currentDecodeTask);
            return;
        }
        //Log.d(DECODE_SERVICE, "Starting decode of page: " + currentDecodeTask +" slice:"+currentDecodeTask.pageSliceBounds);
        CodecPage vuPage = getPage(currentDecodeTask.pageNumber);
        preloadNextPage(currentDecodeTask.pageNumber);

        if (isTaskDead(currentDecodeTask)) {
            //Log.d(DECODE_SERVICE, "Skipping decode when decoding task for page " + currentDecodeTask);
            return;
        }
        //Log.d(DECODE_SERVICE, "Start converting map to bitmap");
        float scale = calculateScale(vuPage) * currentDecodeTask.zoom;
        //Log.d(DECODE_SERVICE, "scale:"+scale+" vuPage.getWidth():"+vuPage.getWidth());
        final Bitmap bitmap = ((PdfPage) vuPage).renderBitmap(getScaledWidth(currentDecodeTask, vuPage, scale),
                getScaledHeight(currentDecodeTask, vuPage, scale), currentDecodeTask.pageSliceBounds, scale);
        //Log.d(DECODE_SERVICE, "Converting map to bitmap finished");
        if (isTaskDead(currentDecodeTask)) {
            //bitmap.recycle();
            return;
        }
        finishDecoding(currentDecodeTask, bitmap);
    }

    private int getScaledHeight(DecodeTask currentDecodeTask, CodecPage vuPage, float scale) {
        return Math.round(getScaledHeight(vuPage, scale) * currentDecodeTask.pageSliceBounds.height());
    }

    private int getScaledWidth(DecodeTask currentDecodeTask, CodecPage vuPage, float scale) {
        return Math.round(getScaledWidth(vuPage, scale) * currentDecodeTask.pageSliceBounds.width());
    }

    private int getScaledHeight(CodecPage vuPage, float scale) {
        return (int) (scale * vuPage.getHeight());
    }

    private int getScaledWidth(CodecPage vuPage, float scale) {
        return (int) (scale * vuPage.getWidth());
    }

    private float calculateScale(CodecPage codecPage) {
        return 1.0f * getTargetWidth() / codecPage.getWidth();
    }

    private void finishDecoding(DecodeTask currentDecodeTask, Bitmap bitmap) {
        updateImage(currentDecodeTask, bitmap);
        //stopDecoding(currentDecodeTask.pageNumber);
        stopDecoding(currentDecodeTask.decodeKey);
    }

    private void preloadNextPage(int pageNumber) throws IOException {
        final int nextPage = pageNumber + 1;
        if (nextPage >= getPageCount()) {
            return;
        }
        getPage(nextPage);
    }

    private CodecPage getPage(int pageIndex) {
        if (null == pages.get(pageIndex) || pages.get(pageIndex).get() == null) {
            pages.put(pageIndex, new SoftReference<CodecPage>(document.getPage(pageIndex)));
            pageEvictionQueue.remove(pageIndex);
            pageEvictionQueue.offer(pageIndex);
            if (pageEvictionQueue.size() > PAGE_POOL_SIZE) {
                Integer evictedPageIndex = pageEvictionQueue.poll();
                CodecPage evictedPage = pages.get(evictedPageIndex).get();
                pages.remove(evictedPageIndex);
                if (evictedPage != null) {
                    evictedPage.recycle();
                }
            }
        }
        return pages.get(pageIndex).get();
    }

    private void waitForDecode(CodecPage vuPage) {
        vuPage.waitForDecode();
    }

    private int getTargetWidth() {
        return containerView.getWidth();
    }

    public int getEffectivePagesWidth() {
        final CodecPage page = getPage(0);
        return getScaledWidth(page, calculateScale(page));
    }

    public int getEffectivePagesHeight() {
        final CodecPage page = getPage(0);
        return getScaledHeight(page, calculateScale(page));
    }

    public int getPageWidth(int pageIndex) {
        return getPage(pageIndex).getWidth();
    }

    public int getPageHeight(int pageIndex) {
        return getPage(pageIndex).getHeight();
    }

    private void updateImage(final DecodeTask currentDecodeTask, Bitmap bitmap) {
        currentDecodeTask.decodeCallback.decodeComplete(bitmap);
    }

    private boolean isTaskDead(DecodeTask currentDecodeTask) {
        synchronized (decodingFutures) {
            return !decodingFutures.containsKey(currentDecodeTask.decodeKey);
        }
    }

    public int getPageCount() {
        if (document == null) {
            return 0;
        }
        return document.getPageCount();
    }

    private class DecodeTask {
        private final Object decodeKey;
        private final int pageNumber;
        private final float zoom;
        private final DecodeCallback decodeCallback;
        private final RectF pageSliceBounds;

        private DecodeTask(int pageNumber, DecodeCallback decodeCallback, float zoom, Object decodeKey, RectF pageSliceBounds) {
            this.pageNumber = pageNumber;
            this.decodeCallback = decodeCallback;
            this.zoom = zoom;
            this.decodeKey = decodeKey;
            this.pageSliceBounds = pageSliceBounds;
        }

        @Override
        public String toString() {
            return "DecodeTask{" +
                    "decodeKey=" + decodeKey +
                    ", pageNumber=" + pageNumber +
                    ", zoom=" + zoom +
                    ", pageSliceBounds=" + pageSliceBounds +
                    '}';
        }
    }

    public void recycle() {
        if (null != mHandler) {
            mHandler.sendEmptyMessage(MSG_DECODE_FINISH);
            mHandler.getLooper().quit();
        }
        synchronized (decodingFutures) {
            isRecycled = true;
        }
        for (Object key : decodingFutures.keySet()) {
            stopDecoding(key);
        }
        executorService.submit(new Runnable() {
            public void run() {
                //for (SoftReference<CodecPage> codecPageSoftReference : pages.values()) {
                int len = pages.size();
                SoftReference<CodecPage> codecPageSoftReference;
                for (int i = 0; i < len; i++) {
                    codecPageSoftReference = pages.valueAt(i);
                    CodecPage page = codecPageSoftReference.get();
                    if (page != null) {
                        page.recycle();
                    }
                }
                document.recycle();
                codecContext.recycle();
                BitmapPool.getInstance().clear();
            }
        });
        executorService.shutdown();
    }

    //=========================

}
