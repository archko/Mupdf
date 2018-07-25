package cn.archko.pdf

import android.annotation.TargetApi
import android.content.Intent
import android.content.res.Configuration
import android.database.Cursor
import android.graphics.Color
import android.graphics.Point
import android.graphics.PointF
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.FragmentActivity
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.util.Log
import android.util.SparseArray
import android.view.*
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import cn.archko.pdf.utils.Util
import com.artifex.mupdf.fitz.Document
import com.artifex.mupdf.fitz.Outline
import com.artifex.mupdfdemo.MuPDFReflowRecyclerViewAdapter
import cx.hell.android.pdfviewpro.Options
import org.jetbrains.anko.toast
import org.vudroid.core.events.CurrentPageListener
import org.vudroid.core.models.CurrentPageModel
import org.vudroid.core.views.PageSeekBarControls
import java.util.*

/**
 * @author: archko 2016/5/9 :12:43
 */
class MuPDFRecyclerActivity : FragmentActivity() {
    private var mPath: String? = null
    private var pos = 0

    lateinit var mRecyclerView: RecyclerView
    private var gestureDetector: GestureDetector? = null
    private var pageNumberToast: Toast? = null

    private var mPageModel: CurrentPageModel? = null
    lateinit var mPageSeekBarControls: PageSeekBarControls
    private var mButtonsView: View? = null
    private var mReflowButton: ImageButton? = null
    private var mOutlineButton: ImageButton? = null
    private var mTitle: TextView? = null
    private var mReflow = false
    private val OUTLINE_REQUEST = 0
    private var pdfBookmarkManager: PDFBookmarkManager? = null
    private var sensorHelper: SensorHelper? = null
    private var mCore: Document? = null
    private var outline: Array<Outline>? = null
    private var items: ArrayList<com.artifex.mupdf.viewer.OutlineActivity.Item>? = null
    private val mPageSizes = SparseArray<PointF>()

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initView()

        if (null != savedInstanceState) {
            mPath = savedInstanceState.getString("path", null)
            pos = savedInstanceState.getInt("pos", 0)
        }

        parseIntent()

        println("path:" + mPath!!)

        if (TextUtils.isEmpty(mPath)) {
            toast("error file path:$mPath")
            return
        }

        pdfBookmarkManager = PDFBookmarkManager()
        pdfBookmarkManager?.setStartBookmark(mPath)
        sensorHelper = SensorHelper(this)

        loadDoc()
    }

    private fun doLoadDoc() {
        try {
            pos = pdfBookmarkManager?.restoreBookmark(mCore!!.countPages())!!

            mRecyclerView.adapter = BaseRecyclerAdapter()
            if (pos > 0) {
                mRecyclerView.scrollToPosition(pos)
            }
            mTitle!!.text = mPath
            if (hasOutline()) {
                mOutlineButton!!.setOnClickListener {
                    openOutline(pos)
                }
            } else {
                mOutlineButton!!.visibility = View.GONE
            }
        } catch (e: Exception) {
            e.printStackTrace()
            finish()
        }
    }

    fun openOutline(pos: Int) {
        if (hasOutline() && null != outline) {
            val intent = Intent(this, com.artifex.mini.OutlineActivity::class.java)
            intent.putExtra("cp", pos)
            intent.putExtra("POSITION", pos);
            intent.putExtra("OUTLINE", getOutline());
            startActivityForResult(intent, OUTLINE_REQUEST)
        }
    }

    fun getOutline(): ArrayList<com.artifex.mupdf.viewer.OutlineActivity.Item> {
        if (null != items) {
            return items!!
        } else {
            items = ArrayList<com.artifex.mupdf.viewer.OutlineActivity.Item>()
            flattenOutlineNodes(items!!, outline, "")
        }
        return items!!
    }

    private fun flattenOutlineNodes(result: ArrayList<com.artifex.mupdf.viewer.OutlineActivity.Item>, list: Array<Outline>?, indent: String) {
        for (node in list!!) {
            if (node.title != null) {
                result.add(com.artifex.mupdf.viewer.OutlineActivity.Item(indent + node.title, node.page))
            }
            if (node.down != null) {
                flattenOutlineNodes(result, node.down, "$indent    ")
            }
        }
    }

    fun hasOutline(): Boolean {
        if (outline == null) {
            outline = mCore?.loadOutline()
        }
        return outline != null
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
        if (null != mCore) {
            mCore!!.destroy()
        }
    }

    private fun initView() {
        mRecyclerView = RecyclerView(this)
        mRecyclerView.layoutManager = LinearLayoutManager(this)
        mRecyclerView.setHasFixedSize(true)
        mRecyclerView.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT)

        mRecyclerView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL_LIST))

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
        mRecyclerView.adapter = if (mReflow) MuPDFReflowRecyclerViewAdapter(this, mCore) else BaseRecyclerAdapter()
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
                    val pageText = (pos + 1).toString() + "/" + mCore!!.countPages()
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
                mPageModel!!.pageCount = mCore!!.countPages()
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            OUTLINE_REQUEST -> {
                if (resultCode >= 0) {
                    pos = resultCode - RESULT_FIRST_USER
                }
                mRecyclerView.layoutManager.scrollToPosition(resultCode)
            }
        }

        super.onActivityResult(requestCode, resultCode, data)
    }
    //--------------------------------------

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    override fun onResume() {
        super.onResume()

        sensorHelper?.onResume()
        val options = PreferenceManager.getDefaultSharedPreferences(this)

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

        pdfBookmarkManager?.saveCurrentPage(mPath, mCore!!.countPages(), pos, 1000f, 0, 0)

        sensorHelper?.onPause()
    }

    //===========================================

    private inner class BaseRecyclerAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            var holder: PdfHolder? = null
            val view = APDFView(parent.context, mCore, Point(parent.width, parent.height))
            var lp: ViewGroup.LayoutParams? = view.layoutParams
            var size: PointF? = null
            var width: Int = ViewGroup.LayoutParams.MATCH_PARENT
            var height: Int = ViewGroup.LayoutParams.MATCH_PARENT
            if (mPageSizes.size() > 0) {
                size = mPageSizes.get(0)
                val xr = parent.width / size.x
                val yr = parent.height / size.y
                var mSourceScale = Math.max(xr, yr)
                val newSize = Point((size.x * mSourceScale).toInt(), (size.y * mSourceScale).toInt())
                width = newSize.x
                height = newSize.y
            }
            if (null == lp) {
                lp = RecyclerView.LayoutParams(width, height)
                view.layoutParams = lp
            } else {
                lp.width = width;
                lp.height = height;
            }
            holder = PdfHolder(view)
            return holder
        }

        override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
            pos = viewHolder.adapterPosition
            val pdfHolder = viewHolder as PdfHolder

            pdfHolder.onBind(position)
        }

        override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
            super.onViewRecycled(holder)
            val pdfHolder = holder as PdfHolder?

            pdfHolder?.view?.releaseResources()
        }

        override fun getItemCount(): Int {
            return mCore!!.countPages()
        }

        inner class PdfHolder(internal var view: APDFView) : RecyclerView.ViewHolder(view) {
            fun onBind(position: Int) {
                val pageSize = mPageSizes.get(position)
                view.setPage(position, pageSize)

                println("onBindViewHolder:$pos, view:${view}")
            }
        }

    }

    companion object {

        private val TAG = "MuPDFRecyclerActivity"
    }

    private fun loadDoc() {
        val sizingTask = object : AsyncTask<Void, Void, Boolean>() {
            override fun doInBackground(vararg arg0: Void): Boolean {
                try {
                    mCore = Document.openDocument(mPath)

                    var cp = mCore!!.countPages();
                    for (i in 0 until cp) {
                        val pointF = getPageSize(i)
                        mPageSizes.put(i, pointF)
                    }
                    return true
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return false
            }

            override fun onPostExecute(result: Boolean) {
                super.onPostExecute(result)
                if (result) {
                    doLoadDoc()
                } else {
                    finish()
                }
            }
        }

        toast("loading:$mPath")
        Util.execute(false, sizingTask)
        //sizingTask.execute(null as Void?)
    }

    fun getPageSize(pageNum: Int): PointF {
        val p = mCore?.loadPage(pageNum)
        val b = p!!.getBounds()
        val w = b.x1 - b.x0
        val h = b.y1 - b.y0
        return PointF(w, h)
    }
}
