package org.vudroid.core;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import cn.archko.pdf.PDFBookmarkManager;
import cn.archko.pdf.SensorHelper;
import org.vudroid.core.events.CurrentPageListener;
import org.vudroid.core.events.DecodingProgressListener;
import org.vudroid.core.events.PageViewPresenter;
import org.vudroid.core.models.CurrentPageModel;
import org.vudroid.core.models.DecodingProgressModel;
import org.vudroid.core.models.ZoomModel;
import org.vudroid.core.views.APageSeekBarControls;
import org.vudroid.core.views.PageViewZoomControls;

import cn.archko.pdf.R;
import cx.hell.android.pdfviewpro.Options;

public abstract class BaseViewerActivity extends Activity implements DecodingProgressListener, CurrentPageListener {
    private static final int MENU_EXIT = 0;
    private static final int MENU_GOTO = 1;
    private static final int MENU_FULL_SCREEN = 2;
    private static final int MENU_OPTIONS = 3;
    private static final int MENU_OUTLINE = 4;
    private static final int DIALOG_GOTO = 0;
    private static final String TAG = "BaseViewer";
    //private static final String DOCUMENT_VIEW_STATE_PREFERENCES = "DjvuDocumentViewState";
    private DecodeService decodeService;
    private DocumentView documentView;
    //private ViewerPreferences viewerPreferences;
    private Toast pageNumberToast;
    private CurrentPageModel currentPageModel;
    PageViewZoomControls mControls;
    //private CurrentPageModel mPageModel;
    APageSeekBarControls mPageSeekBarControls;

    PDFBookmarkManager pdfBookmarkManager;
    SensorHelper sensorHelper;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initDecodeService();
        final ZoomModel zoomModel = new ZoomModel();
        pdfBookmarkManager=new PDFBookmarkManager();
        sensorHelper=new SensorHelper(this);

        Uri uri = getIntent().getData();
        pdfBookmarkManager.setStartBookmark(Uri.decode(uri.getEncodedPath()));
        if (null != pdfBookmarkManager.getBookmarkToRestore()) {
            zoomModel.setZoom(pdfBookmarkManager.getBookmarkToRestore().absoluteZoomLevel / 1000);
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
        mControls = createZoomControls(zoomModel);
        frameLayout.addView(mControls);
        //setFullScreen();
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        setContentView(frameLayout);

        /*final SharedPreferences sharedPreferences = getSharedPreferences(DOCUMENT_VIEW_STATE_PREFERENCES, 0);
        documentView.goToPage(sharedPreferences.getInt(getIntent().getData().toString(), 0));*/
        int currentPage=pdfBookmarkManager.restoreBookmark(decodeService.getPageCount());
        if (0 < currentPage) {
            documentView.goToPage(currentPage, pdfBookmarkManager.getBookmarkToRestore().offsetX, pdfBookmarkManager.getBookmarkToRestore().offsetY);
        }
        documentView.showDocument();

        //viewerPreferences.addRecent(getIntent().getData());

        /*mPageModel=new CurrentPageModel();
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
        mPageSeekBarControls=createSeekControls(mPageModel);*/
        mPageSeekBarControls = new APageSeekBarControls(this, new PageViewPresenter() {
            @Override
            public int getPageCount() {
                return decodeService.getPageCount();
            }

            @Override
            public int getCurrentPageIndex() {
                return documentView.getCurrentPage();
            }

            @Override
            public void goToPageIndex(int page) {
                documentView.goToPage(page);
            }

            @Override
            public void showOutline() {
                openOutline();
            }

            @Override
            public void back() {
                BaseViewerActivity.this.finish();
            }

            @Override
            public String getTitle() {
                Uri uri = getIntent().getData();
                String filePath = Uri.decode(uri.getEncodedPath());
                return filePath;
            }

            @Override
            public void reflow() {

            }
        });
        documentView.setAPageSeekBarControls(mPageSeekBarControls);
        frameLayout.addView(mPageSeekBarControls);
        mPageSeekBarControls.hide();
        mPageSeekBarControls.setReflow(true);
    }

    public void decodingProgressChanged(final int currentlyDecoding) {
        runOnUiThread(new Runnable() {
            public void run() {
                //getWindow().setFeatureInt(Window.FEATURE_INDETERMINATE_PROGRESS, currentlyDecoding == 0 ? 10000 : currentlyDecoding);
            }
        });
    }

    public void currentPageChanged(int pageIndex) {
        final String pageText = (pageIndex + 1) + "/" + decodeService.getPageCount();
        if (pageNumberToast != null) {
            pageNumberToast.setText(pageText);
        } else {
            pageNumberToast = Toast.makeText(this, pageText, Toast.LENGTH_SHORT);
        }
        pageNumberToast.setGravity(Gravity.BOTTOM | Gravity.LEFT, 0, 0);
        pageNumberToast.show();
        //saveCurrentPage();
    }

    private void setWindowTitle() {
        final String name = getIntent().getData().getLastPathSegment();
        getWindow().setTitle(name);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        setWindowTitle();
    }

    private void setFullScreen() {
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

    private PageViewZoomControls createZoomControls(ZoomModel zoomModel) {
        final PageViewZoomControls controls = new PageViewZoomControls(this, zoomModel);
        controls.setGravity(Gravity.RIGHT | Gravity.BOTTOM);
        zoomModel.addEventListener(controls);
        return controls;
    }

    /*private PageSeekBarControls createSeekControls(CurrentPageModel pageModel)
    {
        final PageSeekBarControls controls = new APageSeekBarControls(this, pageModel);
        //controls.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.TOP);
        pageModel.addEventListener(controls);
        return controls;
    }*/

    private FrameLayout createMainContainer() {
        return new FrameLayout(this);
    }

    private void initDecodeService() {
        if (decodeService == null) {
            decodeService = createDecodeService();
        }
    }

    protected abstract DecodeService createDecodeService();

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        decodeService.recycle();
        decodeService = null;
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_EXIT, 0, "Exit");
        menu.add(1, MENU_GOTO, 0, getString(R.string.goto_page));
        menu.add(2, MENU_OUTLINE, 0, getString(R.string.table_of_contents));
        menu.add(3, MENU_OPTIONS, 0, getString(R.string.options));
        /*final MenuItem menuItem = menu.add(0, MENU_FULL_SCREEN, 0, "Full screen").setCheckable(true).setChecked(viewerPreferences.isFullScreen());
        setFullScreenMenuItemText(menuItem);*/
        return true;
    }

    private void setFullScreenMenuItemText(MenuItem menuItem) {
        menuItem.setTitle("Full screen " + (menuItem.isChecked() ? "on" : "off"));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_EXIT:
                finish();
                return true;
            case MENU_GOTO:
                //showDialog(DIALOG_GOTO);
                //mPageModel.setCurrentPage(currentPageModel.getCurrentPageIndex());
                //mPageModel.setPageCount(decodeService.getPageCount());
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
            case MENU_OUTLINE: {
                openOutline();
                return true;
            }
            case MENU_OPTIONS:
                startActivity(new Intent(this, Options.class));
        }
        return false;
    }

    public void openOutline() {
    }

    public DecodeService getDecodeService() {
        return decodeService;
    }

    public DocumentView getDocumentView() {
        return documentView;
    }

    public APageSeekBarControls getPageSeekBarControls() {
        return mPageSeekBarControls;
    }

    //--------------------------------------

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    protected void onResume() {
        super.onResume();

        sensorHelper.onResume();
        SharedPreferences options = PreferenceManager.getDefaultSharedPreferences(this);

        if (options.getBoolean(Options.PREF_KEEP_ON, false)) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
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
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        mControls.hide();
        int height = documentView.getHeight();
        if (height <= 0) {
            height = new ViewConfiguration().getScaledTouchSlop() * 2;
        } else {
            height = (int) (height * 0.03);
        }
        documentView.setScrollMargin(height);
        documentView.setDecodePage(options.getBoolean(Options.PREF_RENDER_AHEAD, true) ? 1 : 0);
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
        Uri uri = getIntent().getData();
        String filePath = Uri.decode(uri.getEncodedPath());
        pdfBookmarkManager.saveCurrentPage(filePath,decodeService.getPageCount(),documentView.getCurrentPage(),
            documentView.getZoomModel().getZoom()*1000f, documentView.getScrollX(), documentView.getScrollY());

        sensorHelper.onPause();
    }

    //--------------------------------

    protected final int OUTLINE_REQUEST = 0;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case OUTLINE_REQUEST:
                if (resultCode >= 0)
                    documentView.goToPage(resultCode);
                mPageSeekBarControls.hide();
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
