package cx.hell.android.pdfviewpro;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import com.artifex.mupdfdemo.R;

/**
 * @version 1.00.00
 * @description:
 * @author: archko 11-11-17
 */
public class HistoryFragment extends BrowserFragment {

    public static final String TAG="HistoryFragment";
	private Recent recent = null;
	private Boolean showExtension = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    
    @Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		menu.clear();
    	this.optionsMenuItem = menu.add(R.string.options);
    	MenuItemCompat.setShowAsAction(this.optionsMenuItem, MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
	}
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view=super.onCreateView(inflater, container, savedInstanceState);

    	this.pathTextView.setVisibility(View.GONE);
        
        return view;
    }
    
    public void update() {
    	if (null==fileListAdapter) {
			return;
		}
    	this.fileListAdapter.clear();
    	
		FileListEntry entry;

		recent = new Recent(getActivity());

		for (int i = 0; i < recent.size(); i++) {
			File file = new File(recent.get(i));

			entry = new FileListEntry(FileListEntry.RECENT, i, file, showExtension);
			this.fileList.add(entry);
		}
    	    	
    	this.filesListView.setSelection(0);
    }
}
