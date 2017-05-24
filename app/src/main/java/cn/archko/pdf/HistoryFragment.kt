package cn.archko.pdf

import android.app.ProgressDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.AsyncTask
import android.os.Bundle
import android.os.Environment
import android.support.v4.content.LocalBroadcastManager
import android.support.v4.view.MenuItemCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.Toast

import cn.archko.pdf.utils.LengthUtils
import cn.archko.pdf.utils.Util
import cx.hell.android.pdfviewpro.APVApplication
import cx.hell.android.pdfviewpro.FileListEntry

import java.io.File
import java.util.ArrayList

/**
 * @version 1.00.00
 * *
 * @description:
 * *
 * @author: archko 11-11-17
 */
class HistoryFragment : BrowserFragment(), AbsListView.OnScrollListener {
    private val showExtension = false

    private var mSavedLastVisibleIndex = -1
    internal var totalCount = 0
    internal var curPage = -1
    internal var totalPage = 0
    internal var mLocalBroadcastManager: LocalBroadcastManager? = null
    internal var mReceiver: BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(activity)

        val filter = IntentFilter()
        filter.addAction(ACTION_STARTED)
        filter.addAction(ACTION_UPDATE)
        filter.addAction(ACTION_STOPPED)
        mReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == ACTION_STARTED) {
                    Log.d(TAG, "STARTED")
                } else if (intent.action == ACTION_UPDATE) {
                    Log.d(TAG, "Got update: " + intent.getIntExtra("value", 0))
                } else if (intent.action == ACTION_STOPPED) {
                    Log.d(TAG, "STOPPED")
                    update()
                }
            }
        }
        mLocalBroadcastManager!!.registerReceiver(mReceiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        mLocalBroadcastManager!!.unregisterReceiver(mReceiver)
    }

    override fun onBackPressed(): Boolean {
        return false
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        this.optionsMenuItem = menu!!.add(R.string.options)
        MenuItemCompat.setShowAsAction(this.optionsMenuItem, MenuItemCompat.SHOW_AS_ACTION_IF_ROOM)
        backMenuItem = menu.add(R.string.options_back)
        MenuItemCompat.setShowAsAction(this.backMenuItem, MenuItemCompat.SHOW_AS_ACTION_IF_ROOM)
        restoreMenuItem = menu.add(R.string.options_restore)
        MenuItemCompat.setShowAsAction(this.restoreMenuItem, MenuItemCompat.SHOW_AS_ACTION_IF_ROOM)
    }

    override fun onOptionsItemSelected(menuItem: MenuItem?): Boolean {
        if (menuItem === this.backMenuItem) {
            backup()
        } else if (menuItem === this.restoreMenuItem) {
            restore()
        }
        return super.onOptionsItemSelected(menuItem)
    }

    private fun backup() {
        val progressDialog = ProgressDialog(activity)
        progressDialog.setTitle("Waiting...")
        progressDialog.setMessage("Waiting...")
        val now = System.currentTimeMillis()
        Util.execute(true, object : AsyncTask<Void, Void, String>() {
            override fun onPreExecute() {
                super.onPreExecute()
                progressDialog.setCancelable(false)
                progressDialog.show()
            }

            override fun doInBackground(vararg params: Void): String {
                val filepath = AKRecent.getInstance(APVApplication.getInstance()).backupFromDb()
                var newTime = System.currentTimeMillis() - now
                if (newTime < 1500L) {
                    newTime = 1500L - newTime
                } else {
                    newTime = 0
                }

                try {
                    Thread.sleep(newTime)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }

                return filepath
            }

            override fun onPostExecute(s: String) {
                progressDialog?.dismiss()

                if (!LengthUtils.isEmpty(s)) {
                    Log.d("", "file:" + s)
                    Toast.makeText(APVApplication.getInstance(), "备份成功:" + s, Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(APVApplication.getInstance(), "备份失败", Toast.LENGTH_LONG).show()
                }
            }
        }, null)
    }

    private fun restore() {
        val progressDialog = ProgressDialog(activity)
        progressDialog.setTitle("Waiting...")
        progressDialog.setMessage("Waiting...")
        val now = System.currentTimeMillis()
        Util.execute(true, object : AsyncTask<Void, Void, Boolean>() {
            override fun onPreExecute() {
                super.onPreExecute()
                progressDialog.setCancelable(false)
                progressDialog.show()
            }

            override fun doInBackground(vararg params: Void): Boolean? {
                var filepath: String? = null
                val filenames = Environment.getExternalStorageDirectory().list() ?: return false

                for (s in filenames) {
                    if (s.startsWith("mupdf_")) {
                        filepath = Environment.getExternalStorageDirectory().toString() + File.separator + s
                        Log.d(TAG, "restore file:" + s)
                    }
                }

                if (null == filepath) {
                    return false
                }
                val flag = AKRecent.getInstance(APVApplication.getInstance()).restoreToDb(filepath)
                var newTime = System.currentTimeMillis() - now
                if (newTime < 1500L) {
                    newTime = 1500L - newTime
                } else {
                    newTime = 0
                }

                try {
                    Thread.sleep(newTime)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }

                return flag
            }

            override fun onPostExecute(s: Boolean?) {
                progressDialog?.dismiss()

                if (s!!) {
                    Toast.makeText(APVApplication.getInstance(), "恢复成功:" + s, Toast.LENGTH_LONG).show()
                    update()
                } else {
                    Toast.makeText(APVApplication.getInstance(), "恢复失败", Toast.LENGTH_LONG).show()
                }
            }
        }, null)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        filesListView!!.divider = null
        filesListView!!.dividerHeight = 0

        this.pathTextView!!.visibility = View.GONE
        filesListView!!.setOnScrollListener(this)

        return view
    }

    private fun reset() {
        curPage = -1
        totalCount = 0
        totalPage = 0
        mSavedLastVisibleIndex = -1
    }

    override fun update() {
        if (null == fileListAdapter) {
            return
        }
        this.fileListAdapter!!.setMode(AKAdapter.TYPE_RENCENT)

        reset()
        getHistory()
    }

    private fun getHistory() {
        Util.execute(true, object : AsyncTask<Void, Void, ArrayList<FileListEntry>>() {
            override fun doInBackground(vararg params: Void): ArrayList<FileListEntry> {
                //final long now=System.currentTimeMillis();
                val recent = AKRecent.getInstance(this@HistoryFragment.activity)
                val count = recent.progressCount
                val progresses = recent.readRecentFromDb(PAGE_SIZE * (curPage + 1), PAGE_SIZE)
                //Log.d(TAG, "progresses:"+progresses);
                val entryList = ArrayList<FileListEntry>()
                if (null != progresses && progresses.size > 0) {
                    var entry: FileListEntry
                    var file: File
                    val path = Environment.getExternalStorageDirectory().path
                    for (progress in progresses) {
                        try {
                            file = File(path + "/" + progress.path)
                            entry = FileListEntry(FileListEntry.RECENT, 0, file, showExtension)
                            entry.akProgress = progress
                            entryList.add(entry)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                    }
                    /*if (entryList.size()>0) {
                        try {
                            Collections.sort(entryList, new Comparator<FileListEntry>() {
                                public int compare(FileListEntry f1, FileListEntry f2) {
                                    if (f1==null) throw new RuntimeException("f1 is null inside sort");
                                    if (f2==null) throw new RuntimeException("f2 is null inside sort");
                                    try {
                                        return f1.getAkProgress().compare(f1.getAkProgress(), f2.getAkProgress());
                                    } catch (NullPointerException e) {
                                        throw new RuntimeException("failed to compare "+f1+" and "+f2, e);
                                    }
                                }
                            });
                        } catch (NullPointerException e) {
                            throw new RuntimeException("failed to sort file list "+" for path ", e);
                        }
                    }*/
                }

                /*long newTime=System.currentTimeMillis()-now;
                if (newTime<600l) {
                    newTime=600l-newTime;
                } else {
                    newTime=0;
                }

                if (newTime>0) {
                    try {
                        Thread.sleep(newTime);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }*/

                if (entryList.size > 0) {
                    totalCount = count
                    curPage++
                    totalPage = count / PAGE_SIZE
                    if (count % PAGE_SIZE > 0) {
                        totalPage++
                    }
                }
                //Log.d(TAG, String.format("totalCount:%d, curPage:%d, totalPage:%d,count:%d", totalCount, curPage, totalPage, count));

                return entryList
            }

            override fun onPostExecute(entries: ArrayList<FileListEntry>?) {
                if (null != entries && entries.size > 0) {
                    if (fileList == null) {
                        fileList = ArrayList<FileListEntry>()
                    }
                    if (curPage == 0) {
                        fileList!!.clear()
                    }
                    fileList!!.addAll(entries)
                    fileListAdapter!!.setData(fileList)
                    fileListAdapter!!.notifyDataSetChanged()
                    //filesListView.setSelection(0);
                }
                mSwipeRefreshWidget!!.isRefreshing = false
            }
        }, null)
    }

    override fun onScrollStateChanged(view: AbsListView, scrollState: Int) {

    }

    override fun onScroll(view: AbsListView, firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int) {
        // Detect whether the last visible item has changed
        val lastVisibleItemIndex = firstVisibleItem + visibleItemCount

        /**
         * Check that the last item has changed, we have any items, and that
         * the last item is visible. lastVisibleItemIndex is a zero-based
         * index, so we onEvent one to it to check against totalItemCount.
         */
        if (visibleItemCount > 0 && lastVisibleItemIndex + 1 == totalItemCount) {
            if (lastVisibleItemIndex != mSavedLastVisibleIndex) {
                mSavedLastVisibleIndex = lastVisibleItemIndex
                //mOnLastItemVisibleListener.onLastItemVisible();
                if (curPage < totalPage) {
                    mSwipeRefreshWidget!!.isRefreshing = true
                    loadMore()
                } else {
                    Log.d(TAG, "curPage>=totalPage:$curPage totalPage:$totalPage")
                }
            }
        }
    }

    private fun loadMore() {
        getHistory()
    }

    companion object {

        val TAG = "HistoryFragment"
        internal val PAGE_SIZE = 15
        @JvmField val ACTION_STARTED = "com.example.android.supportv4.STARTED"
        @JvmField val ACTION_UPDATE = "com.example.android.supportv4.UPDATE"
        @JvmField val ACTION_STOPPED = "com.example.android.supportv4.STOPPED"
    }
}
