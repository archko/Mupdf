package cn.archko.pdf

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.MenuItemCompat
import android.support.v4.view.ViewPager
import android.util.Log
import android.util.SparseArray
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.Toast
import cx.hell.android.pdfviewpro.APVApplication
import java.lang.ref.WeakReference
import java.util.*

/**
 * Minimalistic file browser.

 * @author archko
 */
open class ChooseFileFragmentActivity : FragmentActivity() {

    internal var mViewPager: ViewPager? = null
    internal var mPagerAdapter: TabsAdapter? = null
    internal val titles = arrayOfNulls<String>(2)

    /**
     * List of [SamplePagerItem] which represent this sample's tabs.
     */
    internal var mTabs: MutableList<SamplePagerItem> = ArrayList()
    /**
     * A custom [android.support.v4.view.ViewPager] title strip which looks much like Tabs present in Android v4.0 and
     * above, but is designed to give continuous feedback to the user when scrolling.
     */
    protected var mSlidingTabLayout: SlidingTabLayout? = null
    private var searchMenuItem: MenuItem? = null

    /**
     * This class represents a tab to be displayed by [android.support.v4.view.ViewPager] and it's associated
     * [cn.archko.pdf.SlidingTabLayout].
     */
    internal class SamplePagerItem {

        /**
         * @return the title which represents this tab. In this sample this is used directly by
         * * [android.support.v4.view.PagerAdapter.getPageTitle]
         */
        val title: CharSequence
        /**
         * @return the color to be used for indicator on the [cn.archko.pdf.SlidingTabLayout]
         */
        val indicatorColor: Int
        /**
         * @return the color to be used for right divider on the [cn.archko.pdf.SlidingTabLayout]
         */
        val dividerColor: Int
        val clss: Class<*>
        val args: Bundle

        constructor(_class: Class<*>, _args: Bundle, title: CharSequence) {
            clss = _class
            args = _args
            this.title = title
            indicatorColor = APVApplication.getInstance().resources.getColor(R.color.tab_indicator_selected_color)
            dividerColor = APVApplication.getInstance().resources.getColor(R.color.tab_divider_color)
        }

        constructor(_class: Class<*>, _args: Bundle, title: CharSequence, indicatorColor: Int, dividerColor: Int) {
            clss = _class
            args = _args
            this.title = title
            this.indicatorColor = indicatorColor
            this.dividerColor = dividerColor
        }
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.fragment_tabs_pager)

        checkSdcardPermission()
    }

    private fun loadView() {
        mViewPager = findViewById<ViewPager>(R.id.pager)

        mSlidingTabLayout = findViewById<SlidingTabLayout>(R.id.sliding_tabs)
        addTab()
        mPagerAdapter = TabsAdapter(this)
        mViewPager!!.setOnPageChangeListener(mPagerAdapter)
        mViewPager!!.adapter = mPagerAdapter

        postSlidingTabLayout()
        // END_INCLUDE (tab_colorizer)
        // END_INCLUDE (setup_slidingtablayout)
        mSlidingTabLayout!!.setViewPager(mViewPager)
        mSlidingTabLayout!!.setSelectPageListener { position -> (mPagerAdapter!!.getItem(position) as RefreshableFragment).update() }
    }


    fun checkSdcardPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // WRITE_EXTERNAL_STORAGE permission has not been granted.

            requestSdcardPermission()
        } else {
            loadView()
        }
    }

    /**
     * Requests the sdcard permission.
     * If the permission has been denied previously, a SnackBar will prompt the user to grant the
     * permission, otherwise it is requested directly.
     */
    private fun requestSdcardPermission() {
        Log.i(TAG, "sdcard permission has NOT been granted. Requesting permission.")

        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            // Provide an additional rationale to the user if the permission was not granted
            // and the user would benefit from additional context for the use of the permission.
            // For example if the user has previously denied the permission.
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    REQUEST_PERMISSION_CODE)
        } else {

            // sdcard permission has not been granted yet. Request it directly.
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    REQUEST_PERMISSION_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //  权限通过
                //((RefreshableFragment) (mPagerAdapter.getItem(mViewPager.getCurrentItem()))).update();
                loadView()
            } else {
                // 权限拒绝
                Toast.makeText(this, "没有获取sdcard的读取权限", Toast.LENGTH_LONG).show()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

    }

    protected fun postSlidingTabLayout() {
        mSlidingTabLayout!!.setMatchWidth(true)
        // BEGIN_INCLUDE (tab_colorizer)
        // Set a TabColorizer to customize the indicator and divider colors. Here we just retrieve
        // the tab at the position, and return it's set color
        mSlidingTabLayout!!.setCustomTabColorizer(object : SlidingTabLayout.TabColorizer {

            override fun getIndicatorColor(position: Int): Int {
                //                LLog.d("JSSlidingTabsColorsFragment", "*****getIndicatorColor******"+mTabs.get(position).getTitle());
                return mTabs[position].indicatorColor
            }

            override fun getDividerColor(position: Int): Int {
                //                LLog.d("JSSlidingTabsColorsFragment", "******getDividerColor*****"+mTabs.get(position).getTitle());
                return mTabs[position].dividerColor
            }

        })
    }

    protected fun addTab() {
        titles[0] = getString(R.string.tab_history)
        titles[1] = getString(R.string.tab_browser)

        var title = titles[0]
        var bundle = Bundle()
        mTabs.add(SamplePagerItem(HistoryFragment::class.java, bundle, title!!))

        title = titles[1]
        bundle = Bundle()
        mTabs.add(SamplePagerItem(BrowserFragment::class.java, bundle, title!!))
    }

    override fun onBackPressed() {
        if (null != mPagerAdapter) {
            val fragment = mPagerAdapter!!.getItem(mViewPager!!.currentItem) as BrowserFragment
            if (fragment.onBackPressed()) {
                return
            }
        }
        super.onBackPressed()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val flag = super.onCreateOptionsMenu(menu)
        this.searchMenuItem = menu.add(R.string.menu_search)
        MenuItemCompat.setShowAsAction(this.searchMenuItem, MenuItemCompat.SHOW_AS_ACTION_IF_ROOM)
        return flag
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        if (menuItem === this.searchMenuItem) {
            showSearchDialog()
        }
        return super.onOptionsItemSelected(menuItem)
    }


    protected fun showSearchDialog() {
        val ft = supportFragmentManager.beginTransaction()
        val prev = supportFragmentManager.findFragmentByTag("dialog")
        if (prev != null) {
            ft.remove(prev)
        }
        ft.addToBackStack(null)

        // Create and show the dialog.
        val fileInfoFragment = SearchFragment()
        val bundle = Bundle()
        fileInfoFragment.arguments = bundle
        fileInfoFragment.show(ft, "dialog")
    }

    /**
     * The [android.support.v4.app.FragmentPagerAdapter] used to display pages in this sample. The individual pages
     * are instances of [ContentFragment] which just display three lines of text. Each page is
     * created by the relevant [SamplePagerItem] for the requested position.
     *
     *
     * The important section of this class is the [.getPageTitle] method which controls
     * what is displayed in the [SlidingTabLayout].
     */
    inner class TabsAdapter(activity: FragmentActivity) : FragmentPagerAdapter(activity.supportFragmentManager), ViewPager.OnPageChangeListener {
        internal var mHandler = Handler()
        private val mContext: Context
        private val mFragmentArray = SparseArray<WeakReference<Fragment>>()
        internal var position = 0

        init {
            mContext = activity
        }

        override fun getCount(): Int {
            return mTabs.size
        }

        override fun getItem(position: Int): Fragment {
            val mWeakFragment = mFragmentArray.get(position)
            if (mWeakFragment != null && mWeakFragment.get() != null) {
                return mWeakFragment.get()!!
            }

            val info = mTabs[position]
            return Fragment.instantiate(mContext, info.clss.name, info.args)
        }

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            Log.v(TAG, "instantiateItem:" + position)
            val mWeakFragment = mFragmentArray.get(position)
            if (mWeakFragment != null && mWeakFragment.get() != null) {
                //mWeakFragment.clear();
                return mWeakFragment.get()!!
            }

            val mFragment = super.instantiateItem(container, position) as Fragment
            mFragmentArray.put(position, WeakReference(mFragment))
            return mFragment
        }

        override fun destroyItem(container: ViewGroup?, position: Int, `object`: Any) {
            super.destroyItem(container, position, `object`)
            val mWeakFragment = mFragmentArray.get(position)
            mWeakFragment?.clear()
        }

        override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

        override fun onPageSelected(position: Int) {
            mViewPager!!.currentItem = position
            /*if (position == 0) {
                APVApplication apvApplication = APVApplication.getInstance();
                if (apvApplication.hasChanged) {
                    delayUpdate(position);
                    apvApplication.hasChanged = false;
                }
            } else if (position == 2) {
                delayUpdate(position);
            }*/
        }

        override fun onPageScrollStateChanged(state: Int) {}

        override fun getPageTitle(position: Int): CharSequence {
            return mTabs[position].title
        }

        internal fun delayUpdate(position: Int) {
            mHandler.postDelayed({
                val fragment = getItem(position) as RefreshableFragment
                fragment.update()
            }, 500L)
        }
    }

    companion object {

        /**
         * Logging tag.
         */
        private val TAG = "pdfviewpro"

        @JvmField
        val PREF_TAG = "ChooseFileActivity"
        @JvmField
        val PREF_HOME = "Home"
        private val REQUEST_PERMISSION_CODE = 0x001
    }
}
