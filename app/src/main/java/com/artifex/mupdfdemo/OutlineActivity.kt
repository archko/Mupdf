package com.artifex.mupdfdemo

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewTreeObserver
import android.widget.AdapterView
import android.widget.ListView
import android.widget.Toolbar

import cn.archko.pdf.R

class OutlineActivity() : Activity() {

    var mItems: Array<OutlineItem>? = null

    /** Called when the activity is first created.  */
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.outline_list)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val toolbar = findViewById(R.id.toolbar) as Toolbar
            setActionBar(toolbar)
            actionBar!!.setDisplayHomeAsUpEnabled(true)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            actionBar!!.setDisplayHomeAsUpEnabled(true)
            actionBar!!.setDisplayShowHomeEnabled(true)
        } else {
            setTheme(R.style.AppFullscreen)
        }

        val listView = findViewById(R.id.list) as ListView
        listView.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id -> onListItemClick(parent as ListView, view, position, id) }

        mItems = OutlineActivityData.get().items
        listView.adapter = OutlineAdapter(layoutInflater, mItems)

        setResult(-1)
        listView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                listView.viewTreeObserver.removeGlobalOnLayoutListener(this)
                setSelection(listView)
            }
        })
    }

    private fun setSelection(listView: ListView) {
        var pos = OutlineActivityData.get().position
        if (null != intent) {
            val cp = intent.getIntExtra("cp", -1)
            if (cp != -1) {
                pos = cp
            }
        }
        val items = OutlineActivityData.get().items
        var item: OutlineItem
        var idx = 0
        for (i in items.indices.reversed()) {
            item = items[i]
            if (item.page <= pos) {
                idx = i
                break
            }
        }

        if (idx < listView.adapter.count) {
            listView.setSelection(idx)
        }
    }

    protected fun onListItemClick(l: ListView, v: View, position: Int, id: Long) {
        OutlineActivityData.get().position = l.firstVisiblePosition
        setResult(mItems!![position].page)
        finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        if (itemId == android.R.id.home) {
            finish()
        }

        return super.onOptionsItemSelected(item)
    }
}
