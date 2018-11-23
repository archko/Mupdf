package cn.archko.pdf

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.preference.PreferenceManager
import android.support.v4.content.FileProvider
import android.support.v4.view.MenuItemCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.util.Log
import android.view.*
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ListView
import android.widget.PopupMenu
import android.widget.TextView
import com.artifex.mupdf.viewer.DocumentActivity
import cx.hell.android.pdfviewpro.APVApplication
import cx.hell.android.pdfviewpro.FileListEntry
import cx.hell.android.pdfviewpro.Options
import org.vudroid.pdfdroid.PdfViewerActivity
import java.io.File
import java.io.FileFilter
import java.util.*

/**
 * @version 1.00.00
 * *
 * @description:
 * *
 * @author: archko 11-11-17
 */
open class BrowserFragment : RefreshableFragment(), OnItemClickListener, SwipeRefreshLayout.OnRefreshListener,
        PopupMenu.OnMenuItemClickListener, AdapterView.OnItemLongClickListener {

    private var mCurrentPath: String? = null

    protected var mSwipeRefreshWidget: SwipeRefreshLayout? = null
    protected var pathTextView: TextView? = null
    protected var filesListView: ListView? = null
    private var fileFilter: FileFilter? = null
    protected var fileListAdapter: AKAdapter? = null
    protected var fileList: ArrayList<FileListEntry>? = null

    private val dirsFirst = true
    private var showExtension: Boolean? = false
    //private val history = true

    private var setAsHomeMenuItem: MenuItem? = null
    protected var optionsMenuItem: MenuItem? = null

    protected var backMenuItem: MenuItem? = null
    protected var restoreMenuItem: MenuItem? = null
    internal var mPathMap: MutableMap<String, Int> = HashMap()
    protected var mSelectedPos = -1
    internal var mScanner: AKProgressScaner? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mCurrentPath = getHome()
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        this.setAsHomeMenuItem = menu!!.add(R.string.set_as_home)
        MenuItemCompat.setShowAsAction(this.setAsHomeMenuItem, MenuItemCompat.SHOW_AS_ACTION_IF_ROOM)
        this.optionsMenuItem = menu.add(R.string.options)
        MenuItemCompat.setShowAsAction(this.optionsMenuItem, MenuItemCompat.SHOW_AS_ACTION_IF_ROOM)
    }

    override fun onOptionsItemSelected(menuItem: MenuItem?): Boolean {
        if (menuItem === this.setAsHomeMenuItem) {
            setAsHome()
            return true
        } else if (menuItem === this.optionsMenuItem) {
            startActivity(Intent(activity, Options::class.java))
        }
        return super.onOptionsItemSelected(menuItem)
    }

    fun setAsHome() {
        val edit = activity?.getSharedPreferences(ChooseFileFragmentActivity.PREF_TAG, 0)?.edit()
        edit?.putString(ChooseFileFragmentActivity.PREF_HOME, mCurrentPath)
        edit?.apply()
    }

    open fun onBackPressed(): Boolean {
        val path = Environment.getExternalStorageDirectory().absolutePath
        if (this.mCurrentPath != path && this.mCurrentPath != "/") {
            val upFolder = File(this.mCurrentPath!!).parentFile
            if (upFolder.isDirectory) {
                this.mCurrentPath = upFolder.absolutePath
                updateAdapter()
                return true
            }
        }
        return false
    }

    override fun onResume() {
        super.onResume()
        Log.i(TAG, ".onResume." + this)
        val options = PreferenceManager.getDefaultSharedPreferences(APVApplication.getInstance())
        showExtension = options.getBoolean(Options.PREF_SHOW_EXTENSION, true)
    }

    override fun onPause() {
        super.onPause()
        Log.i(TAG, ".onPause." + this)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, ".onDestroy." + this)
    }

    override fun onDetach() {
        super.onDetach()
        Log.i(TAG, ".onDetach." + this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater?.inflate(R.layout.filechooser, container, false)

        this.pathTextView = view.findViewById<TextView>(R.id.path)
        this.filesListView = view.findViewById<ListView>(R.id.files)
        mSwipeRefreshWidget = view.findViewById<SwipeRefreshLayout>(R.id.swipe_refresh_widget) as SwipeRefreshLayout
        mSwipeRefreshWidget!!.setColorSchemeResources(R.color.text_border_pressed, R.color.text_border_pressed,
                R.color.text_border_pressed, R.color.text_border_pressed)
        mSwipeRefreshWidget!!.setOnRefreshListener(this)

        return view
    }

    override fun onRefresh() {
        update()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        this.fileFilter = FileFilter { file ->
            //return (file.isDirectory() || file.getName().toLowerCase().endsWith(".pdf"));
            if (file.isDirectory)
                return@FileFilter true
            val fname = file.name.toLowerCase()

            if (fname.endsWith(".pdf"))
                return@FileFilter true
            if (fname.endsWith(".xps"))
                return@FileFilter true
            if (fname.endsWith(".cbz"))
                return@FileFilter true
            if (fname.endsWith(".png"))
                return@FileFilter true
            if (fname.endsWith(".jpe"))
                return@FileFilter true
            if (fname.endsWith(".jpeg"))
                return@FileFilter true
            if (fname.endsWith(".jpg"))
                return@FileFilter true
            if (fname.endsWith(".jfif"))
                return@FileFilter true
            if (fname.endsWith(".jfif-tbnl"))
                return@FileFilter true
            if (fname.endsWith(".tif"))
                return@FileFilter true
            if (fname.endsWith(".tiff"))
                return@FileFilter true
            if (fname.endsWith(".epub"))
                return@FileFilter true
            false
        }

        if (null == fileList) {
            this.fileList = ArrayList<FileListEntry>()
        }
        updateAdapter()

        //registerForContextMenu(this.filesListView);
        filesListView?.onItemClickListener = this
        filesListView?.onItemLongClickListener = this
    }

    fun updateAdapter() {
        val activity = activity
        if (null == fileListAdapter) {
            fileListAdapter = AKAdapter(activity)
        }
        this.filesListView?.adapter = this.fileListAdapter
        this.filesListView?.onItemClickListener = this

        update()
    }

    override fun update() {
        if (!isResumed) {
            return
        }
        if (null == fileList) {
            this.fileList = ArrayList<FileListEntry>()
        }
        fileList!!.clear()
        this.pathTextView!!.text = this.mCurrentPath
        var entry: FileListEntry

        entry = FileListEntry(FileListEntry.HOME, resources.getString(R.string.go_home))
        fileList!!.add(entry)

        if (this.mCurrentPath != "/") {
            val upFolder = File(this.mCurrentPath!!).parentFile
            entry = FileListEntry(FileListEntry.NORMAL, -1, upFolder, "..")
            fileList!!.add(entry)
        }

        val files = File(this.mCurrentPath!!).listFiles(this.fileFilter)

        if (files != null) {
            try {
                Arrays.sort(files, Comparator<File> { f1, f2 ->
                    if (f1 == null) throw RuntimeException("f1 is null inside sort")
                    if (f2 == null) throw RuntimeException("f2 is null inside sort")
                    try {
                        if (dirsFirst && f1.isDirectory != f2.isDirectory) {
                            if (f1.isDirectory)
                                return@Comparator -1
                            else
                                return@Comparator 1
                        }
                        return@Comparator f1.name.toLowerCase().compareTo(f2.name.toLowerCase())
                    } catch (e: NullPointerException) {
                        throw RuntimeException("failed to compare $f1 and $f2", e)
                    }
                })
            } catch (e: NullPointerException) {
                throw RuntimeException("failed to sort file list " + files + " for path " + this.mCurrentPath, e)
            }

            for (file in files) {
                entry = FileListEntry(FileListEntry.NORMAL, -1, file, showExtension)
                fileList!!.add(entry)
            }
        }

        fileListAdapter!!.setData(fileList!!)
        //System.out.println("mPathMap.get(mCurrentPath):"+mPathMap.get(mCurrentPath)+ " size:"+fileList.size());
        if (null != mPathMap[mCurrentPath!!]) {
            val pos = mPathMap[mCurrentPath!!]
            if (pos!! < fileList!!.size) {
                filesListView!!.setSelection(pos!!)
            }
        }
        fileListAdapter!!.notifyDataSetChanged()
        //this.filesListView.setSelection(0);
        mSwipeRefreshWidget!!.isRefreshing = false

        startGetProgress(fileList, mCurrentPath)
    }

    private fun startGetProgress(fileList: ArrayList<FileListEntry>?, currentPath: String?) {
        if (null == mScanner) {
            mScanner = AKProgressScaner()
        }
        mScanner!!.startScan(fileList, activity!!, currentPath, object : DataListener {
            override fun onSuccess(vararg args: Any?) {
                var path = args[0] as String
                if (!mCurrentPath.equals(path)) {
                    return
                }

                fileListAdapter!!.setData(args[1] as ArrayList<FileListEntry>)
                fileListAdapter!!.notifyDataSetChanged()
            }

            override fun onFailed(vararg args: Any?) {
            }
        })
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

    fun pdfView(f: File) {
        Log.i(TAG, "post intent to open file " + f)
        val intent = Intent()
        intent.setDataAndType(Uri.fromFile(f), "application/pdf")
        //intent.setClass(getActivity(), OpenFileActivity.class);
        intent.setClass(activity, PdfViewerActivity::class.java)
        intent.action = "android.intent.action.VIEW"
        activity?.startActivity(intent)
    }

    override fun onItemClick(parent: AdapterView<*>, v: View, position: Int, id: Long) {
        val clickedEntry = this.fileList!![position]
        val clickedFile: File?

        if (clickedEntry.type == FileListEntry.HOME) {
            clickedFile = File(getHome())
        } else {
            clickedFile = clickedEntry.file
        }

        if (null == clickedFile || !clickedFile.exists())
            return

        if (clickedFile.isDirectory) {
            mPathMap.put(mCurrentPath!!, position)
            this.mCurrentPath = clickedFile.absolutePath
            updateAdapter()
        } else {
            pdfView(clickedFile)
        }
    }

    override fun onItemLongClick(parent: AdapterView<*>, view: View, position: Int, id: Long): Boolean {
        val entry = this.fileListAdapter!!.getItem(position) as FileListEntry
        if (!entry.isDirectory && entry.type != FileListEntry.HOME) {
            mSelectedPos = position
            prepareMenu(view, entry)
            return true
        }
        mSelectedPos = -1
        return false
    }

    //--------------------- popupMenu ---------------------

    /**
     * 初始化自定义菜单

     * @param anchorView 菜单显示的锚点View。
     */
    fun prepareMenu(anchorView: View, entry: FileListEntry) {
        val popupMenu = PopupMenu(activity, anchorView)

        onCreateCustomMenu(popupMenu)
        onPrepareCustomMenu(popupMenu, entry)
        //return showCustomMenu(anchorView);
        popupMenu.setOnMenuItemClickListener(this)
        popupMenu.show()
    }

    /**
     * 创建菜单项，供子类覆盖，以便动态地添加菜单项。

     * @param menuBuilder
     */
    fun onCreateCustomMenu(menuBuilder: PopupMenu) {
        /*menuBuilder.add(0, 1, 0, "title1");*/
        menuBuilder.menu.clear()
    }

    /**
     * 创建菜单项，供子类覆盖，以便动态地添加菜单项。

     * @param menuBuilder
     */
    fun onPrepareCustomMenu(menuBuilder: PopupMenu, entry: FileListEntry) {
        /*menuBuilder.add(0, 1, 0, "title1");*/
        if (entry.type == FileListEntry.HOME) {
            //menuBuilder.getMenu().add(R.string.set_as_home);
        } else if (entry.type == FileListEntry.RECENT) {
            menuBuilder.menu.add(0, vudroidContextMenuItem, 0, getString(R.string.menu_vudroid))
            menuBuilder.menu.add(0, mupdfContextMenuItem, 0, getString(R.string.menu_mupdf))
            menuBuilder.menu.add(0, EXAMPLEContextMenuItem, 0, "Mupdf new Viewer")
            //menuBuilder.menu.add(0, apvContextMenuItem, 0, getString(R.string.menu_apv))
            menuBuilder.menu.add(0, otherContextMenuItem, 0, getString(R.string.menu_other))
            menuBuilder.menu.add(0, infoContextMenuItem, 0, getString(R.string.menu_info))
            /*if (Build.VERSION_CODES.LOLLIPOP <= Build.VERSION.SDK_INT) {
				menuBuilder.getMenu().add(0, LPDFContextMenuItem, 0, "LOLLIPOP_PDF_Viewer");
			}*/

            menuBuilder.menu.add(0, removeContextMenuItem, 0, getString(R.string.remove_from_recent))
        } else if (!entry.isDirectory && entry.type != FileListEntry.HOME) {
            menuBuilder.menu.add(0, vudroidContextMenuItem, 0, getString(R.string.menu_vudroid))
            menuBuilder.menu.add(0, mupdfContextMenuItem, 0, getString(R.string.menu_mupdf))
            menuBuilder.menu.add(0, EXAMPLEContextMenuItem, 0, "Mupdf new Viewer")
            //menuBuilder.menu.add(0, apvContextMenuItem, 0, getString(R.string.menu_apv))
            menuBuilder.menu.add(0, otherContextMenuItem, 0, getString(R.string.menu_other))
            menuBuilder.menu.add(0, infoContextMenuItem, 0, getString(R.string.menu_info))
            /*if (Build.VERSION_CODES.LOLLIPOP <= Build.VERSION.SDK_INT) {
				menuBuilder.getMenu().add(0, LPDFContextMenuItem, 0, "LOLLIPOP_PDF_Viewer");
			}*/

            menuBuilder.menu.add(0, deleteContextMenuItem, 0, getString(R.string.delete))
        }
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        if (null == fileList || mSelectedPos == -1) {
            return true
        }
        val position = mSelectedPos
        val entry = this.fileList!![position]
        if (item.itemId == deleteContextMenuItem) {
            Log.d(TAG, "delete:" + entry)
            if (entry.type == FileListEntry.NORMAL && !entry.isDirectory) {
                entry.file.delete()
                update()
            }
            return true
        } else if (item.itemId == removeContextMenuItem) {
            if (entry.type == FileListEntry.RECENT) {
                AKRecent.getInstance(activity?.applicationContext).removeFromDb(entry.file.absolutePath)
                update()
            }
        } else {
            val clickedFile: File?

            clickedFile = entry.file

            if (null != clickedFile && clickedFile.exists()) {
                val uri = Uri.parse(clickedFile.absolutePath)
                val intent: Intent
                intent = Intent()
                intent.action = Intent.ACTION_VIEW
                intent.data = uri

                if (item.itemId == vudroidContextMenuItem) {
                    intent.setClass(activity, PdfViewerActivity::class.java)
                    intent.data = Uri.fromFile(clickedFile)
                    startActivity(intent)
                    return true
                } else if (item.itemId == mupdfContextMenuItem) {
                    intent.setClass(activity, MuPDFRecyclerActivity::class.java)
                    startActivity(intent)
                    return true
                } else if (item.itemId == otherContextMenuItem) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        intent.setDataAndType(FileProvider.getUriForFile(getContext()!!, "cn.archko.mupdf.fileProvider", clickedFile), "application/pdf");
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.addCategory(Intent.CATEGORY_DEFAULT);
                    } else {
                        intent.setDataAndType(Uri.fromFile(clickedFile), "application/pdf")
                    }
                    startActivity(intent)
                } else if (infoContextMenuItem == item.itemId) {
                    showFileInfoDialog(entry)
                } else if (EXAMPLEContextMenuItem == item.itemId) {
                    intent.setClass(activity, DocumentActivity::class.java)
                    // API>=21: intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT); /* launch as a new document */
                    intent.setAction(Intent.ACTION_VIEW)
                    intent.setData(Uri.fromFile(clickedFile))
                    startActivity(intent)
                    return true
                }
            }
        }
        return false
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
        fileInfoFragment.setListener(object : DataListener {
            override fun onSuccess(vararg args: Any?) {
                val fileEntry = args[0] as FileListEntry
                filesListView?.let { prepareMenu(it, fileEntry) }
            }

            override fun onFailed(vararg args: Any?) {
            }
        })
        fileInfoFragment.show(ft, "dialog")
    }

    companion object {

        val TAG = "BrowserFragment"

        protected val deleteContextMenuItem = Menu.FIRST + 100
        protected val removeContextMenuItem = Menu.FIRST + 101

        protected val mupdfContextMenuItem = Menu.FIRST + 110
        //protected val apvContextMenuItem = Menu.FIRST + 111
        protected val vudroidContextMenuItem = Menu.FIRST + 112
        protected val otherContextMenuItem = Menu.FIRST + 113
        protected val infoContextMenuItem = Menu.FIRST + 114
        //protected val LPDFContextMenuItem = Menu.FIRST + 115
        protected val EXAMPLEContextMenuItem = Menu.FIRST + 116
    }
}
