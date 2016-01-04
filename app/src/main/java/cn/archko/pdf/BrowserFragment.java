package cn.archko.pdf;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import com.artifex.mupdfdemo.MuPDFActivity;
import org.vudroid.pdfdroid.PdfViewerActivity;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import cx.hell.android.pdfviewpro.APVApplication;
import cx.hell.android.pdfviewpro.FileListEntry;
import cx.hell.android.pdfviewpro.OpenFileActivity;
import cx.hell.android.pdfviewpro.Options;

/**
 * @version 1.00.00
 * @description:
 * @author: archko 11-11-17
 */
public class BrowserFragment extends RefreshableFragment implements OnItemClickListener,
		SwipeRefreshLayout.OnRefreshListener, PopupMenu.OnMenuItemClickListener, AdapterView.OnItemLongClickListener {

    public static final String TAG="BrowserFragment";

    private String currentPath;

	protected SwipeRefreshLayout mSwipeRefreshWidget;
	protected TextView pathTextView = null;
	protected ListView filesListView = null;
	private FileFilter fileFilter = null;
	protected AKAdapter fileListAdapter = null;
	protected ArrayList<FileListEntry> fileList = null;
	
	private Boolean dirsFirst = true;
	private Boolean showExtension = false;
	private Boolean history = true;

	private MenuItem setAsHomeMenuItem = null;
	protected MenuItem optionsMenuItem = null;

	protected static final int deleteContextMenuItem=Menu.FIRST+100;
	protected static final int removeContextMenuItem=Menu.FIRST+101;
	protected static final int openContextMenuItem=Menu.FIRST+102;

	protected static final int mupdfContextMenuItem=Menu.FIRST+110;
	protected static final int apvContextMenuItem=Menu.FIRST+111;
	protected static final int vudroidContextMenuItem=Menu.FIRST+112;
	protected static final int otherContextMenuItem=Menu.FIRST+113;

    protected MenuItem backMenuItem = null;
    protected MenuItem restoreMenuItem = null;
	Map<String, Integer> mPathMap=new HashMap<>();
	protected int mSelectedPos=-1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        currentPath = getHome();
        setHasOptionsMenu(true);
    }

    @Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		this.setAsHomeMenuItem = menu.add(R.string.set_as_home);
    	MenuItemCompat.setShowAsAction(this.setAsHomeMenuItem, MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
    	this.optionsMenuItem = menu.add(R.string.options);
    	MenuItemCompat.setShowAsAction(this.optionsMenuItem, MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
	}
    

	@Override
	public boolean onOptionsItemSelected(MenuItem menuItem) {
		if (menuItem == this.setAsHomeMenuItem) {
    		setAsHome();
    		return true;
    	}
    	else if (menuItem == this.optionsMenuItem){
    		startActivity(new Intent(getActivity(), Options.class));
    	}
		return super.onOptionsItemSelected(menuItem);
	}
	
	public void setAsHome() {
		SharedPreferences.Editor edit = getActivity().getSharedPreferences(ChooseFileFragmentActivity.PREF_TAG, 0).edit();
		edit.putString(ChooseFileFragmentActivity.PREF_HOME, currentPath);
		edit.apply();
    }

	public boolean onBackPressed() {
		String path=Environment.getExternalStorageDirectory().getAbsolutePath();
		if (!this.currentPath.equals(path)&&!this.currentPath.equals("/")) {
			File upFolder=new File(this.currentPath).getParentFile();
			if (upFolder.isDirectory()) {
				this.currentPath=upFolder.getAbsolutePath();
				updateAdapter();
				return true;
			}
		}
		return false;
	}

	@Override
	public void onResume() {
        super.onResume();
        Log.i(TAG, ".onResume."+this);
		SharedPreferences options = PreferenceManager.getDefaultSharedPreferences(APVApplication.getInstance());
		showExtension = options.getBoolean(Options.PREF_SHOW_EXTENSION, false);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i(TAG, ".onPause."+this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, ".onDestroy."+this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.i(TAG, ".onDetach."+this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view=inflater.inflate(R.layout.filechooser, container, false);

    	this.pathTextView = (TextView) view.findViewById(R.id.path);
    	this.filesListView = (ListView) view.findViewById(R.id.files);
		mSwipeRefreshWidget = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh_widget);
		mSwipeRefreshWidget.setColorSchemeResources(R.color.text_border_pressed, R.color.text_border_pressed,
				R.color.text_border_pressed, R.color.text_border_pressed);
		mSwipeRefreshWidget.setOnRefreshListener(this);

		return view;
    }

	@Override
	public void onRefresh() {
		update();
	}

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

    	this.fileFilter = new FileFilter() {
    		public boolean accept(File file) {
    			//return (file.isDirectory() || file.getName().toLowerCase().endsWith(".pdf"));
    			if (file.isDirectory())
					return true;
				String fname = file.getName().toLowerCase();
				
					if (fname.endsWith(".pdf"))
						return true;
					if (fname.endsWith(".xps"))
						return true;
					if (fname.endsWith(".cbz"))
						return true;
					if (fname.endsWith(".png"))
						return true;
					if (fname.endsWith(".jpe"))
						return true;
					if (fname.endsWith(".jpeg"))
						return true;
					if (fname.endsWith(".jpg"))
						return true;
					if (fname.endsWith(".jfif"))
						return true;
					if (fname.endsWith(".jfif-tbnl"))
						return true;
					if (fname.endsWith(".tif"))
						return true;
					if (fname.endsWith(".tiff"))
						return true;
					if (fname.endsWith(".epub"))
						return true;
					return false;
    		}
    	};
    	
    	if (null==fileList) {
			this.fileList = new ArrayList<FileListEntry>();
		}
		updateAdapter();

    	//registerForContextMenu(this.filesListView);
		filesListView.setOnItemClickListener(this);
		filesListView.setOnItemLongClickListener(this);
    }
    
    public void updateAdapter() {
    	final Activity activity = getActivity();
    	if (null==fileListAdapter) {
            fileListAdapter=new AKAdapter(activity);
		}
		this.filesListView.setAdapter(this.fileListAdapter);
    	this.filesListView.setOnItemClickListener(this);
    	
    	update();
	}

	public void update() {
		if (!isResumed()) {
			return;
		}
		if (null==fileList) {
			this.fileList = new ArrayList<FileListEntry>();
		}
        fileList.clear();
    	this.pathTextView.setText(this.currentPath);
		FileListEntry entry;

        entry=new FileListEntry(FileListEntry.HOME, getResources().getString(R.string.go_home));
        fileList.add(entry);
    	
    	if (!this.currentPath.equals("/")) {
    		File upFolder = new File(this.currentPath).getParentFile();
            entry=new FileListEntry(FileListEntry.NORMAL, -1, upFolder, "..");
            fileList.add(entry);
    	}
    	
    	File files[] = new File(this.currentPath).listFiles(this.fileFilter);

    	if (files != null) {
	    	try {
		    	Arrays.sort(files, new Comparator<File>() {
		    		public int compare(File f1, File f2) {
		    			if (f1 == null) throw new RuntimeException("f1 is null inside sort");
		    			if (f2 == null) throw new RuntimeException("f2 is null inside sort");
		    			try {
		    				if (dirsFirst && f1.isDirectory() != f2.isDirectory()) {
		    					if (f1.isDirectory())
		    						return -1;
		    					else
		    						return 1;
		    				}
		    				return f1.getName().toLowerCase().compareTo(f2.getName().toLowerCase());
		    			} catch (NullPointerException e) {
		    				throw new RuntimeException("failed to compare " + f1 + " and " + f2, e);
		    			}
					}
		    	});
	    	} catch (NullPointerException e) {
	    		throw new RuntimeException("failed to sort file list " + files + " for path " + this.currentPath, e);
	    	}
	    	
	    	for (File file:files) {
	    		entry = new FileListEntry(FileListEntry.NORMAL, -1, file, showExtension);
                fileList.add(entry);
	    	}
    	}

        fileListAdapter.setData(fileList);
		//System.out.println("mPathMap.get(currentPath):"+mPathMap.get(currentPath)+ " size:"+fileList.size());
		if (null!=mPathMap.get(currentPath)) {
			int pos=mPathMap.get(currentPath);
			if (pos<fileList.size()) {
				filesListView.setSelection(pos);
			}
		}
		fileListAdapter.notifyDataSetChanged();
    	//this.filesListView.setSelection(0);
		mSwipeRefreshWidget.setRefreshing(false);
	}

	private String getHome() {
    	String defaultHome = Environment.getExternalStorageDirectory().getAbsolutePath(); 
		String path = getActivity().getSharedPreferences(ChooseFileFragmentActivity.PREF_TAG, 0).
				getString(ChooseFileFragmentActivity.PREF_HOME, defaultHome);
		if (path.length()>1 && path.endsWith("/")) {
			path = path.substring(0,path.length()-2);
		}

		File pathFile = new File(path);

		if (pathFile.exists() && pathFile.isDirectory())
			return path;
		else
			return defaultHome;
    }
	
	public void pdfView(File f) {
		Log.i(TAG, "post intent to open file "+f);
		Intent intent = new Intent();
		intent.setDataAndType(Uri.fromFile(f), "application/pdf");
		//intent.setClass(getActivity(), OpenFileActivity.class);
        intent.setClass(getActivity(), PdfViewerActivity.class);
		intent.setAction("android.intent.action.VIEW");
		getActivity().startActivity(intent);
    }

	@Override
	public void onItemClick(AdapterView parent, View v, int position, long id) {
		FileListEntry clickedEntry = this.fileList.get(position);
    	File clickedFile;

    	if (clickedEntry.getType() == FileListEntry.HOME) {
    		clickedFile = new File(getHome());
    	}
    	else {
    		clickedFile = clickedEntry.getFile();
    	}
    	
    	if (null==clickedFile||!clickedFile.exists())
    		return;
    	
    	if (clickedFile.isDirectory()) {
			mPathMap.put(currentPath, position);
    		this.currentPath = clickedFile.getAbsolutePath();
    		updateAdapter();
    	} else {
    		pdfView(clickedFile);
    	}
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		FileListEntry entry = (FileListEntry) this.fileListAdapter.getItem(position);
		if (!entry.isDirectory() && entry.getType() != FileListEntry.HOME) {
			mSelectedPos=position;
			prepareMenu(view, entry);
			return true;
		}
		mSelectedPos=-1;
		return false;
	}
    
    /*@Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    	super.onCreateContextMenu(menu, v, menuInfo);
    	
    	if (v == this.filesListView) {
    		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
    		
    		if (info.position < 0)
    			return;
    		
        	FileListEntry entry = this.fileList.get(info.position);
        	
        	*//*if (entry.getType() == FileListEntry.HOME) {
        		setAsHomeContextMenuItem = menu.add(R.string.set_as_home);
        	}
        	else*//* if (entry.getType() == FileListEntry.RECENT) {
                menu.add(0, apvContextMenuItem, 0, getString(R.string.menu_apv));
                menu.add(0, mupdfContextMenuItem, 0, getString(R.string.menu_mupdf));
                menu.add(0, vudroidContextMenuItem, 0, getString(R.string.menu_vudroid));
				menu.add(0, otherContextMenuItem, 0, getString(R.string.menu_other));

        		//openContextMenuItem = menu.add(R.string.open);
        		menu.add(0, removeContextMenuItem, 0, getString(R.string.remove_from_recent));
        	}
        	else if (! entry.isDirectory()&&entry.getType() != FileListEntry.HOME) {
				menu.add(0, apvContextMenuItem, 0, getString(R.string.menu_apv));
				menu.add(0, mupdfContextMenuItem, 0, getString(R.string.menu_mupdf));
				menu.add(0, vudroidContextMenuItem, 0, getString(R.string.menu_vudroid));
				menu.add(0, otherContextMenuItem, 0, getString(R.string.menu_other));

        		//openContextMenuItem = menu.add(R.string.open);
        		menu.add(0, deleteContextMenuItem, 0, getString(R.string.delete));
        	}
    	}
    }
	
	@Override
    public boolean onContextItemSelected(MenuItem item) {
    	return contextItemSeleted(item);
    }
	
	protected boolean contextItemSeleted(MenuItem item) {
		if (null==fileList) {
			return true;
		}
    	int position =
        		((AdapterView.AdapterContextMenuInfo)item.getMenuInfo()).position;
		Log.d(TAG, "delete:"+position+ "++"+ (item.getItemId() == deleteContextMenuItem));
		if (item.getItemId() == deleteContextMenuItem) {
    		FileListEntry entry = this.fileList.get(position);
			Log.d(TAG, "delete:"+entry);
    		if (entry.getType() == FileListEntry.NORMAL &&
    				! entry.isDirectory()) {
    			entry.getFile().delete();
    			update();
    		}    		
    		return true;
    	}
    	else if (item.getItemId() == removeContextMenuItem) {
    		FileListEntry entry = this.fileList.get(position);
    		if (entry.getType() == FileListEntry.RECENT) {//TODO
                AKRecent.getInstance(getActivity().getApplicationContext()).removeFromDb(entry.getFile().getAbsolutePath());
    			update();
    		}
    	} else if (item.getItemId() == openContextMenuItem) {
			FileListEntry entry = this.fileList.get(position);
			File clickedFile;

			clickedFile = entry.getFile();

			if (clickedFile.exists()) {
				pdfView(clickedFile);
				return true;
			}
		} else {
            FileListEntry entry = this.fileList.get(position);
            File clickedFile;

            clickedFile = entry.getFile();

            if (null!=clickedFile&&clickedFile.exists()) {
                Uri uri = Uri.parse(clickedFile.getAbsolutePath());
                Intent intent ;
                intent=new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                intent.setData(uri);

                if (item.getItemId()==vudroidContextMenuItem) {
                    intent.setClass(getActivity(), PdfViewerActivity.class);
                    intent.setData(Uri.fromFile(clickedFile));
                    startActivity(intent);
                    return true;
                } else if (item.getItemId()==mupdfContextMenuItem) {
                    intent.setClass(getActivity(), MuPDFActivity.class);
                    startActivity(intent);
                    return true;
                } else if (item.getItemId()==apvContextMenuItem) {
                    intent.setClass(getActivity(), OpenFileActivity.class);
                    intent.setDataAndType(Uri.fromFile(clickedFile), "application/pdf");
                    startActivity(intent);
                    return true;
                } else if (item.getItemId()==otherContextMenuItem) {
					intent.setAction(Intent.ACTION_VIEW);
					intent.setDataAndType(Uri.fromFile(clickedFile), "application/pdf");
					startActivity(intent);
				}
            }
        }
    	return false;
	}*/

	//--------------------- popupMenu ---------------------

	/**
	 * 初始化自定义菜单
	 *
	 * @param anchorView 菜单显示的锚点View。
	 */
	public void prepareMenu(View anchorView, FileListEntry entry) {
		PopupMenu popupMenu=new PopupMenu(getActivity(), anchorView);

		onCreateCustomMenu(popupMenu);
		onPrepareCustomMenu(popupMenu, entry);
		//return showCustomMenu(anchorView);
		popupMenu.setOnMenuItemClickListener(this);
		popupMenu.show();
	}

	/**
	 * 创建菜单项，供子类覆盖，以便动态地添加菜单项。
	 *
	 * @param menuBuilder
	 */
	public void onCreateCustomMenu(PopupMenu menuBuilder) {
        /*menuBuilder.add(0, 1, 0, "title1");*/
		menuBuilder.getMenu().clear();
	}

	/**
	 * 创建菜单项，供子类覆盖，以便动态地添加菜单项。
	 *
	 * @param menuBuilder
	 */
	public void onPrepareCustomMenu(PopupMenu menuBuilder, FileListEntry entry) {
        /*menuBuilder.add(0, 1, 0, "title1");*/
		if (entry.getType() == FileListEntry.HOME) {
			//menuBuilder.getMenu().add(R.string.set_as_home);
		} else if (entry.getType() == FileListEntry.RECENT) {
			menuBuilder.getMenu().add(0, apvContextMenuItem, 0, getString(R.string.menu_apv));
			menuBuilder.getMenu().add(0, mupdfContextMenuItem, 0, getString(R.string.menu_mupdf));
			menuBuilder.getMenu().add(0, vudroidContextMenuItem, 0, getString(R.string.menu_vudroid));
			menuBuilder.getMenu().add(0, otherContextMenuItem, 0, getString(R.string.menu_other));

			menuBuilder.getMenu().add(0, removeContextMenuItem, 0, getString(R.string.remove_from_recent));
		} else if (! entry.isDirectory()&&entry.getType() != FileListEntry.HOME) {
			menuBuilder.getMenu().add(0, apvContextMenuItem, 0, getString(R.string.menu_apv));
			menuBuilder.getMenu().add(0, mupdfContextMenuItem, 0, getString(R.string.menu_mupdf));
			menuBuilder.getMenu().add(0, vudroidContextMenuItem, 0, getString(R.string.menu_vudroid));
			menuBuilder.getMenu().add(0, otherContextMenuItem, 0, getString(R.string.menu_other));

			menuBuilder.getMenu().add(0, deleteContextMenuItem, 0, getString(R.string.delete));
		}
	}

	@Override
	public boolean onMenuItemClick(MenuItem item) {
		if (null==fileList || mSelectedPos==-1) {
			return true;
		}
		int position = mSelectedPos;
		Log.d(TAG, "delete:"+position+ "++"+ (item.getItemId() == deleteContextMenuItem));
		if (item.getItemId() == deleteContextMenuItem) {
			FileListEntry entry = this.fileList.get(position);
			Log.d(TAG, "delete:"+entry);
			if (entry.getType() == FileListEntry.NORMAL && ! entry.isDirectory()) {
				entry.getFile().delete();
				update();
			}
			return true;
		}
		else if (item.getItemId() == removeContextMenuItem) {
			FileListEntry entry = this.fileList.get(position);
			if (entry.getType() == FileListEntry.RECENT) {
				AKRecent.getInstance(getActivity().getApplicationContext()).removeFromDb(entry.getFile().getAbsolutePath());
				update();
			}
		} else if (item.getItemId() == openContextMenuItem) {
			FileListEntry entry = this.fileList.get(position);
			File clickedFile;

			clickedFile = entry.getFile();

			if (clickedFile.exists()) {
				pdfView(clickedFile);
				return true;
			}
		} else {
			FileListEntry entry = this.fileList.get(position);
			File clickedFile;

			clickedFile = entry.getFile();

			if (null!=clickedFile&&clickedFile.exists()) {
				Uri uri = Uri.parse(clickedFile.getAbsolutePath());
				Intent intent ;
				intent=new Intent();
				intent.setAction(Intent.ACTION_VIEW);
				intent.setData(uri);

				if (item.getItemId()==vudroidContextMenuItem) {
					intent.setClass(getActivity(), PdfViewerActivity.class);
					intent.setData(Uri.fromFile(clickedFile));
					startActivity(intent);
					return true;
				} else if (item.getItemId()==mupdfContextMenuItem) {
					intent.setClass(getActivity(), MuPDFActivity.class);
					startActivity(intent);
					return true;
				} else if (item.getItemId()==apvContextMenuItem) {
					intent.setClass(getActivity(), OpenFileActivity.class);
					intent.setDataAndType(Uri.fromFile(clickedFile), "application/pdf");
					startActivity(intent);
					return true;
				} else if (item.getItemId()==otherContextMenuItem) {
					intent.setAction(Intent.ACTION_VIEW);
					intent.setDataAndType(Uri.fromFile(clickedFile), "application/pdf");
					startActivity(intent);
				}
			}
		}
		return false;
	}
}
