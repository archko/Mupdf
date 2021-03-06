package cn.archko.pdf.fragments

import android.os.Build
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import cn.archko.pdf.R
import cn.archko.pdf.entity.AKProgress
import cn.archko.pdf.listeners.DataListener
import cn.archko.pdf.common.RecentManager
import cn.archko.pdf.utils.DateUtil
import cn.archko.pdf.utils.FileUtils
import cn.archko.pdf.utils.Util
import com.artifex.mupdf.fitz.Document
import com.artifex.mupdf.fitz.Matrix
import com.artifex.mupdf.fitz.android.AndroidDrawDevice
import cx.hell.android.pdfviewpro.APVApplication
import cx.hell.android.pdfviewpro.FileListEntry
import java.math.BigDecimal

/**
 * @author: archko 2016/1/16 :14:34
 */
class FileInfoFragment : DialogFragment() {

    var mEntry: FileListEntry? = null
    lateinit var mLocation: TextView
    lateinit var mFileName: TextView
    lateinit var mFileSize: TextView
    lateinit var mLastModified: TextView
    lateinit var mLastReadLayout: View
    lateinit var mLastRead: TextView
    lateinit var mPageCount: TextView
    lateinit var mProgressBar: ProgressBar
    lateinit var mIcon: ImageView
    var progress: AKProgress? = null
    var pageCount = 0;
    lateinit var mDataListener: DataListener

    public fun setListener(dataListener: DataListener) {
        mDataListener = dataListener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var themeId = android.R.style.Theme_Holo_Light_Dialog
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            themeId = android.R.style.Theme_Material_Light_Dialog;
        }
        setStyle(DialogFragment.STYLE_NORMAL, themeId)
    }

    override fun setArguments(args: Bundle?) {
        super.setArguments(args)
        if (null != args) {
            mEntry = args.getSerializable(FILE_LIST_ENTRY) as FileListEntry
            progress = mEntry!!.akProgress
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.file_info, container, false)
        mLocation = view.findViewById<TextView>(R.id.location)
        mFileName = view.findViewById<TextView>(R.id.fileName)
        mFileSize = view.findViewById<TextView>(R.id.fileSize)
        mLastReadLayout = view.findViewById(R.id.lay_last_read)
        mLastRead = view.findViewById<TextView>(R.id.lastRead)
        mProgressBar = view.findViewById<ProgressBar>(R.id.progressbar)
        mLastModified = view.findViewById<TextView>(R.id.lastModified)
        mPageCount = view.findViewById<TextView>(R.id.pageCount)
        mIcon = view.findViewById<ImageView>(R.id.icon)
        var button = view.findViewById<Button>(R.id.btn_cancel)
        button.setOnClickListener { this@FileInfoFragment.dismiss() }
        button = view.findViewById<Button>(R.id.btn_ok)
        button.setOnClickListener {
            read()
        }

        dialog.setTitle(R.string.menu_info)

        return view
    }

    private fun read() {
        this@FileInfoFragment.dismiss()
        mDataListener?.onSuccess(mEntry)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (null == mEntry || mEntry!!.file == null) {
            Toast.makeText(activity, "file is null.", Toast.LENGTH_LONG).show()
            dismiss()
            return
        }

        val file = mEntry!!.file
        mLocation.text = file.path
        mFileName.text = file.name
        mFileSize.text = Util.getFileSize(mEntry!!.fileSize)

        if (null == progress) {
            val recentManager = RecentManager(APVApplication.getInstance())
            try {
                recentManager.open()
                progress = recentManager.getProgress(FileUtils.getRealPath(file.absolutePath))
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                recentManager.close()
            }
        }
        if (null != progress) {
            mLastReadLayout.visibility = View.VISIBLE
            showProgress(progress!!)
        } else {
            mLastReadLayout.visibility = View.GONE
        }
        mLastModified.text = DateUtil.formatTime(file.lastModified(), DateUtil.TIME_FORMAT_TWO)

        showIcon(file.path)
    }

    private fun showIcon(path: String) {
        try {
            val core: Document? = Document.openDocument(path)
            pageCount = core!!.countPages()
            setPage()

            val page = core.loadPage(0)
            val ctm: Matrix = AndroidDrawDevice.fitPageWidth(page, activity!!.windowManager.defaultDisplay.width * 2 / 5)
            val bitmap = AndroidDrawDevice.drawPage(page, ctm)
            mIcon.setImageBitmap(bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setPage() {
        if (null != progress) {
            mPageCount.setText(progress!!.page.toString() + "/" + pageCount.toString())
        } else {
            mPageCount.setText(pageCount.toString())
        }
    }

    private fun showProgress(progress: AKProgress) {
        var text = DateUtil.formatTime(progress.timestampe, DateUtil.TIME_FORMAT_TWO)
        val percent = progress.page * 100f / progress.numberOfPages
        val b = BigDecimal(percent.toDouble())
        text += "       " + b.setScale(2, BigDecimal.ROUND_HALF_UP).toFloat() + "%"
        mLastRead.text = text
        mProgressBar.max = progress.numberOfPages
        mProgressBar.progress = progress.page
        setPage()
    }

    companion object {

        val FILE_LIST_ENTRY = "FILE_LIST_ENTRY"
    }
}
