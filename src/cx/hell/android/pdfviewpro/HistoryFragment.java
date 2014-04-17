package cx.hell.android.pdfviewpro;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

import android.os.AsyncTask;
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
import cn.me.archko.pdf.AKProgress;
import cn.me.archko.pdf.AKRecent;
import cn.me.archko.pdf.Util;
import com.artifex.mupdfdemo.R;

/**
 * @version 1.00.00
 * @description:
 * @author: archko 11-11-17
 */
public class HistoryFragment extends BrowserFragment {

    public static final String TAG="HistoryFragment";
    //private Recent recent = null;
    private Boolean showExtension=false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
        this.optionsMenuItem=menu.add(R.string.options);
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
        this.fileListAdapter.setMode(AKAdapter.TYPE_RENCENT);

        /*FileListEntry entry;

		recent = new Recent(getActivity());

		for (int i = 0; i < recent.size(); i++) {
			File file = new File(recent.get(i));

			entry = new FileListEntry(FileListEntry.RECENT, i, file, showExtension);
			this.fileList.add(entry);
		}
    	    	
    	this.filesListView.setSelection(0);*/
        Util.execute(false, new AsyncTask<Void, Void, ArrayList<FileListEntry>>() {
            @Override
            protected ArrayList<FileListEntry> doInBackground(Void... params) {
                AKRecent recent=AKRecent.getInstance(HistoryFragment.this.getActivity());
                recent.readRecent();
                ArrayList<AKProgress> progresses=recent.getAkProgresses();
                Log.d(TAG, "progresses:"+progresses);
                ArrayList<FileListEntry> entryList=new ArrayList<FileListEntry>();
                if (null!=progresses&&progresses.size()>0) {
                    FileListEntry entry;
                    entryList.clear();
                    for (AKProgress progress : progresses) {
                        entry=new FileListEntry(FileListEntry.RECENT, 0, new File(progress.path), showExtension);
                        entry.setAkProgress(progress);
                        entryList.add(entry);
                    }
                    if (entryList.size()>0) {
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
                    }
                }

                return entryList;
            }

            @Override
            protected void onPostExecute(ArrayList<FileListEntry> entries) {
                if (null!=entries&&entries.size()>0) {
                    fileList=entries;
                    fileListAdapter.setData(fileList);
                    fileListAdapter.notifyDataSetChanged();
                    filesListView.setSelection(0);
                }
            }
        }, (Void[]) null);
    }
}
