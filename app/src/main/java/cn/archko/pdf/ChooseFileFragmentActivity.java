package cn.archko.pdf;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.Toast;

import cx.hell.android.pdfviewpro.APVApplication;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Minimalistic file browser.
 *
 * @author archko
 */
public class ChooseFileFragmentActivity extends FragmentActivity {

    /**
     * Logging tag.
     */
    private final static String TAG = "pdfviewpro";

    public final static String PREF_TAG = "ChooseFileActivity";
    public final static String PREF_HOME = "Home";

    ViewPager mViewPager;
    TabsAdapter mPagerAdapter;
    final String[] titles = new String[2];
    private static final int REQUEST_PERMISSION_CODE = 0x001;

    /**
     * List of {@link SamplePagerItem} which represent this sample's tabs.
     */
    protected List<SamplePagerItem> mTabs = new ArrayList<SamplePagerItem>();
    /**
     * A custom {@link android.support.v4.view.ViewPager} title strip which looks much like Tabs present in Android v4.0 and
     * above, but is designed to give continuous feedback to the user when scrolling.
     */
    protected SlidingTabLayout mSlidingTabLayout;
    private MenuItem searchMenuItem;

    /**
     * This class represents a tab to be displayed by {@link android.support.v4.view.ViewPager} and it's associated
     * {@link cn.archko.pdf.SlidingTabLayout}.
     */
    static class SamplePagerItem {

        private final CharSequence mTitle;
        private final int mIndicatorColor;
        private final int mDividerColor;
        private final Class<?> clss;
        private final Bundle args;

        SamplePagerItem(Class<?> _class, Bundle _args, CharSequence title) {
            clss = _class;
            args = _args;
            mTitle = title;
            mIndicatorColor = APVApplication.getInstance().getResources().getColor(R.color.tab_indicator_selected_color);
            mDividerColor = APVApplication.getInstance().getResources().getColor(R.color.tab_divider_color);
        }

        SamplePagerItem(Class<?> _class, Bundle _args, CharSequence title, int indicatorColor, int dividerColor) {
            clss = _class;
            args = _args;
            mTitle = title;
            mIndicatorColor = indicatorColor;
            mDividerColor = dividerColor;
        }

        /**
         * @return the title which represents this tab. In this sample this is used directly by
         * {@link android.support.v4.view.PagerAdapter#getPageTitle(int)}
         */
        CharSequence getTitle() {
            return mTitle;
        }

        /**
         * @return the color to be used for indicator on the {@link cn.archko.pdf.SlidingTabLayout}
         */
        int getIndicatorColor() {
            return mIndicatorColor;
        }

        /**
         * @return the color to be used for right divider on the {@link cn.archko.pdf.SlidingTabLayout}
         */
        int getDividerColor() {
            return mDividerColor;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.fragment_tabs_pager);

        checkSdcardPermission();
    }

    private void loadView() {
        mViewPager = (ViewPager) findViewById(R.id.pager);

        mSlidingTabLayout = (SlidingTabLayout) findViewById(R.id.sliding_tabs);
        addTab();
        mPagerAdapter = new TabsAdapter(this);
        mViewPager.setOnPageChangeListener(mPagerAdapter);
        mViewPager.setAdapter(mPagerAdapter);

        postSlidingTabLayout();
        // END_INCLUDE (tab_colorizer)
        // END_INCLUDE (setup_slidingtablayout)
        mSlidingTabLayout.setViewPager(mViewPager);
        mSlidingTabLayout.setSelectPageListener(new SlidingTabLayout.SelectPageListener() {
            @Override
            public void updateSubView(int position) {
                ((RefreshableFragment) mPagerAdapter.getItem(position)).update();
            }
        });
    }


    public void checkSdcardPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // WRITE_EXTERNAL_STORAGE permission has not been granted.

            requestSdcardPermission();
        } else {
            loadView();
        }
    }

    /**
     * Requests the sdcard permission.
     * If the permission has been denied previously, a SnackBar will prompt the user to grant the
     * permission, otherwise it is requested directly.
     */
    private void requestSdcardPermission() {
        Log.i(TAG, "sdcard permission has NOT been granted. Requesting permission.");

        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            // Provide an additional rationale to the user if the permission was not granted
            // and the user would benefit from additional context for the use of the permission.
            // For example if the user has previously denied the permission.
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_PERMISSION_CODE);
        } else {

            // sdcard permission has not been granted yet. Request it directly.
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_PERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //  权限通过
                //((RefreshableFragment) (mPagerAdapter.getItem(mViewPager.getCurrentItem()))).update();
                loadView();
            } else {
                // 权限拒绝
                Toast.makeText(this, "没有获取sdcard的读取权限", Toast.LENGTH_LONG).show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    }

    protected void postSlidingTabLayout() {
        mSlidingTabLayout.setMatchWidth(true);
        // BEGIN_INCLUDE (tab_colorizer)
        // Set a TabColorizer to customize the indicator and divider colors. Here we just retrieve
        // the tab at the position, and return it's set color
        mSlidingTabLayout.setCustomTabColorizer(new SlidingTabLayout.TabColorizer() {

            @Override
            public int getIndicatorColor(int position) {
//                LLog.d("JSSlidingTabsColorsFragment", "*****getIndicatorColor******"+mTabs.get(position).getTitle());
                return mTabs.get(position).getIndicatorColor();
            }

            @Override
            public int getDividerColor(int position) {
//                LLog.d("JSSlidingTabsColorsFragment", "******getDividerColor*****"+mTabs.get(position).getTitle());
                return mTabs.get(position).getDividerColor();
            }

        });
    }

    protected void addTab() {
        titles[0] = getString(R.string.tab_history);
        titles[1] = getString(R.string.tab_browser);

        String title = titles[0];
        Bundle bundle = new Bundle();
        mTabs.add(new SamplePagerItem(HistoryFragment.class, bundle, title));

        title = titles[1];
        bundle = new Bundle();
        mTabs.add(new SamplePagerItem(BrowserFragment.class, bundle, title));
    }

    @Override
    public void onBackPressed() {
        if (null != mPagerAdapter) {
            BrowserFragment fragment = (BrowserFragment) mPagerAdapter.getItem(mViewPager.getCurrentItem());
            if (fragment.onBackPressed()) {
                return;
            }
        }
        super.onBackPressed();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean flag= super.onCreateOptionsMenu(menu);
        this.searchMenuItem = menu.add(R.string.menu_search);
        MenuItemCompat.setShowAsAction(this.searchMenuItem, MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
        return flag;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem == this.searchMenuItem) {
            showSearchDialog();
        }
        return super.onOptionsItemSelected(menuItem);
    }


    protected void showSearchDialog() {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment prev = getSupportFragmentManager().findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        // Create and show the dialog.
        SearchFragment fileInfoFragment = new SearchFragment();
        Bundle bundle = new Bundle();
        fileInfoFragment.setArguments(bundle);
        fileInfoFragment.show(ft, "dialog");
    }

    /**
     * The {@link android.support.v4.app.FragmentPagerAdapter} used to display pages in this sample. The individual pages
     * are instances of {@link ContentFragment} which just display three lines of text. Each page is
     * created by the relevant {@link SamplePagerItem} for the requested position.
     * <p/>
     * The important section of this class is the {@link #getPageTitle(int)} method which controls
     * what is displayed in the {@link SlidingTabLayout}.
     */
    public class TabsAdapter extends FragmentPagerAdapter
            implements ViewPager.OnPageChangeListener {
        Handler mHandler = new Handler();
        private final Context mContext;
        private final SparseArray<WeakReference<Fragment>> mFragmentArray = new SparseArray<WeakReference<Fragment>>();
        int position = 0;

        public TabsAdapter(FragmentActivity activity) {
            super(activity.getSupportFragmentManager());
            mContext = activity;
        }

        @Override
        public int getCount() {
            return mTabs.size();
        }

        @Override
        public Fragment getItem(int position) {
            final WeakReference<Fragment> mWeakFragment = mFragmentArray.get(position);
            if (mWeakFragment != null && mWeakFragment.get() != null) {
                return mWeakFragment.get();
            }

            SamplePagerItem info = mTabs.get(position);
            return Fragment.instantiate(mContext, info.clss.getName(), info.args);
        }

        @Override
        public Object instantiateItem(final ViewGroup container, final int position) {
            Log.v(TAG, "instantiateItem:" + position);
            WeakReference<Fragment> mWeakFragment = mFragmentArray.get(position);
            if (mWeakFragment != null && mWeakFragment.get() != null) {
                //mWeakFragment.clear();
                return mWeakFragment.get();
            }

            final Fragment mFragment = (Fragment) super.instantiateItem(container, position);
            mFragmentArray.put(position, new WeakReference<Fragment>(mFragment));
            return mFragment;
        }

        @Override
        public void destroyItem(final ViewGroup container, final int position, final Object object) {
            super.destroyItem(container, position, object);
            final WeakReference<Fragment> mWeakFragment = mFragmentArray.get(position);
            if (mWeakFragment != null) {
                mWeakFragment.clear();
            }
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
            mViewPager.setCurrentItem(position);
            if (position == 0) {
                APVApplication apvApplication = APVApplication.getInstance();
                if (apvApplication.hasChanged) {
                    delayUpdate(position);
                    apvApplication.hasChanged = false;
                }
            } else if (position == 2) {
                delayUpdate(position);
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mTabs.get(position).getTitle();
        }

        void delayUpdate(final int position) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    RefreshableFragment fragment = (RefreshableFragment) getItem(position);
                    fragment.update();
                }
            }, 500L);
        }
    }
}
