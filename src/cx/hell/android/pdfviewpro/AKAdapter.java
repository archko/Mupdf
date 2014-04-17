package cx.hell.android.pdfviewpro;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import cn.me.archko.pdf.AKProgress;
import cn.me.archko.pdf.AKRecent;
import cn.me.archko.pdf.Util;
import com.artifex.mupdfdemo.R;

import java.util.ArrayList;

/**
 * @author: archko 2014/4/17 :15:43
 */
public class AKAdapter extends BaseAdapter {

    public static final int TYPE_FILE=0;
    public static final int TYPE_RENCENT=1;

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
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;

        if (convertView==null) {
            convertView=View.inflate(mContext, R.layout.picker_entry, null);
            viewHolder=new ViewHolder();
            convertView.setTag(viewHolder);
        } else {
            viewHolder=(ViewHolder) convertView.getTag();
        }

        FileListEntry entry=mData.get(position);

        viewHolder.mIcon=(ImageView) convertView.findViewById(R.id.icon);
        viewHolder.mName=(TextView) convertView.findViewById(R.id.name);
        viewHolder.mProgressBar=(ProgressBar) convertView.findViewById(R.id.progressbar);
        viewHolder.mSize=(TextView) convertView.findViewById(R.id.size);

        if (mMode==TYPE_FILE) {
            viewHolder.mProgressBar.setVisibility(View.GONE);
        } else {
            AKProgress progress=entry.getAkProgress();
            if (null!=progress) {
                viewHolder.mProgressBar.setVisibility(View.VISIBLE);
                viewHolder.mProgressBar.setMax(progress.numberOfPages);
                viewHolder.mProgressBar.setProgress(progress.page);
            } else {
                viewHolder.mProgressBar.setVisibility(View.GONE);
            }
        }

        if (entry.getType()==FileListEntry.HOME) {
            viewHolder.mIcon.setImageResource(R.drawable.ic_dir);
        } else if (entry.getType()==FileListEntry.NORMAL&&entry.isDirectory()&&!entry.isUpFolder()) {
            viewHolder.mIcon.setImageResource(R.drawable.ic_dir);
        } else if (entry.isUpFolder()) {
            viewHolder.mIcon.setImageResource(R.drawable.ic_arrow_up);
        } else {
            viewHolder.mIcon.setImageResource(R.drawable.ic_doc);
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
    }
}
