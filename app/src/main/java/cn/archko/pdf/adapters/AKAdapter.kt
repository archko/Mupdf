package cn.archko.pdf.adapters

import android.content.Context
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import cn.archko.pdf.R
import cn.archko.pdf.utils.Util
import cx.hell.android.pdfviewpro.FileListEntry
import java.util.*

/**
 * @author: archko 2014/4/17 :15:43
 */
class AKAdapter(internal var mContext: Context?) : BaseAdapter() {

    internal var mData: ArrayList<FileListEntry>
    internal var mMode = TYPE_FILE

    fun setMode(mMode: Int) {
        this.mMode = mMode
    }

    fun setData(mData: ArrayList<FileListEntry>) {
        this.mData = mData
    }

    init {
        this.mData = ArrayList<FileListEntry>()
    }

    override fun getCount(): Int {
        return mData.size
    }

    override fun getItem(position: Int): Any {
        return mData[position]
    }

    override fun getItemId(position: Int): Long {
        return 0
    }

    override fun getItemViewType(position: Int): Int {
        return mMode
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView
        var viewHolder: ViewHolder? = null

        val type = getItemViewType(position)
        if (convertView == null) {
            viewHolder = ViewHolder()
            when (type) {
                TYPE_FILE -> {
                    convertView = View.inflate(mContext, R.layout.picker_entry, null)
                    viewHolder.mProgressBar = convertView!!.findViewById<ProgressBar>(R.id.progressbar)
                }
                TYPE_RENCENT -> {
                    convertView = View.inflate(mContext, R.layout.picker_entry, null)
                    viewHolder.mProgressBar = convertView!!.findViewById<ProgressBar>(R.id.progressbar)
                }
                TYPE_SEARCH -> {
                    convertView = View.inflate(mContext, R.layout.picker_entry_search, null)
                    viewHolder.mPath = convertView!!.findViewById<TextView>(R.id.fullpath)
                }
            }

            viewHolder.mIcon = convertView!!.findViewById<ImageView>(R.id.icon)
            viewHolder.mName = convertView.findViewById<TextView>(R.id.name)
            viewHolder.mSize = convertView.findViewById<TextView>(R.id.size)
            convertView.tag = viewHolder
        } else {
            viewHolder = convertView.tag as ViewHolder
        }

        val entry = mData[position]


        if (type == TYPE_RENCENT || TYPE_FILE == type) {
            val progress = entry.akProgress
            if (null != progress) {
                viewHolder.mProgressBar!!.visibility = View.VISIBLE
                viewHolder.mProgressBar!!.max = progress.numberOfPages
                viewHolder.mProgressBar!!.progress = progress.page
            } else {
                viewHolder.mProgressBar!!.visibility = View.INVISIBLE
            }
        } else if (type == TYPE_SEARCH) {
            viewHolder.mPath!!.text = entry.file.absolutePath
        }

        if (entry.type == FileListEntry.HOME) {
            viewHolder.mIcon!!.setImageResource(R.drawable.ic_explorer_fldr)
        } else if (entry.type == FileListEntry.NORMAL && entry.isDirectory && !entry.isUpFolder) {
            viewHolder.mIcon!!.setImageResource(R.drawable.ic_explorer_fldr)
        } else if (entry.isUpFolder) {
            viewHolder.mIcon!!.setImageResource(R.drawable.ic_explorer_fldr)
        } else {
            viewHolder.mIcon!!.setImageResource(R.drawable.browser_item_book)
        }

        viewHolder.mName!!.text = entry.label
        viewHolder.mName!!.setTypeface(
                viewHolder.mName!!.typeface,
                if (entry.type == FileListEntry.RECENT)
                    Typeface.ITALIC
                else
                    Typeface.NORMAL
        )

        if (null != entry.file) {
            viewHolder.mSize!!.text = Util.getFileSize(entry.fileSize)
        }
        return convertView
    }

    private inner class ViewHolder {

        internal var mName: TextView? = null
        internal var mIcon: ImageView? = null
        internal var mProgressBar: ProgressBar? = null
        internal var mSize: TextView? = null
        internal var mPath: TextView? = null
    }

    companion object {

        @JvmField
        val TYPE_FILE = 0
        @JvmField
        val TYPE_RENCENT = 1
        @JvmField
        val TYPE_SEARCH = 2
    }
}
