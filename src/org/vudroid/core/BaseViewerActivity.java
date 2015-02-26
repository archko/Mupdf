package org.vudroid.core;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.*;
import android.widget.FrameLayout;
import android.widget.Toast;
import cn.me.archko.pdf.AKRecent;
import cn.me.archko.pdf.DataListener;
import org.vudroid.core.views.PageSeekBarControls;
import com.artifex.mupdfdemo.OutlineActivity;
import com.artifex.mupdfdemo.OutlineActivityData;
import com.artifex.mupdfdemo.R;
import cx.hell.android.pdfviewpro.Bookmark;
import cx.hell.android.pdfviewpro.BookmarkEntry;
import cx.hell.android.pdfviewpro.Options;
import org.vudroid.core.events.CurrentPageListener;
import org.vudroid.core.events.DecodingProgressListener;
import org.vudroid.core.models.CurrentPageModel;
import org.vudroid.core.models.DecodingProgressModel;
import org.vudroid.core.models.ZoomModel;
import org.vudroid.core.views.PageViewZoomControls;

public abstract class BaseViewerActivity extends Activity implements DecodingProgressListener, CurrentPageListener, SensorEventListener
{
    private static final int MENU_EXIT = 0;
    private static final int MENU_GOTO = 1;
    private static final int MENU_FULL_SCREEN = 2;
    private static final int MENU_OPTIONS = 3;
    private static final int MENU_OUTLINE = 4;
    private static final int DIALOG_GOTO = 0;
    private static final String TAG="BaseViewer";
    //private static final String DOCUMENT_VIEW_STATE_PREFERENCES = "DjvuDocumentViewState";
    private DecodeService decodeService;
    private DocumentView documentView;
    //private ViewerPreferences viewerPreferences;
    private Toast pageNumberToast;
    private CurrentPageModel currentPageModel;
    PageViewZoomControls mControls;
    private CurrentPageModel mPageModel;
    PageSeekBarControls mPageSeekBarControls;

    /**
     * Bookmarked page to go to.
     */
    private BookmarkEntry bookmarkToRestore = null;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        initDecodeService();
        final ZoomModel zoomModel = new ZoomModel();
        setStartBookmark();
        if (null!=bookmarkToRestore) {
            zoomModel.setZoom(bookmarkToRestore.absoluteZoomLevel/1000);
        }
        final DecodingProgressModel progressModel = new DecodingProgressModel();
        progressModel.addEventListener(this);
        currentPageModel = new CurrentPageModel();
        currentPageModel.addEventListener(this);
        documentView = new DocumentView(this, zoomModel, progressModel, currentPageModel);
        zoomModel.addEventListener(documentView);
        documentView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        decodeService.setContentResolver(getContentResolver());
        decodeService.setContainerView(documentView);
        documentView.setDecodeService(decodeService);
        decodeService.open(getIntent().getData());

        //viewerPreferences = new ViewerPreferences(this);

        final FrameLayout frameLayout = createMainContainer();
        frameLayout.addView(documentView);
        mControls=createZoomControls(zoomModel);
        frameLayout.addView(mControls);
        //setFullScreen();
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        setContentView(frameLayout);

        /*final SharedPreferences sharedPreferences = getSharedPreferences(DOCUMENT_VIEW_STATE_PREFERENCES, 0);
        documentView.goToPage(sharedPreferences.getInt(getIntent().getData().toString(), 0));*/
        restoreBookmark();
        documentView.showDocument();

        //viewerPreferences.addRecent(getIntent().getData());

        mPageModel=new CurrentPageModel();
        mPageModel.addEventListener(new CurrentPageListener() {
            @Override
            public void currentPageChanged(int pageIndex) {
                Log.d(TAG, "currentPageChanged:"+pageIndex);
                if (documentView.getCurrentPage()!=pageIndex) {
                    documentView.goToPage(pageIndex);
                }
            }
        });
        documentView.setPageModel(mPageModel);
        mPageSeekBarControls=createSeekControls(mPageModel);
        frameLayout.addView(mPageSeekBarControls);
        mPageSeekBarControls.hide();
    }

    public void decodingProgressChanged(final int currentlyDecoding)
    {
        runOnUiThread(new Runnable() {
            public void run() {
                //getWindow().setFeatureInt(Window.FEATURE_INDETERMINATE_PROGRESS, currentlyDecoding == 0 ? 10000 : currentlyDecoding);
            }
        });
    }

    public void currentPageChanged(int pageIndex)
    {
        final String pageText = (pageIndex + 1) + "/" + decodeService.getPageCount();
        if (pageNumberToast != null)
        {
            pageNumberToast.setText(pageText);
        }
        else
        {
            pageNumberToast = Toast.makeText(this, pageText, 80);
        }
        pageNumberToast.setGravity(Gravity.BOTTOM | Gravity.LEFT,0,0);
        pageNumberToast.show();
        //saveCurrentPage();
    }

    private void setWindowTitle()
    {
        final String name = getIntent().getData().getLastPathSegment();
        getWindow().setTitle(name);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState)
    {
        super.onPostCreate(savedInstanceState);
        setWindowTitle();
    }

    private void setFullScreen()
    {
        /*if (viewerPreferences.isFullScreen())
        {
            getWindow().requestFeature(Window.FEATURE_NO_TITLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        else
        {
            getWindow().requestFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        }*/
    }

    private PageViewZoomControls createZoomControls(ZoomModel zoomModel)
    {
        final PageViewZoomControls controls = new PageViewZoomControls(this, zoomModel);
        controls.setGravity(Gravity.RIGHT | Gravity.BOTTOM);
        zoomModel.addEventListener(controls);
        return controls;
    }

    private PageSeekBarControls createSeekControls(CurrentPageModel pageModel)
    {
        final PageSeekBarControls controls = new PageSeekBarControls(this, pageModel);
        controls.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.TOP);
        pageModel.addEventListener(controls);
        return controls;
    }

    private FrameLayout createMainContainer()
    {
        return new FrameLayout(this);
    }

    private void initDecodeService()
    {
        if (decodeService == null)
        {
            decodeService = createDecodeService();
        }
    }

    protected abstract DecodeService createDecodeService();

    @Override
    protected void onStop()
    {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        decodeService.recycle();
        decodeService = null;
        super.onDestroy();
    }

    /*private void saveCurrentPage()
    {
        final SharedPreferences sharedPreferences = getSharedPreferences(DOCUMENT_VIEW_STATE_PREFERENCES, 0);
        final SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(getIntent().getData().toString(), documentView.getCurrentPage());
        editor.commit();
    }*/

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        menu.add(0, MENU_EXIT, 0, "Exit");
        menu.add(1, MENU_GOTO, 0, getString(R.string.goto_page));
        menu.add(2, MENU_OUTLINE, 0, getString(R.string.table_of_contents));
        menu.add(3, MENU_OPTIONS, 0, getString(R.string.options));
        /*final MenuItem menuItem = menu.add(0, MENU_FULL_SCREEN, 0, "Full screen").setCheckable(true).setChecked(viewerPreferences.isFullScreen());
        setFullScreenMenuItemText(menuItem);*/
        return true;
    }

    private void setFullScreenMenuItemText(MenuItem menuItem)
    {
        menuItem.setTitle("Full screen " + (menuItem.isChecked() ? "on" : "off"));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case MENU_EXIT:
                finish();
                return true;
            case MENU_GOTO:
                //showDialog(DIALOG_GOTO);
                mPageModel.setCurrentPage(currentPageModel.getCurrentPageIndex());
                mPageModel.setPageCount(decodeService.getPageCount());
                mPageSeekBarControls.fade();
                return true;
            case MENU_FULL_SCREEN: {
                item.setChecked(!item.isChecked());
                setFullScreenMenuItemText(item);
                //viewerPreferences.setFullScreen(item.isChecked());

                finish();
                startActivity(getIntent());
                return true;
            }
            case MENU_OUTLINE:{
                if (OutlineActivityData.get().items!=null) {
                    Intent intent=new Intent(this, OutlineActivity.class);
                    intent.putExtra("cp", documentView.getCurrentPage());
                    startActivityForResult(intent, OUTLINE_REQUEST);
                }
                return true;
            }
            case MENU_OPTIONS:
                startActivity(new Intent(this, Options.class));
        }
        return false;
    }

    @Override
    protected Dialog onCreateDialog(int id)
    {
        /*switch (id)
        {
            case DIALOG_GOTO:
                return new GoToPageDialog(this, documentView, decodeService);
        }*/
        return null;
    }

    //--------------------------------------

    public void setStartBookmark() {
        Bookmark b = new Bookmark(getApplicationContext()).open();
        Uri uri=getIntent().getData();
        String bookmarkName=Uri.decode(uri.getEncodedPath());
        if (b != null) {
            bookmarkToRestore = b.getLast(bookmarkName);
            Log.d(TAG, "setStartBookmark:"+bookmarkToRestore);
        }
        b.close();
    }

    void restoreBookmark(){
        if (bookmarkToRestore == null) {
            return;
        }

        if (bookmarkToRestore.numberOfPages!=decodeService.getPageCount()&&decodeService.getPageCount()!=0) {
            bookmarkToRestore = null;
            return;
        }

        if (0<bookmarkToRestore.page) {
            int currentPage = bookmarkToRestore.page;
            documentView.goToPage(currentPage, bookmarkToRestore.offsetX);
        }
    }

    private void saveCurrentPage()
    {
        /*final SharedPreferences sharedPreferences = getSharedPreferences(DOCUMENT_VIEW_STATE_PREFERENCES, 0);
        final SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(getIntent().getData().toString(), documentView.getCurrentPage());
        editor.commit();*/
        Uri uri=getIntent().getData();
        String filePath=Uri.decode(uri.getEncodedPath());
        BookmarkEntry entry = toBookmarkEntry();
        Bookmark b = new Bookmark(getApplicationContext()).open();
        b.setLast(filePath, entry);
        b.close();
        Log.i(TAG, "last page saved for "+filePath+" entry:"+entry);
        AKRecent.getInstance(getApplicationContext()).addAsync(filePath, entry.page, entry.numberOfPages, entry.toString(),
            new DataListener() {
                @Override
                public void onSuccess(Object... args) {
                    AKRecent.getInstance(getApplicationContext()).backup("mupdf_recent.jso");
                }

                @Override
                public void onFailed(Object... args) {

                }
            });
    }

    public BookmarkEntry toBookmarkEntry() {
        return new BookmarkEntry(decodeService.getPageCount(),
            documentView.getCurrentPage(), documentView.getZoomModel().getZoom()*1000, 0,
            documentView.getScrollX(), documentView.getScrollY()-documentView.getHeight()/2);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    private SensorManager sensorManager;
    private float[] gravity = { 0f, -9.81f, 0f};
    private long gravityAge = 0;

    private int prevOrientation=ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;

    protected void onResume() {
        super.onResume();

        sensorManager = null;

        SharedPreferences options = PreferenceManager.getDefaultSharedPreferences(this);

        if (Options.setOrientation(this)) {
            sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
            if (sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).size() > 0) {
                gravity[0] = 0f;
                gravity[1] = -9.81f;
                gravity[2] = 0f;
                gravityAge = 0;
                sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                    SensorManager.SENSOR_DELAY_NORMAL);
                prevOrientation = options.getInt(Options.PREF_PREV_ORIENTATION, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                setRequestedOrientation(prevOrientation);
            }
            else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
        }

        if (options.getBoolean(Options.PREF_KEEP_ON, false)) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        /*setZoomLayout(options);

        pagesView.setZoomLayout(zoomLayout);*/

        documentView.setVerticalScrollLock(options.getBoolean(Options.PREF_VERTICAL_SCROLL_LOCK, true));

        /*int zoomAnimNumber = Integer.parseInt(options.getString(Options.PREF_ZOOM_ANIMATION, "2"));

        if (zoomAnimNumber == Options.ZOOM_BUTTONS_DISABLED)
            zoomAnim = null;
        else
            zoomAnim = AnimationUtils.loadAnimation(this,
                zoomAnimations[zoomAnimNumber]);
        int pageNumberAnimNumber = Integer.parseInt(options.getString(Options.PREF_PAGE_ANIMATION, "3"));

        if (pageNumberAnimNumber == Options.PAGE_NUMBER_DISABLED)
            pageNumberAnim = null;
        else
            pageNumberAnim = AnimationUtils.loadAnimation(this,
                pageNumberAnimations[pageNumberAnimNumber]);*/

        if (options.getBoolean(Options.PREF_FULLSCREEN, true)) {
            //getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        mControls.hide();
        int height=documentView.getHeight();
        if (height<=0) {
            height=new ViewConfiguration().getScaledTouchSlop()*2;
        } else {
            height=(int) (height*0.97);
        }
        documentView.setScrollMargin(height);
    }

    @Override
    protected void onPause() {
        super.onPause();

        saveCurrentPage();

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

        float sq0 = gravity[0]*gravity[0];
        float sq1 = gravity[1]*gravity[1];
        float sq2 = gravity[2]*gravity[2];

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
        }
        else if (sq0 > 3 * (sq1 + sq2)) {
            if (gravity[0] > 4)
                setOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            else if (gravity[0] < -4 && Integer.parseInt(Build.VERSION.SDK) >= 9)
                setOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
        }
    }

    //--------------------------------

    private final int OUTLINE_REQUEST=0;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case OUTLINE_REQUEST:
                if (resultCode >= 0)
                    documentView.goToPage(resultCode);
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
