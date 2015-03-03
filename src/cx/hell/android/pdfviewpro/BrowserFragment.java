package cx.hell.android.pdfviewpro;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import cn.me.archko.pdf.AKRecent;
import com.artifex.mupdfdemo.MuPDFActivity;
import com.artifex.mupdfdemo.R;
import org.vudroid.pdfdroid.PdfViewerActivity;

/**
 * @version 1.00.00
 * @description:
 * @author: archko 11-11-17
 */
public class BrowserFragment extends RefreshableFragment implements OnItemClickListener, SwipeRefreshLayout.OnRefreshListener {

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
	protected MenuItem deleteContextMenuItem = null;
	protected MenuItem removeContextMenuItem = null;
	protected MenuItem openContextMenuItem = null;

    private MenuItem mupdfContextMenuItem= null;
    private MenuItem apvContextMenuItem= null;
    private MenuItem vudroidContextMenuItem= null;

    protected MenuItem backMenuItem = null;
    protected MenuItem restoreMenuItem = null;

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
		edit.commit();
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

        registerForContextMenu(this.filesListView);
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
					return false;
    		}
    	};
    	
    	if (null==fileList) {
			this.fileList = new ArrayList<FileListEntry>();
		}
		updateAdapter();

    	//registerForContextMenu(this.filesListView);
    }
    
    public void updateAdapter() {
    	final Activity activity = getActivity();
    	if (null==fileListAdapter) {
            fileListAdapter=new AKAdapter(activity);
			/*this.fileListAdapter = new ArrayAdapter(activity,
					R.layout.picker_entry, fileList) {
				public View getView(int position, View convertView,
						ViewGroup parent) {
					View v;

					if (convertView == null) {
						v = View.inflate(activity, R.layout.picker_entry,
								null);
					} else {
						v = convertView;
					}

					FileListEntry entry = fileList.get(position);

					ImageView icon = (ImageView) v.findViewById(R.id.icon);
					if (entry.getType() == FileListEntry.HOME) {
						icon.setImageResource(R.drawable.ic_dir);
					} else if (entry.getType() == FileListEntry.NORMAL&& entry.isDirectory() && !entry.isUpFolder()) {
						icon.setImageResource(R.drawable.ic_dir);
					} else if (entry.isUpFolder()) {
						icon.setImageResource(R.drawable.ic_arrow_up);
					} else {
						icon.setImageResource(R.drawable.ic_doc);
					}

					TextView tv = (TextView) v.findViewById(R.id.name);

					tv.setText(entry.getLabel());
					tv.setTypeface(
							tv.getTypeface(),
							entry.getType() == FileListEntry.RECENT ? Typeface.ITALIC
									: Typeface.NORMAL);
					
					return v;
				}
			};*/
		}
		this.filesListView.setAdapter(this.fileListAdapter);
    	this.filesListView.setOnItemClickListener(this);
    	
    	update();
	}

	public void update() {
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
		Log.i(TAG, "post intent to open file " + f);
		Intent intent = new Intent();
		intent.setDataAndType(Uri.fromFile(f), "application/pdf");
		intent.setClass(getActivity(), OpenFileActivity.class);
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
    		this.currentPath = clickedFile.getAbsolutePath();
    		updateAdapter();
    	} else {
    		pdfView(clickedFile);
    	}
	}
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    	super.onCreateContextMenu(menu, v, menuInfo);
    	
    	if (v == this.filesListView) {
    		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
    		
    		if (info.position < 0)
    			return;
    		
        	FileListEntry entry = this.fileList.get(info.position);
        	
        	/*if (entry.getType() == FileListEntry.HOME) {
        		setAsHomeContextMenuItem = menu.add(R.string.set_as_home);
        	}
        	else*/ if (entry.getType() == FileListEntry.RECENT) {
                apvContextMenuItem=menu.add(R.string.menu_apv);
                mupdfContextMenuItem=menu.add(R.string.menu_mupdf);
                vudroidContextMenuItem=menu.add(R.string.menu_vudroid);

        		//openContextMenuItem = menu.add(R.string.open);
        		removeContextMenuItem = menu.add(R.string.remove_from_recent);
        	}
        	else if (! entry.isDirectory()&&entry.getType() != FileListEntry.HOME) {
                apvContextMenuItem=menu.add(R.string.menu_apv);
                mupdfContextMenuItem=menu.add(R.string.menu_mupdf);
                vudroidContextMenuItem=menu.add(R.string.menu_vudroid);

        		//openContextMenuItem = menu.add(R.string.open);
        		deleteContextMenuItem = menu.add(R.string.delete);
        	}
    	}
    }
	
	@Override
    public boolean onContextItemSelected(MenuItem item) {
    	/*if (item == deleteContextMenuItem) {
    		FileListEntry entry = this.fileList.get(position);
    		if (entry.getType() == FileListEntry.NORMAL &&
    				! entry.isDirectory()) {
    			entry.getFile().delete();
    			update();
    		}    		
    		return true;
    	}
    	else if (item == removeContextMenuItem) {
    		FileListEntry entry = this.fileList.get(position);
    		if (entry.getType() == FileListEntry.RECENT) {
    			recent.remove(entry.getRecentNumber());
    			recent.commit();
    			update();
    		}
    	}
    	return false;*/
    	return contextItemSeleted(item);
    }
	
	protected boolean contextItemSeleted(MenuItem item) {
    	int position =  
        		((AdapterView.AdapterContextMenuInfo)item.getMenuInfo()).position;
		if (item == deleteContextMenuItem) {
    		FileListEntry entry = this.fileList.get(position);
    		if (entry.getType() == FileListEntry.NORMAL &&
    				! entry.isDirectory()) {
    			entry.getFile().delete();
    			update();
    		}    		
    		return true;
    	}
    	else if (item == removeContextMenuItem) {
    		FileListEntry entry = this.fileList.get(position);
    		if (entry.getType() == FileListEntry.RECENT) {//TODO
    			/*Recent recent=new Recent(getActivity());
    			recent.remove(entry.getRecentNumber());
    			recent.commit();*/
                AKRecent.getInstance(getActivity().getApplicationContext()).removeFromDb(entry.getFile().getAbsolutePath());
    			update();
    		}
    	} else if (item == openContextMenuItem) {
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

                if (item==vudroidContextMenuItem) {
                    intent.setClass(getActivity(), PdfViewerActivity.class);
                    intent.setData(Uri.fromFile(clickedFile));
                    startActivity(intent);
                    return true;
                } else if (item==mupdfContextMenuItem) {
                    intent.setClass(getActivity(), MuPDFActivity.class);
                    startActivity(intent);
                    return true;
                } else if (item==apvContextMenuItem) {
                    intent.setClass(getActivity(), OpenFileActivity.class);
                    intent.setDataAndType(Uri.fromFile(clickedFile), "application/pdf");
                    startActivity(intent);
                    return true;
                }
            }
        }
    	return false;
	}
}
