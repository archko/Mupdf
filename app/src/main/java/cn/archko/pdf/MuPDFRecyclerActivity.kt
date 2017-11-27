package cn.archko.pdf

import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.FragmentActivity
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.util.Log
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast

import com.artifex.mupdfdemo.MuPDFCore
import com.artifex.mupdfdemo.MuPDFReflowRecyclerViewAdapter
import com.artifex.mupdfdemo.OutlineActivity
import com.artifex.mupdfdemo.OutlineActivityData

import org.vudroid.core.events.CurrentPageListener
import org.vudroid.core.models.CurrentPageModel
import org.vudroid.core.views.PageSeekBarControls

import cx.hell.android.pdfviewpro.Bookmark
import cx.hell.android.pdfviewpro.BookmarkEntry
import cx.hell.android.pdfviewpro.Options

/**
 * @author: archko 2016/5/9 :12:43
 */
class MuPDFRecyclerActivity : FragmentActivity(), SensorEventListener {
    private var mPath: String? = null
    private var pos = 0

    internal var width = 720
    lateinit var mRecyclerView: RecyclerView
    private var gestureDetector: GestureDetector? = null
    private var bookmarkToRestore: BookmarkEntry? = null
    private var pageNumberToast: Toast? = null

    private var core: MuPDFCore? = null
    private var mPageModel: CurrentPageModel? = null
    lateinit var mPageSeekBarControls: PageSeekBarControls
    private var mButtonsView: View? = null
    private var mReflowButton: ImageButton? = null
    private var mOutlineButton: ImageButton? = null
    private var mTitle: TextView? = null
    private var mReflow = false
    private val OUTLINE_REQUEST = 0

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        width = windowManager.defaultDisplay.width
        initView()

        if (null != savedInstanceState) {
            mPath = savedInstanceState.getString("path", null)
            pos = savedInstanceState.getInt("pos", 0)
        }

        parseIntent()

        println("path:" + mPath!!)

        setStartBookmark()

        try {
            core = MuPDFCore(this, mPath)

            restoreBookmark()

            if (pos > 0) {
                mRecyclerView.scrollToPosition(pos)
            }
            mTitle!!.text = mPath
            if (core!!.hasOutline()) {
                mOutlineButton!!.setOnClickListener {
                    val outline = core!!.outline
                    if (outline != null) {
                        OutlineActivityData.get().items = outline
                        val intent = Intent(this@MuPDFRecyclerActivity, OutlineActivity::class.java)
                        intent.putExtra("cp", pos)
                        startActivityForResult(intent, OUTLINE_REQUEST)
                    }
                }
            } else {
                mOutlineButton!!.visibility = View.GONE
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun parseIntent() {
        if (TextUtils.isEmpty(mPath)) {
            val intent = intent

            if (Intent.ACTION_VIEW == intent.action) {
                var uri = intent.data
                println("URI to open is: " + uri)
                if (uri.toString().startsWith("content://")) {
                    var reason: String? = null
                    var cursor: Cursor? = null
                    try {
                        cursor = contentResolver.query(uri, arrayOf("_data"), null, null, null)
                        if (cursor!!.moveToFirst()) {
                            val str = cursor.getString(0)
                            if (str == null) {
                                reason = "Couldn't parse data in intent"
                            } else {
                                uri = Uri.parse(str)
                            }
                        }
                    } catch (e2: Exception) {
                        println("Exception in Transformer Prime file manager code: " + e2)
                        reason = e2.toString()
                    } finally {
                        if (null != cursor) {
                            cursor.close()
                        }
                    }
                }
                var path: String? = Uri.decode(uri.encodedPath)
                if (path == null) {
                    path = uri.toString()
                }
                mPath = path
            } else {
                if (!TextUtils.isEmpty(getIntent().getStringExtra("path"))) {
                    mPath = getIntent().getStringExtra("path")
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("path", mPath)
        outState.putInt("pos", pos)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(HistoryFragment.ACTION_STOPPED))
        mRecyclerView.adapter = null
        if (null != core) {
            core!!.onDestroy()
        }
    }

    private fun initView() {
        mRecyclerView = RecyclerView(this)
        mRecyclerView.layoutManager = LinearLayoutManager(this)
        mRecyclerView.setHasFixedSize(true)
        mRecyclerView.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT)
        mRecyclerView.adapter = BaseRecyclerAdapter()
        mRecyclerView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL_LIST))

        mRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView?, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    mRecyclerView.adapter.notifyDataSetChanged()
                }
            }

            override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
            }
        })

        val layout = RelativeLayout(this)
        layout.addView(mRecyclerView)

        setContentView(layout)

        initTouchParams()
        mRecyclerView.setOnTouchListener { v, event -> gestureDetector!!.onTouchEvent(event) }

        mPageModel = CurrentPageModel()
        mPageModel!!.addEventListener(CurrentPageListener { pageIndex ->
            Log.d(TAG, "currentPageChanged:" + pageIndex)
            if (pos != pageIndex) {
                mRecyclerView.scrollToPosition(pageIndex)
            }
        })
        mPageSeekBarControls = createSeekControls(mPageModel!!)

        var lp = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)

        layout.addView(mPageSeekBarControls, lp)
        mPageSeekBarControls.hide()

        makeButtonsView()
        lp = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.addRule(RelativeLayout.ALIGN_PARENT_TOP)
        layout.addView(mButtonsView, lp)
        mButtonsView!!.visibility = View.GONE
    }

    private fun makeButtonsView() {
        mButtonsView = layoutInflater.inflate(R.layout.view_buttons, null)
        mReflowButton = mButtonsView!!.findViewById<ImageButton>(R.id.reflowButton) as ImageButton
        mOutlineButton = mButtonsView!!.findViewById<ImageButton>(R.id.outlineButton) as ImageButton
        mTitle = mButtonsView!!.findViewById<TextView>(R.id.title) as TextView

        mReflowButton!!.setOnClickListener { toggleReflow() }
    }

    private fun toggleReflow() {
        reflowModeSet(!mReflow)
        Toast.makeText(this, if (mReflow) getString(R.string.entering_reflow_mode) else getString(R.string.leaving_reflow_mode), Toast.LENGTH_SHORT).show()
    }

    private fun reflowModeSet(reflow: Boolean) {
        mReflow = reflow
        mRecyclerView.adapter = if (mReflow) MuPDFReflowRecyclerViewAdapter(this, core) else BaseRecyclerAdapter()
        mReflowButton!!.setColorFilter(if (mReflow) Color.argb(0xFF, 172, 114, 37) else Color.argb(0xFF, 255, 255, 255))
        if (pos > 0) {
            mRecyclerView.scrollToPosition(pos)
        }
    }

    internal fun initTouchParams() {
        var margin = mRecyclerView.height
        if (margin <= 0) {
            margin = ViewConfiguration.get(this).scaledTouchSlop * 2
        } else {
            margin = (margin * 0.03).toInt()
        }
        gestureDetector = GestureDetector(this, object : GestureDetector.OnGestureListener {

            override fun onDown(e: MotionEvent): Boolean {
                return false
            }

            override fun onShowPress(e: MotionEvent) {

            }

            override fun onSingleTapUp(e: MotionEvent): Boolean {
                return false
            }

            override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
                return false
            }

            override fun onLongPress(e: MotionEvent) {

            }

            override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                return false
            }
        })

        val finalMargin = margin
        gestureDetector!!.setOnDoubleTapListener(object : GestureDetector.OnDoubleTapListener {

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                val top = mRecyclerView.height / 4
                val bottom = mRecyclerView.height * 3 / 4

                if (e.y.toInt() < top) {
                    var scrollY = mRecyclerView.scrollY
                    scrollY -= mRecyclerView.height
                    mRecyclerView.scrollBy(0, scrollY + finalMargin)
                    return true
                } else if (e.y.toInt() > bottom) {
                    var scrollY = mRecyclerView.scrollY
                    scrollY += mRecyclerView.height
                    mRecyclerView.scrollBy(0, scrollY - finalMargin)
                    return true
                } else {
                    val pageText = (pos + 1).toString() + "/" + core!!.countPages()
                    if (pageNumberToast != null) {
                        pageNumberToast!!.setText(pageText)
                    } else {
                        pageNumberToast = Toast.makeText(this@MuPDFRecyclerActivity, pageText, Toast.LENGTH_SHORT)
                    }
                    pageNumberToast!!.setGravity(Gravity.BOTTOM or Gravity.START, 0, 0)
                    pageNumberToast!!.show()
                }
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                mPageModel!!.setCurrentPage(pos)
                mPageModel!!.pageCount = core!!.countPages()
                mPageModel!!.toggleSeekControls()
                if (mButtonsView!!.visibility == View.GONE) {
                    mButtonsView!!.visibility = View.VISIBLE
                } else {
                    mButtonsView!!.visibility = View.GONE
                }
                return true
            }

            override fun onDoubleTapEvent(e: MotionEvent): Boolean {
                return false
            }
        })
    }

    private fun createSeekControls(pageModel: CurrentPageModel): PageSeekBarControls {
        val controls = PageSeekBarControls(this, pageModel)
        controls.gravity = Gravity.CENTER_HORIZONTAL or Gravity.TOP
        pageModel.addEventListener(controls)
        return controls
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        when (requestCode) {
            OUTLINE_REQUEST -> {
                if (resultCode >= 0) {
                    pos = resultCode
                }
                mRecyclerView.layoutManager.scrollToPosition(resultCode)
            }
        }

        super.onActivityResult(requestCode, resultCode, data)
    }
    //--------------------------------------

    fun setStartBookmark() {
        val progress = AKRecent.getInstance(applicationContext).readRecentFromDb(mPath)
        if (null != progress) {
            val entry = BookmarkEntry(progress.bookmarkEntry)
            bookmarkToRestore = entry
        }
    }

    internal fun restoreBookmark() {
        if (bookmarkToRestore == null || core == null || core!!.countPages() <= 0) {
            return
        }

        if (bookmarkToRestore!!.numberOfPages != core!!.countPages() || bookmarkToRestore!!.page > core!!.countPages()) {
            bookmarkToRestore = null
            return
        }

        if (0 < bookmarkToRestore!!.page) {
            pos = bookmarkToRestore!!.page
        }
    }

    private fun saveCurrentPage() {
        val filePath = Uri.decode(mPath)
        val entry = toBookmarkEntry()
        val b = Bookmark(applicationContext).open()
        b.setLast(filePath, entry)
        b.close()
        Log.i(TAG, "last page saved for $filePath entry:$entry")
        AKRecent.getInstance(applicationContext).addAsyncToDB(filePath, entry.page, entry.numberOfPages, entry.toString(),
                object : DataListener {
                    override fun onSuccess(vararg args: Any) {
                        //AKRecent.getInstance(getApplicationContext()).backup("mupdf_recent.jso");
                    }

                    override fun onFailed(vararg args: Any) {

                    }
                })
    }

    fun toBookmarkEntry(): BookmarkEntry {
        if (null != bookmarkToRestore) {
            bookmarkToRestore!!.page = pos
            bookmarkToRestore!!.numberOfPages = core!!.countPages()
            return bookmarkToRestore!!
        }
        return BookmarkEntry(core!!.countPages(),
                pos, (1 * 1000).toFloat(), 0,
                0, 0)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    private var sensorManager: SensorManager? = null
    private val gravity = floatArrayOf(0f, -9.81f, 0f)
    private var gravityAge: Long = 0

    private var prevOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

    override fun onResume() {
        super.onResume()

        sensorManager = null

        val options = PreferenceManager.getDefaultSharedPreferences(this)

        if (Options.setOrientation(this)) {
            sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
            if (sensorManager!!.getSensorList(Sensor.TYPE_ACCELEROMETER).size > 0) {
                gravity[0] = 0f
                gravity[1] = -9.81f
                gravity[2] = 0f
                gravityAge = 0
                sensorManager!!.registerListener(this, sensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                        SensorManager.SENSOR_DELAY_NORMAL)
                prevOrientation = options.getInt(Options.PREF_PREV_ORIENTATION, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
                requestedOrientation = prevOrientation
            } else {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }

        if (options.getBoolean(Options.PREF_KEEP_ON, false)) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        //documentView.setVerticalScrollLock(options.getBoolean(Options.PREF_VERTICAL_SCROLL_LOCK, true));
        if (options.getBoolean(Options.PREF_FULLSCREEN, true)) {
            //getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE
        }
    }

    override fun onPause() {
        super.onPause()

        saveCurrentPage()

        if (sensorManager != null) {
            sensorManager!!.unregisterListener(this)
            sensorManager = null
            val edit = PreferenceManager.getDefaultSharedPreferences(this).edit()
            edit.putInt(Options.PREF_PREV_ORIENTATION, prevOrientation)
            //Log.v(TAG, "prevOrientation saved: "+prevOrientation);
            edit.apply()
        }
    }

    private fun setOrientation(orientation: Int) {
        if (orientation != prevOrientation) {
            //Log.v(TAG, "setOrientation: "+orientation);
            requestedOrientation = orientation
            prevOrientation = orientation
        }
    }

    /**
     * Called when accuracy changes.
     * This method is empty, but it's required by relevant interface.
     */
    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent) {
        gravity[0] = 0.8f * gravity[0] + 0.2f * event.values[0]
        gravity[1] = 0.8f * gravity[1] + 0.2f * event.values[1]
        gravity[2] = 0.8f * gravity[2] + 0.2f * event.values[2]

        val sq0 = gravity[0] * gravity[0]
        val sq1 = gravity[1] * gravity[1]
        val sq2 = gravity[2] * gravity[2]

        gravityAge++

        if (gravityAge < 4) {
            // ignore initial hiccups
            return
        }

        if (sq1 > 3 * (sq0 + sq2)) {
            if (gravity[1] > 4)
                setOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
            else if (gravity[1] < -4 && Integer.parseInt(Build.VERSION.SDK) >= 9)
                setOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT)
        } else if (sq0 > 3 * (sq1 + sq2)) {
            if (gravity[0] > 4)
                setOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
            else if (gravity[0] < -4 && Integer.parseInt(Build.VERSION.SDK) >= 9)
                setOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE)
        }
    }

    //===========================================

    private inner class BaseRecyclerAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            var holder: PdfHolder? = null
            val view = ImageView(baseContext)
            var lp: ViewGroup.LayoutParams? = view.layoutParams
            if (null == lp) {
                lp = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                view.layoutParams = lp
            }
            holder = PdfHolder(view)
            return holder
        }

        override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
            pos = viewHolder.adapterPosition
            val pdfHolder = viewHolder as PdfHolder

            if (pdfHolder.mBitmap != null && !pdfHolder.mBitmap!!.isRecycled) {
                updateBitmap(pdfHolder, pdfHolder.mBitmap!!)
                return
            } else {
                if (pdfHolder.mThumbnail != null && !pdfHolder.mThumbnail!!.isRecycled) {
                    updateBitmap(pdfHolder, pdfHolder.mThumbnail!!)
                }
            }

            if (mRecyclerView.scrollState == RecyclerView.SCROLL_STATE_IDLE) {
                val bm = renderBitmap(position, 1)
                pdfHolder.mBitmap = bm
                updateBitmap(pdfHolder, pdfHolder.mBitmap!!)
            } else {
                val bm = renderBitmap(position, 8)
                pdfHolder.mThumbnail = bm
                updateBitmap(pdfHolder, pdfHolder.mThumbnail!!)
            }
            Log.d(TAG, "onBindViewHolder:" + pos)
        }

        private fun renderBitmap(position: Int, scale: Int): Bitmap {
            val result = core!!.getPageSize(position)
            val width = result.x.toInt() / scale
            val height = result.y.toInt() / scale
            val bm = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            core!!.drawPage(bm, position, width, height, 0, 0, width, height, core!!.Cookie())
            return bm
        }

        private fun updateBitmap(pdfHolder: PdfHolder, bitmap: Bitmap) {
            pdfHolder.imageView.setImageBitmap(bitmap)
            pdfHolder.imageView.adjustViewBounds = true
        }

        override fun onViewRecycled(holder: RecyclerView.ViewHolder?) {
            super.onViewRecycled(holder)
            val pdfHolder = holder as PdfHolder?
            pdfHolder!!.imageView.setImageBitmap(null)
            if (null != pdfHolder && null != pdfHolder.mThumbnail && !pdfHolder.mThumbnail!!.isRecycled) {
                pdfHolder.mThumbnail!!.recycle()
            }
            if (null != pdfHolder && null != pdfHolder.mBitmap && !pdfHolder.mBitmap!!.isRecycled) {
                pdfHolder.mBitmap!!.recycle()
            }
        }

        override fun getItemCount(): Int {
            return core!!.countPages()
        }

        inner class PdfHolder(internal var imageView: ImageView) : RecyclerView.ViewHolder(imageView) {
            internal var mThumbnail: Bitmap? = null
            internal var mBitmap: Bitmap? = null
        }

    }

    internal inner class PDFRecyclerAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            var holder: PdfHolder? = null
            val view = PDFView(baseContext, core)
            holder = PdfHolder(view)
            return holder
        }

        override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
            pos = viewHolder.adapterPosition
            val pdfHolder = viewHolder as PdfHolder

            val result = core!!.getPageSize(position)
            pdfHolder.mPdfView.setPage(position, result)
        }

        override fun onViewRecycled(holder: RecyclerView.ViewHolder?) {
            super.onViewRecycled(holder)
            val pdfHolder = holder as PdfHolder?
            pdfHolder!!.mPdfView.releaseBitmaps()
        }

        override fun getItemCount(): Int {
            return core!!.countPages()
        }

        inner class PdfHolder(internal var mPdfView: PDFView) : RecyclerView.ViewHolder(mPdfView)

    }

    companion object {

        private val TAG = "MuPDFRecyclerActivity"
    }
}
