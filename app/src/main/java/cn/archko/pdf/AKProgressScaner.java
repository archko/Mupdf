package cn.archko.pdf;

import android.content.Context;
import android.os.AsyncTask;

import java.util.ArrayList;
import java.util.List;

import cn.archko.pdf.utils.Util;
import cx.hell.android.pdfviewpro.FileListEntry;

/**
 * @author: wushuyong 2018/2/24 :14:58
 */
public class AKProgressScaner {

    private AsyncTask<Void, Void, List<FileListEntry>> mAsyncTask;

    public void startScan(List<FileListEntry> fileListEntries, Context activity, String currentPath, DataListener dataListener) {
        if (null != mAsyncTask) {
            mAsyncTask.cancel(true);
        }

        mAsyncTask = new AsyncTask<Void, Void, List<FileListEntry>>() {
            @Override
            protected List<FileListEntry> doInBackground(Void... voids) {
                List<FileListEntry> entries = new ArrayList<>();
                AKRecent recent = AKRecent.getInstance(activity);
                for (FileListEntry entry : fileListEntries) {
                    FileListEntry listEntry = entry.clone();
                    if (null != listEntry) {
                        entries.add(listEntry);
                        if (!listEntry.isDirectory() && entry.getFile() != null) {
                            if (listEntry.getAkProgress() == null) {
                                AKProgress progress = recent.readRecentFromDb(entry.getFile().getPath());
                                if (null != progress) {
                                    listEntry.setAkProgress(progress);
                                }
                            }
                        }
                    }
                }
                return entries;
            }

            @Override
            protected void onPostExecute(List<FileListEntry> listEntries) {
                if (null != dataListener && listEntries.size() > 0) {
                    dataListener.onSuccess(currentPath, listEntries);
                }
            }
        };
        Util.execute(true, mAsyncTask);
    }
}
