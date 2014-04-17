package cn.me.archko.pdf;

import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import cx.hell.android.pdfviewpro.APVApplication;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;

/**
 * 存储最近阅读的记录
 *
 * @author: archko 2014/4/17 :15:05
 */
public class AKRecent implements Serializable {

    public static final String TAG="AKRecent";
    private static final long serialVersionUID=4899452726203839401L;
    private final String FILE_RECENT="AKRecent";

    ArrayList<AKProgress> mAkProgresses;
    Context mContext;
    private static AKRecent sInstance;

    public ArrayList<AKProgress> getAkProgresses() {
        return mAkProgresses;
    }

    public static AKRecent getInstance(Context context) {
        if (null==sInstance) {
            sInstance=new AKRecent(context);
        }
        return sInstance;
    }

    private AKRecent(Context context) {
        mContext=context;
    }

    public void addAsync(final String path, final int page, final int numberOfPage) {
        Util.execute(false, new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                add(path, page, numberOfPage);
                return null;
            }
        }, (Void[]) null);
    }

    public ArrayList<AKProgress> add(final String path, final int page, final int numberOfPage) {
        if (TextUtils.isEmpty(path)) {
            Log.d("", "path is null.");
            return mAkProgresses;
        }

        if (null==mAkProgresses) {
            mAkProgresses=new ArrayList<AKProgress>();
            readRecent();
        }

        if (mAkProgresses.size()>0) {
            AKProgress tmp=null;
            for (AKProgress progress : mAkProgresses) {
                if (progress.path.equals(path)) {
                    tmp=progress;
                    break;
                }
            }

            Log.d(TAG, "add :"+" progress:"+tmp);
            if (tmp!=null) {
                mAkProgresses.remove(tmp);
                tmp.timestampe=System.currentTimeMillis();
                tmp.page=page;
                tmp.numberOfPages=numberOfPage;
                mAkProgresses.add(0, tmp);
                String filepath=mContext.getFilesDir().getPath()+File.separator+FILE_RECENT;
                Util.serializeObject(mAkProgresses, filepath);
            } else {
                addNewRecent(path, page, numberOfPage);
            }
        } else {
            addNewRecent(path, page, numberOfPage);
        }
        return mAkProgresses;

    }

    private void addNewRecent(String path, int page, int numberOfPage) {
        Log.d(TAG, "addNewRecent:"+page+" nop:"+numberOfPage+" path:"+path);
        AKProgress tmp=new AKProgress(path);
        tmp.page=page;
        tmp.numberOfPages=numberOfPage;
        mAkProgresses.add(tmp);
        String filepath=mContext.getFilesDir().getPath()+File.separator+FILE_RECENT;
        Util.serializeObject(mAkProgresses, filepath);
    }

    public ArrayList<AKProgress> remove(final String path) {
        Log.d(TAG, "remove:"+path);
        if (TextUtils.isEmpty(path)) {
            Log.d("", "path is null.");
            return null;
        }

        if (null==mAkProgresses) {
            mAkProgresses=new ArrayList<AKProgress>();
            readRecent();
        }

        if (mAkProgresses.size()>0) {
            AKProgress tmp=null;
            for (AKProgress progress : mAkProgresses) {
                if (progress.path.equals(path)) {
                    tmp=progress;
                    break;
                }
            }

            Log.d(TAG, "remove :"+" progress:"+tmp);
            if (tmp!=null) {
                mAkProgresses.remove(tmp);
                String filepath=mContext.getFilesDir().getPath()+File.separator+FILE_RECENT;
                Util.serializeObject(mAkProgresses, filepath);
            }
        }
        return null;
    }

    public void readRecent() {
        if (null==mAkProgresses) {
            mAkProgresses=new ArrayList<AKProgress>();
        }
        String path=mContext.getFilesDir().getPath()+File.separator+FILE_RECENT;
        ArrayList<AKProgress> progresses=(ArrayList<AKProgress>) Util.deserializeObject(path);
        if (null!=progresses) {
            mAkProgresses.clear();
            mAkProgresses.addAll(progresses);
            Log.d(TAG, "read path:"+path+" size:"+mAkProgresses.size());
        }
    }
}
