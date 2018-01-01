package com.artifex.mupdfdemo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import cn.archko.pdf.R
import com.artifex.mupdf.viewer.OutlineActivity
import com.artifex.mupdf.viewer.OutlineActivity.Item
import java.util.*

class OutlineAdapter(private val mInflater: LayoutInflater, items: ArrayList<OutlineActivity.Item>) : BaseAdapter() {

    private val mItems: ArrayList<Item>

    init {
        mItems = items
    }

    override fun getCount(): Int {
        return mItems.size
    }

    override fun getItem(pos: Int): Any {
        return mItems[pos]
    }

    override fun getItemId(pos: Int): Long {
        return 0
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val v: View
        if (convertView == null) {
            v = mInflater.inflate(R.layout.outline_entry, null)
        } else {
            v = convertView
        }

        (v.findViewById<View>(R.id.title) as TextView).text = mItems[position].title
        (v.findViewById<View>(R.id.page) as TextView).text = (mItems[position].page + 1).toString()
        return v
    }

}
