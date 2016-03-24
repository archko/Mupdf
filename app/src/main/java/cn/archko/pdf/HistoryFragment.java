package cn.archko.pdf;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.view.MenuItemCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.Toast;

import cn.archko.pdf.utils.LengthUtils;
import cn.archko.pdf.utils.Util;
import cx.hell.android.pdfviewpro.APVApplication;
import cx.hell.android.pdfviewpro.FileListEntry;

import java.io.File;
import java.util.ArrayList;

/**
 * @version 1.00.00
 * @description:
 * @author: archko 11-11-17
 */
public class HistoryFragment extends BrowserFragment implements AbsListView.OnScrollListener {

    public static final String TAG="HistoryFragment";
    private Boolean showExtension=false;

    private int mSavedLastVisibleIndex=-1;
    int totalCount=0;
    int curPage=-1;
    int totalPage=0;
    static final int PAGE_SIZE=15;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public boolean onBackPressed() {
        return false;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        this.optionsMenuItem=menu.add(R.string.options);
        MenuItemCompat.setShowAsAction(this.optionsMenuItem, MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
        backMenuItem=menu.add(R.string.options_back);
        MenuItemCompat.setShowAsAction(this.backMenuItem, MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
        restoreMenuItem=menu.add(R.string.options_restore);
        MenuItemCompat.setShowAsAction(this.restoreMenuItem, MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem==this.backMenuItem) {
            backup();
        } else if (menuItem==this.restoreMenuItem) {
            restore();
        }
        return super.onOptionsItemSelected(menuItem);
    }

    private void backup() {
        final ProgressDialog progressDialog=new ProgressDialog(getActivity());
        progressDialog.setTitle("Waiting...");
        final long now=System.currentTimeMillis();
        Util.execute(true, new AsyncTask<Void, Void, String>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                progressDialog.setCancelable(false);
                progressDialog.show();
            }

            @Override
            protected String doInBackground(Void... params) {
                String filepath=AKRecent.getInstance(APVApplication.getInstance()).backupFromDb();
                long newTime=System.currentTimeMillis()-now;
                if (newTime<1500l) {
                    newTime=1500l-newTime;
                } else {
                    newTime=0;
                }

                try {
                    Thread.sleep(newTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return filepath;
            }

            @Override
            protected void onPostExecute(String s) {
                if (null!=progressDialog) {
                    progressDialog.dismiss();
                }

                if (!LengthUtils.isEmpty(s)) {
                    Log.d("", "file:"+s);
                    Toast.makeText(APVApplication.getInstance(), "备份成功:"+s, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(APVApplication.getInstance(), "备份失败", Toast.LENGTH_LONG).show();
                }
            }
        }, (Void[]) null);
    }

    private void restore() {
        final ProgressDialog progressDialog=new ProgressDialog(getActivity());
        progressDialog.setTitle("Waiting...");
        final long now=System.currentTimeMillis();
        Util.execute(true, new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                progressDialog.setCancelable(false);
                progressDialog.show();
            }

            @Override
            protected Boolean doInBackground(Void... params) {
                String filepath=null;
                String[] filenames=Environment.getExternalStorageDirectory().list();
                if (null==filenames) {
                    return false;
                }

                for (String s : filenames) {
                    if (s.startsWith("mupdf_")) {
                        filepath=Environment.getExternalStorageDirectory()+File.separator+s;
                        Log.d(TAG, "restore file:"+s);
                    }
                }

                if (null==filepath) {
                    return false;
                }
                boolean flag=AKRecent.getInstance(APVApplication.getInstance()).restoreToDb(filepath);
                long newTime=System.currentTimeMillis()-now;
                if (newTime<1500l) {
                    newTime=1500l-newTime;
                } else {
                    newTime=0;
                }

                try {
                    Thread.sleep(newTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return flag;
            }

            @Override
            protected void onPostExecute(Boolean s) {
                if (null!=progressDialog) {
                    progressDialog.dismiss();
                }

                if (s) {
                    Toast.makeText(APVApplication.getInstance(), "恢复成功:"+s, Toast.LENGTH_LONG).show();
                    update();
                } else {
                    Toast.makeText(APVApplication.getInstance(), "恢复失败", Toast.LENGTH_LONG).show();
                }
            }
        }, (Void[]) null);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view=super.onCreateView(inflater, container, savedInstanceState);
        filesListView.setDivider(null);
        filesListView.setDividerHeight(0);

        this.pathTextView.setVisibility(View.GONE);
        filesListView.setOnScrollListener(this);

        return view;
    }

    private void reset() {
        curPage=-1;
        totalCount=0;
        totalPage=0;
        mSavedLastVisibleIndex=-1;
    }

    public void update() {
        if (null==fileListAdapter) {
            return;
        }
        this.fileListAdapter.setMode(AKAdapter.TYPE_RENCENT);

        reset();
        getHistory();
    }

    private void getHistory() {
        Util.execute(true, new AsyncTask<Void, Void, ArrayList<FileListEntry>>() {
            @Override
            protected ArrayList<FileListEntry> doInBackground(Void... params) {
                final long now=System.currentTimeMillis();
                AKRecent recent=AKRecent.getInstance(HistoryFragment.this.getActivity());
                int count=recent.getProgressCount();
                ArrayList<AKProgress> progresses=recent.readRecentFromDb(PAGE_SIZE*(curPage+1), PAGE_SIZE);
                //Log.d(TAG, "progresses:"+progresses);
                ArrayList<FileListEntry> entryList=new ArrayList<FileListEntry>();
                if (null!=progresses&&progresses.size()>0) {
                    FileListEntry entry;
                    File file;
                    String path=Environment.getExternalStorageDirectory().getPath();
                    for (AKProgress progress : progresses) {
                        try {
                            file=new File(path+"/"+progress.path);
                            entry=new FileListEntry(FileListEntry.RECENT, 0, file, showExtension);
                            entry.setAkProgress(progress);
                            entryList.add(entry);
                        } catch (Exception e) {
                            e.printStackTrace();
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

                long newTime=System.currentTimeMillis()-now;
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
                }

                if (entryList.size()>0) {
                    totalCount=count;
                    curPage++;
                    totalPage=count/PAGE_SIZE;
                    if (count%PAGE_SIZE>0) {
                        totalPage++;
                    }
                }
                //Log.d(TAG, String.format("totalCount:%d, curPage:%d, totalPage:%d,count:%d", totalCount, curPage, totalPage, count));

                return entryList;
            }

            @Override
            protected void onPostExecute(ArrayList<FileListEntry> entries) {
                if (null!=entries&&entries.size()>0) {
                    if (fileList==null) {
                        fileList=new ArrayList<FileListEntry>();
                    }
                    if (curPage==0) {
                        fileList.clear();
                    }
                    fileList.addAll(entries);
                    fileListAdapter.setData(fileList);
                    fileListAdapter.notifyDataSetChanged();
                    //filesListView.setSelection(0);
                }
                mSwipeRefreshWidget.setRefreshing(false);
            }
        }, (Void[]) null);
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {

    }

    public final void onScroll(final AbsListView view, final int firstVisibleItem, final int visibleItemCount, final int totalItemCount) {
        // Detect whether the last visible item has changed
        final int lastVisibleItemIndex=firstVisibleItem+visibleItemCount;

        /**
         * Check that the last item has changed, we have any items, and that
         * the last item is visible. lastVisibleItemIndex is a zero-based
         * index, so we onEvent one to it to check against totalItemCount.
         */
        if (visibleItemCount>0&&(lastVisibleItemIndex+1)==totalItemCount) {
            if (lastVisibleItemIndex!=mSavedLastVisibleIndex) {
                mSavedLastVisibleIndex=lastVisibleItemIndex;
                //mOnLastItemVisibleListener.onLastItemVisible();
                if (curPage<totalPage) {
                    mSwipeRefreshWidget.setRefreshing(true);
                    loadMore();
                } else {
                    Log.d(TAG, "curPage>=totalPage:"+curPage+" totalPage:"+totalPage);
                }
            }
        }
    }

    private void loadMore() {
        getHistory();
    }
}
