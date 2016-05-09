package cn.archko.pdf;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import org.vudroid.core.events.CurrentPageListener;
import org.vudroid.core.models.CurrentPageModel;
import org.vudroid.core.views.PageSeekBarControls;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import cx.hell.android.pdfviewpro.Bookmark;
import cx.hell.android.pdfviewpro.BookmarkEntry;
import cx.hell.android.pdfviewpro.Options;

/**
 * @author: archko 2016/5/9 :12:43
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class LOLLIPOPPDFActivity extends FragmentActivity implements SensorEventListener {

    private static final String TAG = "LOLLIPOPPDFActivity";
    private String mPath;
    private PdfRenderer mPdfRenderer;
    private int pos = 0;

    int width = 720;
    RecyclerView mRecyclerView;
    private GestureDetector gestureDetector;
    private BookmarkEntry bookmarkToRestore = null;
    private Toast pageNumberToast;

    private CurrentPageModel mPageModel;
    PageSeekBarControls mPageSeekBarControls;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        width = getWindowManager().getDefaultDisplay().getWidth();
        initView();

        if (null != savedInstanceState) {
            mPath = savedInstanceState.getString("path", null);
            pos = savedInstanceState.getInt("pos", 0);
        }

        parseIntent();

        System.out.println("path:" + mPath);

        setStartBookmark();

        try {
            File file = new File(mPath);
            ParcelFileDescriptor fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
            mPdfRenderer = new PdfRenderer(fd);
            restoreBookmark();

            if (pos > 0 && mPdfRenderer.getPageCount() > pos) {
                //mPdfRenderer.openPage(pos);
                mRecyclerView.scrollToPosition(pos);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void parseIntent() {
        if (TextUtils.isEmpty(mPath)) {
            Intent intent = getIntent();

            if (Intent.ACTION_VIEW.equals(intent.getAction())) {
                Uri uri = intent.getData();
                System.out.println("URI to open is: " + uri);
                if (uri.toString().startsWith("content://")) {
                    String reason = null;
                    Cursor cursor = null;
                    try {
                        cursor = getContentResolver().query(uri, new String[]{"_data"}, null, null, null);
                        if (cursor.moveToFirst()) {
                            String str = cursor.getString(0);
                            if (str == null) {
                                reason = "Couldn't parse data in intent";
                            } else {
                                uri = Uri.parse(str);
                            }
                        }
                    } catch (Exception e2) {
                        System.out.println("Exception in Transformer Prime file manager code: " + e2);
                        reason = e2.toString();
                    } finally {
                        if (null != cursor) {
                            cursor.close();
                        }
                    }
                }
                String path = Uri.decode(uri.getEncodedPath());
                if (path == null) {
                    path = uri.toString();
                }
                mPath = path;
            } else {
                if (!TextUtils.isEmpty(getIntent().getStringExtra("path"))) {
                    mPath = getIntent().getStringExtra("path");
                }
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("path", mPath);
        outState.putInt("pos", pos);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(HistoryFragment.ACTION_STOPPED));
    }

    private void initView() {
        mRecyclerView = new RecyclerView(this);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        mRecyclerView.setAdapter(new BaseRecyclerAdapter());
        mRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL_LIST));

        mRecyclerView.setBackgroundColor(Color.WHITE);

        final FrameLayout frameLayout = new FrameLayout(this);
        frameLayout.addView(mRecyclerView);

        setContentView(frameLayout);

        initTouchParams();
        mRecyclerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return gestureDetector.onTouchEvent(event);
            }
        });

        mPageModel = new CurrentPageModel();
        mPageModel.addEventListener(new CurrentPageListener() {
            @Override
            public void currentPageChanged(int pageIndex) {
                Log.d(TAG, "currentPageChanged:" + pageIndex);
                if (pos != pageIndex) {
                    mRecyclerView.scrollToPosition(pageIndex);
                }
            }
        });
        mPageSeekBarControls = createSeekControls(mPageModel);
        frameLayout.addView(mPageSeekBarControls);
        mPageSeekBarControls.hide();
    }

    void initTouchParams() {
        int margin = mRecyclerView.getHeight();
        if (margin <= 0) {
            margin = ViewConfiguration.get(this).getScaledTouchSlop() * 2;
        } else {
            margin = (int) (margin * 0.03);
        }
        gestureDetector = new GestureDetector(this, new GestureDetector.OnGestureListener() {

            @Override
            public boolean onDown(MotionEvent e) {
                return false;
            }

            @Override
            public void onShowPress(MotionEvent e) {

            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                return false;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                return false;
            }

            @Override
            public void onLongPress(MotionEvent e) {

            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                return false;
            }
        });

        final int finalMargin = margin;
        gestureDetector.setOnDoubleTapListener(new GestureDetector.OnDoubleTapListener() {

            public boolean onSingleTapConfirmed(MotionEvent e) {
                int top = mRecyclerView.getHeight() / 4;
                int bottom = mRecyclerView.getHeight() * 3 / 4;

                if ((int) e.getY() < top) {
                    int scrollY = mRecyclerView.getScrollY();
                    scrollY -= mRecyclerView.getHeight();
                    mRecyclerView.scrollBy(0, scrollY + finalMargin);
                    return true;
                } else if ((int) e.getY() > bottom) {
                    int scrollY = mRecyclerView.getScrollY();
                    scrollY += mRecyclerView.getHeight();
                    mRecyclerView.scrollBy(0, scrollY - finalMargin);
                    return true;
                } else {
                    final String pageText = (pos + 1) + "/" + mPdfRenderer.getPageCount();
                    if (pageNumberToast != null) {
                        pageNumberToast.setText(pageText);
                    } else {
                        pageNumberToast = Toast.makeText(LOLLIPOPPDFActivity.this, pageText, 80);
                    }
                    pageNumberToast.setGravity(Gravity.BOTTOM | Gravity.LEFT, 0, 0);
                    pageNumberToast.show();
                }
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                mPageModel.setCurrentPage(pos);
                mPageModel.setPageCount(mPdfRenderer.getPageCount());
                mPageModel.toggleSeekControls();
                return true;
            }

            @Override
            public boolean onDoubleTapEvent(MotionEvent e) {
                return false;
            }
        });
    }

    private PageSeekBarControls createSeekControls(CurrentPageModel pageModel) {
        final PageSeekBarControls controls = new PageSeekBarControls(this, pageModel);
        controls.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.TOP);
        pageModel.addEventListener(controls);
        return controls;
    }

    //--------------------------------------

    public void setStartBookmark() {
        AKProgress progress = AKRecent.getInstance(getApplicationContext()).readRecentFromDb(mPath);
        if (null != progress) {
            BookmarkEntry entry = new BookmarkEntry(progress.bookmarkEntry);
            bookmarkToRestore = entry;
        }
    }

    void restoreBookmark() {
        if (bookmarkToRestore == null || mPdfRenderer == null || mPdfRenderer.getPageCount() <= 0) {
            return;
        }

        if (bookmarkToRestore.numberOfPages != mPdfRenderer.getPageCount() || bookmarkToRestore.page > mPdfRenderer.getPageCount()) {
            bookmarkToRestore = null;
            return;
        }

        if (0 < bookmarkToRestore.page) {
            pos = bookmarkToRestore.page;
        }
    }

    private void saveCurrentPage() {
        String filePath = Uri.decode(mPath);
        BookmarkEntry entry = toBookmarkEntry();
        Bookmark b = new Bookmark(getApplicationContext()).open();
        b.setLast(filePath, entry);
        b.close();
        Log.i(TAG, "last page saved for " + filePath + " entry:" + entry);
        AKRecent.getInstance(getApplicationContext()).addAsyncToDB(filePath, entry.page, entry.numberOfPages, entry.toString(),
                new DataListener() {
                    @Override
                    public void onSuccess(Object... args) {
                        //AKRecent.getInstance(getApplicationContext()).backup("mupdf_recent.jso");
                    }

                    @Override
                    public void onFailed(Object... args) {

                    }
                });
    }

    public BookmarkEntry toBookmarkEntry() {
        if (null != bookmarkToRestore) {
            bookmarkToRestore.page = pos;
            bookmarkToRestore.numberOfPages = mPdfRenderer.getPageCount();
            return bookmarkToRestore;
        }
        return new BookmarkEntry(mPdfRenderer.getPageCount(),
                pos, 1 * 1000, 0,
                0, 0);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    private SensorManager sensorManager;
    private float[] gravity = {0f, -9.81f, 0f};
    private long gravityAge = 0;

    private int prevOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;

    @Override
    protected void onResume() {
        super.onResume();

        sensorManager = null;

        SharedPreferences options = PreferenceManager.getDefaultSharedPreferences(this);

        if (Options.setOrientation(this)) {
            sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
            if (sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).size() > 0) {
                gravity[0] = 0f;
                gravity[1] = -9.81f;
                gravity[2] = 0f;
                gravityAge = 0;
                sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                        SensorManager.SENSOR_DELAY_NORMAL);
                prevOrientation = options.getInt(Options.PREF_PREV_ORIENTATION, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                setRequestedOrientation(prevOrientation);
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
        }

        if (options.getBoolean(Options.PREF_KEEP_ON, false)) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        //documentView.setVerticalScrollLock(options.getBoolean(Options.PREF_VERTICAL_SCROLL_LOCK, true));
        if (options.getBoolean(Options.PREF_FULLSCREEN, true)) {
            //getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        //saveCurrentPage();

        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
            sensorManager = null;
            SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(this).edit();
            edit.putInt(Options.PREF_PREV_ORIENTATION, prevOrientation);
            //Log.v(TAG, "prevOrientation saved: "+prevOrientation);
            edit.apply();
        }
    }

    private void setOrientation(int orientation) {
        if (orientation != prevOrientation) {
            //Log.v(TAG, "setOrientation: "+orientation);
            setRequestedOrientation(orientation);
            prevOrientation = orientation;
        }
    }

    /**
     * Called when accuracy changes.
     * This method is empty, but it's required by relevant interface.
     */
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public void onSensorChanged(SensorEvent event) {
        gravity[0] = 0.8f * gravity[0] + 0.2f * event.values[0];
        gravity[1] = 0.8f * gravity[1] + 0.2f * event.values[1];
        gravity[2] = 0.8f * gravity[2] + 0.2f * event.values[2];

        float sq0 = gravity[0] * gravity[0];
        float sq1 = gravity[1] * gravity[1];
        float sq2 = gravity[2] * gravity[2];

        gravityAge++;

        if (gravityAge < 4) {
            // ignore initial hiccups
            return;
        }

        if (sq1 > 3 * (sq0 + sq2)) {
            if (gravity[1] > 4)
                setOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            else if (gravity[1] < -4 && Integer.parseInt(Build.VERSION.SDK) >= 9)
                setOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
        } else if (sq0 > 3 * (sq1 + sq2)) {
            if (gravity[0] > 4)
                setOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            else if (gravity[0] < -4 && Integer.parseInt(Build.VERSION.SDK) >= 9)
                setOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
        }
    }

    //===========================================

    class BaseRecyclerAdapter<T> extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, final int viewType) {
            Holder holder = null;
            ImageView view = new ImageView(getBaseContext());
            ViewGroup.LayoutParams lp = view.getLayoutParams();
            if (null == lp) {
                lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                view.setLayoutParams(lp);
            }
            holder = new Holder(view);
            return holder;
        }


        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
            PdfRenderer.Page page = mPdfRenderer.openPage(position);

            float f = (float) width / page.getWidth();
            Bitmap bitmap = Bitmap.createBitmap((int) (page.getWidth() * f), (int) (page.getHeight() * f), Bitmap.Config.ARGB_8888);
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            page.close();
            ((Holder) viewHolder).imageView.setImageBitmap(bitmap);
            ((Holder) viewHolder).imageView.setAdjustViewBounds(true);
            pos = viewHolder.getAdapterPosition();
        }


        @Override
        public int getItemCount() {
            return mPdfRenderer.getPageCount();
        }

        public class Holder extends RecyclerView.ViewHolder {
            ImageView imageView;

            public Holder(ImageView itemView) {
                super(itemView);
                imageView = itemView;
            }
        }

    }
}
