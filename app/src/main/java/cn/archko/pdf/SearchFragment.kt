package cn.archko.pdf

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.DialogFragment
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.EditText
import android.widget.ImageView
import android.widget.ListView
import cx.hell.android.pdfviewpro.FileListEntry
import org.vudroid.pdfdroid.PdfViewerActivity
import java.io.File
import java.io.FileFilter
import java.util.*

/**
 * @author: archko 2016/2/14 :15:58
 */
open class SearchFragment : DialogFragment(), AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {

    lateinit var editView: EditText
    lateinit var filesListView: ListView
    lateinit var imgClose: ImageView
    private val fileFilter: FileFilter? = null
    protected var fileListAdapter: AKAdapter? = null
    protected var fileList: ArrayList<FileListEntry>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val themeId = R.style.AppDialogTheme
        /*if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            themeId = android.R.style.Theme_Material_Light_Dialog;
        }*/
        setStyle(DialogFragment.STYLE_NORMAL, themeId)
    }

    override fun setArguments(args: Bundle?) {
        super.setArguments(args)
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater!!.inflate(R.layout.filesearcher, container, false)

        dialog.setTitle(R.string.menu_search)
        editView = view.findViewById<EditText>(R.id.searchEdit)
        imgClose = view.findViewById<ImageView>(R.id.img_close)
        filesListView = view.findViewById<ListView>(R.id.files)

        imgClose.setOnClickListener { clear() }

        editView.addTextChangedListener(ATextWatcher())

        return view
    }

    private inner class ATextWatcher : TextWatcher {

        lateinit var string: String

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            string = s.toString()
        }

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

        }

        override fun afterTextChanged(s: Editable) {
            val cs = s.toString()
            if (!TextUtils.isEmpty(cs)) {
                if (cs != string) {
                    search(cs.toLowerCase())
                }
            } else {
                clearList()
            }
        }
    }

    private fun clearList() {
        if (fileList!!.size > 0) {
            fileList!!.clear()
            fileListAdapter!!.notifyDataSetChanged()
        }
    }

    private fun clear() {
        editView.text = null
        clearList()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (null == fileListAdapter) {
            fileListAdapter = AKAdapter(activity)
            this.fileListAdapter!!.setMode(AKAdapter.TYPE_SEARCH)
        }
        filesListView.adapter = this.fileListAdapter
        filesListView.onItemClickListener = this
        filesListView.onItemLongClickListener = this
    }

    fun updateAdapter() {}

    fun search(keyword: String) {
        if (!isResumed) {
            return
        }
        if (null == fileList) {
            this.fileList = ArrayList<FileListEntry>()
        }
        fileList!!.clear()

        val home = getHome()
        doSearch(fileList!!, keyword, File(home))

        fileListAdapter!!.setData(fileList!!)
        fileListAdapter!!.notifyDataSetChanged()
        //this.filesListView.setSelection(0);
    }

    private fun doSearch(fileList: ArrayList<FileListEntry>, keyword: String, dir: File) {
        if (dir.isDirectory) {
            val files = dir.listFiles(this.fileFilter)

            if (files != null && files.size > 0) {
                for (f in files) {
                    if (f.isFile) {
                        if (f.name.toLowerCase().contains(keyword)) {
                            fileList.add(FileListEntry(FileListEntry.NORMAL, -1, f, true))
                        }
                    } else {
                        doSearch(fileList, keyword, f)
                    }
                }
            }
        } else {
            if (dir.name.contains(keyword)) {
                fileList.add(FileListEntry(FileListEntry.NORMAL, -1, dir, true))
            }
        }
    }

    private fun getHome(): String {
        val defaultHome = Environment.getExternalStorageDirectory().absolutePath
        var path: String = activity?.getSharedPreferences(ChooseFileFragmentActivity.PREF_TAG, 0)!!.getString(ChooseFileFragmentActivity.PREF_HOME, defaultHome)
        if (path.length > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length - 2)
        }

        val pathFile = File(path)

        if (pathFile.exists() && pathFile.isDirectory)
            return path
        else
            return defaultHome
    }

    override fun onItemClick(parent: AdapterView<*>, v: View, position: Int, id: Long) {
        val clickedEntry = this.fileListAdapter!!.getItem(position) as FileListEntry
        val clickedFile = clickedEntry.file

        if (null == clickedFile || !clickedFile.exists())
            return

        pdfView(clickedFile)
    }

    override fun onItemLongClick(parent: AdapterView<*>, view: View, position: Int, id: Long): Boolean {
        val entry = this.fileListAdapter!!.getItem(position) as FileListEntry
        showFileInfoDialog(entry)
        return false
    }

    fun pdfView(f: File) {
        Log.i("", "post intent to open file " + f)
        dismiss()
        val intent = Intent()
        intent.setDataAndType(Uri.fromFile(f), "application/pdf")
        //intent.setClass(getActivity(), OpenFileActivity.class);
        intent.setClass(activity, PdfViewerActivity::class.java)
        intent.action = "android.intent.action.VIEW"
        activity?.startActivity(intent)
    }

    protected fun showFileInfoDialog(entry: FileListEntry) {
        val ft = activity?.supportFragmentManager?.beginTransaction()
        val prev = activity?.supportFragmentManager?.findFragmentByTag("dialog")
        if (prev != null) {
            ft?.remove(prev)
        }
        ft?.addToBackStack(null)

        // Create and show the dialog.
        val fileInfoFragment = FileInfoFragment()
        val bundle = Bundle()
        bundle.putSerializable(FileInfoFragment.FILE_LIST_ENTRY, entry)
        fileInfoFragment.arguments = bundle
        fileInfoFragment.show(ft, "dialog")
    }
}
