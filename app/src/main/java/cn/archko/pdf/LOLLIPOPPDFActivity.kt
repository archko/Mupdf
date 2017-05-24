package cn.archko.pdf

import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.preference.PreferenceManager
import android.support.v4.app.FragmentActivity
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
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast

import org.vudroid.core.events.CurrentPageListener
import org.vudroid.core.models.CurrentPageModel
import org.vudroid.core.views.PageSeekBarControls

import java.io.File
import java.io.IOException

import cx.hell.android.pdfviewpro.Bookmark
import cx.hell.android.pdfviewpro.BookmarkEntry
import cx.hell.android.pdfviewpro.Options

/**
 * @author: archko 2016/5/9 :12:43
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
class LOLLIPOPPDFActivity : FragmentActivity(), SensorEventListener {
    private var mPath: String? = null
    private var mPdfRenderer: PdfRenderer? = null
    private var pos = 0

    internal var width = 720
    lateinit var mRecyclerView: RecyclerView
    private var gestureDetector: GestureDetector? = null
    private var bookmarkToRestore: BookmarkEntry? = null
    private var pageNumberToast: Toast? = null

    private var mPageModel: CurrentPageModel? = null
    lateinit var mPageSeekBarControls: PageSeekBarControls

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
            val file = File(mPath!!)
            val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            mPdfRenderer = PdfRenderer(fd)
            restoreBookmark()

            if (pos > 0 && mPdfRenderer!!.pageCount > pos) {
                //mPdfRenderer.openPage(pos);
                mRecyclerView.scrollToPosition(pos)
            }
        } catch (e: IOException) {
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
        //LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(HistoryFragment.ACTION_STOPPED));
    }

    private fun initView() {
        mRecyclerView = RecyclerView(this)
        mRecyclerView.layoutManager = LinearLayoutManager(this)
        mRecyclerView.setHasFixedSize(true)
        mRecyclerView.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT)
        mRecyclerView.adapter = BaseRecyclerAdapter()
        mRecyclerView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL_LIST))

        mRecyclerView.setBackgroundColor(Color.WHITE)

        val frameLayout = FrameLayout(this)
        frameLayout.addView(mRecyclerView)

        setContentView(frameLayout)

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
        frameLayout.addView(mPageSeekBarControls)
        mPageSeekBarControls.hide()
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
                    val pageText = (pos + 1).toString() + "/" + mPdfRenderer!!.pageCount
                    if (pageNumberToast != null) {
                        pageNumberToast!!.setText(pageText)
                    } else {
                        pageNumberToast = Toast.makeText(this@LOLLIPOPPDFActivity, pageText, Toast.LENGTH_SHORT)
                    }
                    pageNumberToast!!.setGravity(Gravity.BOTTOM or Gravity.LEFT, 0, 0)
                    pageNumberToast!!.show()
                }
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                mPageModel!!.setCurrentPage(pos)
                mPageModel!!.pageCount = mPdfRenderer!!.pageCount
                mPageModel!!.toggleSeekControls()
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

    //--------------------------------------

    fun setStartBookmark() {
        val progress = AKRecent.getInstance(applicationContext).readRecentFromDb(mPath)
        if (null != progress) {
            val entry = BookmarkEntry(progress.bookmarkEntry)
            bookmarkToRestore = entry
        }
    }

    internal fun restoreBookmark() {
        if (bookmarkToRestore == null || mPdfRenderer == null || mPdfRenderer!!.pageCount <= 0) {
            return
        }

        if (bookmarkToRestore!!.numberOfPages != mPdfRenderer!!.pageCount || bookmarkToRestore!!.page > mPdfRenderer!!.pageCount) {
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
            bookmarkToRestore!!.numberOfPages = mPdfRenderer!!.pageCount
            return bookmarkToRestore!!
        }
        return BookmarkEntry(mPdfRenderer!!.pageCount,
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

        //saveCurrentPage();

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

    internal inner class BaseRecyclerAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

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
            val page = mPdfRenderer!!.openPage(position)

            val f = width.toFloat() / page.width
            val bitmap = Bitmap.createBitmap((page.width * f).toInt(), (page.height * f).toInt(), Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            (viewHolder as PdfHolder).imageView.setImageBitmap(bitmap)
            viewHolder.imageView.adjustViewBounds = true
            pos = viewHolder.getAdapterPosition()
        }


        override fun getItemCount(): Int {
            return mPdfRenderer!!.pageCount
        }

        inner class PdfHolder(internal var imageView: ImageView) : RecyclerView.ViewHolder(imageView)

    }

    companion object {

        private val TAG = "LOLLIPOPPDFActivity"
    }
}
