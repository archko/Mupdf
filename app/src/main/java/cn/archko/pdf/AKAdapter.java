package cn.archko.pdf;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import cn.archko.pdf.utils.Util;
import cx.hell.android.pdfviewpro.FileListEntry;

import java.util.ArrayList;

/**
 * @author: archko 2014/4/17 :15:43
 */
public class AKAdapter extends BaseAdapter {

    public static final int TYPE_FILE=0;
    public static final int TYPE_RENCENT=1;
    public static final int TYPE_SEARCH=2;

    ArrayList<FileListEntry> mData;
    Context mContext;
    int mMode=TYPE_FILE;

    public void setMode(int mMode) {
        this.mMode=mMode;
    }

    public void setData(ArrayList<FileListEntry> mData) {
        this.mData=mData;
    }

    public AKAdapter(Context context) {
        mContext=context;
        this.mData=new ArrayList<FileListEntry>();
    }

    @Override

    public int getCount() {
        return mData.size();
    }

    @Override
    public Object getItem(int position) {
        return mData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public int getItemViewType(int position) {
        return mMode;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder = null;

        int type = getItemViewType(position);
        if (convertView == null) {
            viewHolder = new ViewHolder();
            switch (type) {
                case TYPE_FILE:
                    convertView = View.inflate(mContext, R.layout.picker_entry, null);
                    break;
                case TYPE_RENCENT:
                    convertView = View.inflate(mContext, R.layout.picker_entry_history, null);
                    viewHolder.mProgressBar = (ProgressBar) convertView.findViewById(R.id.progressbar);
                    break;
                case TYPE_SEARCH:
                    convertView = View.inflate(mContext, R.layout.picker_entry_search, null);
                    viewHolder.mPath= (TextView) convertView.findViewById(R.id.fullpath);
                    break;
            }

            viewHolder.mIcon = (ImageView) convertView.findViewById(R.id.icon);
            viewHolder.mName = (TextView) convertView.findViewById(R.id.name);
            viewHolder.mSize = (TextView) convertView.findViewById(R.id.size);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        FileListEntry entry=mData.get(position);


        if (type==TYPE_RENCENT) {
            AKProgress progress=entry.getAkProgress();
            if (null!=progress) {
                viewHolder.mProgressBar.setVisibility(View.VISIBLE);
                viewHolder.mProgressBar.setMax(progress.numberOfPages);
                viewHolder.mProgressBar.setProgress(progress.page);
            } else {
                viewHolder.mProgressBar.setVisibility(View.GONE);
            }
        } else if (type == TYPE_SEARCH) {
            viewHolder.mPath.setText(entry.getFile().getAbsolutePath());
        }

        if (entry.getType()==FileListEntry.HOME) {
            viewHolder.mIcon.setImageResource(R.drawable.browser_item_folder_open);
        } else if (entry.getType()==FileListEntry.NORMAL&&entry.isDirectory()&&!entry.isUpFolder()) {
            viewHolder.mIcon.setImageResource(R.drawable.browser_item_folder_open);
        } else if (entry.isUpFolder()) {
            viewHolder.mIcon.setImageResource(R.drawable.browser_item_folder_open);
        } else {
            viewHolder.mIcon.setImageResource(R.drawable.browser_item_book);
        }

        viewHolder.mName.setText(entry.getLabel());
        viewHolder.mName.setTypeface(
            viewHolder.mName.getTypeface(),
            entry.getType()==FileListEntry.RECENT ? Typeface.ITALIC
                : Typeface.NORMAL
        );

        if (null!=entry.getFile()) {
            viewHolder.mSize.setText(Util.getFileSize(entry.getFileSize()));
        }
        return convertView;
    }

    private final class ViewHolder {

        TextView mName;
        ImageView mIcon;
        ProgressBar mProgressBar;
        TextView mSize;
        TextView mPath;
    }
}
