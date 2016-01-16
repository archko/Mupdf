package cn.archko.pdf;

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.math.BigDecimal;

import cn.archko.pdf.utils.DateUtil;
import cn.archko.pdf.utils.Util;
import cx.hell.android.pdfviewpro.FileListEntry;

/**
 * @author: archko 2016/1/16 :14:34
 */
public class FileInfoFragment extends DialogFragment {

    public static final String FILE_LIST_ENTRY = "FILE_LIST_ENTRY";
    FileListEntry mEntry;
    TextView mLocation;
    TextView mFileName;
    TextView mFileSize;
    TextView mLastModified;
    View mLastReadLayout;
    TextView mLastRead;
    ProgressBar mProgressBar;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int themeId = android.R.style.Theme_Holo_Light_Dialog;
        /*if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            themeId = android.R.style.Theme_Material_Light_Dialog;
        }*/
        setStyle(DialogFragment.STYLE_NORMAL, themeId);
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);
        if (null != args) {
            mEntry = (FileListEntry) args.getSerializable(FILE_LIST_ENTRY);
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.file_info, container, false);
        mLocation = (TextView) view.findViewById(R.id.location);
        mFileName = (TextView) view.findViewById(R.id.fileName);
        mFileSize = (TextView) view.findViewById(R.id.fileSize);
        mLastReadLayout = view.findViewById(R.id.lay_last_read);
        mLastRead = (TextView) view.findViewById(R.id.lastRead);
        mProgressBar = (ProgressBar) view.findViewById(R.id.progressbar);
        mLastModified = (TextView) view.findViewById(R.id.lastModified);
        Button button = (Button) view.findViewById(R.id.btn_ok);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FileInfoFragment.this.dismiss();
            }
        });

        getDialog().setTitle(R.string.menu_info);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (null == mEntry || mEntry.getFile() == null) {
            Toast.makeText(getActivity(), "file is null.", Toast.LENGTH_LONG).show();
            dismiss();
            return;
        }

        File file = mEntry.getFile();
        mLocation.setText(file.getPath());
        mFileName.setText(file.getName());
        mFileSize.setText(Util.getFileSize(mEntry.getFileSize()));

        AKProgress progress = mEntry.getAkProgress();
        if (null == progress) {
            mLastReadLayout.setVisibility(View.GONE);
        } else {
            mLastReadLayout.setVisibility(View.VISIBLE);
            String text = DateUtil.formatTime(progress.timestampe, DateUtil.TIME_FORMAT_TWO);
            float percent = progress.page * 100f / progress.numberOfPages;
            BigDecimal b = new BigDecimal(percent);
            text += "       " + b.setScale(2, BigDecimal.ROUND_HALF_UP).floatValue() + "%";
            mLastRead.setText(text);
            mProgressBar.setMax(progress.numberOfPages);
            mProgressBar.setProgress(progress.page);
        }
        mLastModified.setText(DateUtil.formatTime(file.lastModified(), DateUtil.TIME_FORMAT_TWO));
    }
}
