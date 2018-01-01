package com.artifex.mini

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.view.MenuItem
import android.view.View
import android.view.ViewTreeObserver
import android.widget.AdapterView
import android.widget.ListView
import android.widget.Toolbar
import cn.archko.pdf.R
import com.artifex.mupdf.viewer.OutlineActivity.Item
import com.artifex.mupdfdemo.OutlineAdapter
import java.util.*

class OutlineActivity : FragmentActivity() {

    protected lateinit var adapter: OutlineAdapter

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.outline_list)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val toolbar = findViewById<Toolbar>(R.id.toolbar)
            setActionBar(toolbar)
            actionBar!!.setDisplayHomeAsUpEnabled(true)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            actionBar!!.setDisplayHomeAsUpEnabled(true)
            actionBar!!.setDisplayShowHomeEnabled(true)
        } else {
            setTheme(R.style.AppFullscreen)
        }
        val listView = findViewById<ListView>(R.id.list)

        val bundle = intent.extras
        val currentPage = bundle!!.getInt("POSITION")
        val outline = bundle.getSerializable("OUTLINE") as ArrayList<Item>
        adapter = OutlineAdapter(layoutInflater, outline)
        listView.adapter = adapter
        listView.onItemClickListener = AdapterView.OnItemClickListener { adapterView, view, i, l -> onListItemClick(adapterView as ListView, view, i, l) }
        var found = -1
        for (i in outline.indices) {
            val item = outline[i]
            if (found < 0 && item.page >= currentPage) {
                found = i
            }
        }
        if (found >= 0) {
            val finalFound = found
            listView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    listView.viewTreeObserver.removeGlobalOnLayoutListener(this)
                    listView.setSelection(finalFound)
                }
            })
        }
        setResult(-1)
    }

    protected fun onListItemClick(l: ListView, v: View, position: Int, id: Long) {
        val item = adapter.getItem(position) as Item
        setResult(Activity.RESULT_FIRST_USER + item.page)
        finish()
    }

    @Override
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
        }
        return super.onOptionsItemSelected(item)
    }
}
