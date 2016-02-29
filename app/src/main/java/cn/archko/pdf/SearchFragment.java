package cn.archko.pdf;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;

import org.vudroid.pdfdroid.PdfViewerActivity;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;

import cx.hell.android.pdfviewpro.FileListEntry;

/**
 * @author: archko 2016/2/14 :15:58
 */
public class SearchFragment extends DialogFragment implements AdapterView.OnItemClickListener,
        AdapterView.OnItemLongClickListener {

    EditText editView;
    ListView filesListView;
    ImageView imgClose;
    private FileFilter fileFilter = null;
    protected AKAdapter fileListAdapter = null;
    protected ArrayList<FileListEntry> fileList = null;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int themeId = R.style.AppDialogTheme;
        /*if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            themeId = android.R.style.Theme_Material_Light_Dialog;
        }*/
        setStyle(DialogFragment.STYLE_NORMAL, themeId);
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.filesearcher, container, false);

        getDialog().setTitle(R.string.menu_search);
        editView = (EditText) view.findViewById(R.id.searchEdit);
        imgClose = (ImageView) view.findViewById(R.id.img_close);
        filesListView = (ListView) view.findViewById(R.id.files);

        imgClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clear();
            }
        });

        editView.addTextChangedListener(new ATextWatcher());

        return view;
    }

    private class ATextWatcher implements TextWatcher {

        String string;

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            string = s.toString();
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            String cs = s.toString();
            if (!TextUtils.isEmpty(cs)) {
                if (!cs.equals(string)) {
                    search(cs);
                }
            } else {
                clearList();
            }
        }
    }

    private void clearList() {
        if (fileList.size() > 0) {
            fileList.clear();
            fileListAdapter.notifyDataSetChanged();
        }
    }

    private void clear() {
        editView.setText(null);
        clearList();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final Activity activity = getActivity();
        if (null == fileListAdapter) {
            fileListAdapter = new AKAdapter(activity);
            this.fileListAdapter.setMode(AKAdapter.TYPE_SEARCH);
        }
        filesListView.setAdapter(this.fileListAdapter);
        filesListView.setOnItemClickListener(this);
        filesListView.setOnItemLongClickListener(this);
    }

    public void updateAdapter() {
    }

    public void search(String keyword) {
        if (!isResumed()) {
            return;
        }
        if (null == fileList) {
            this.fileList = new ArrayList<FileListEntry>();
        }
        fileList.clear();

        String home = getHome();
        doSearch(fileList, keyword, new File(home));

        fileListAdapter.setData(fileList);
        fileListAdapter.notifyDataSetChanged();
        //this.filesListView.setSelection(0);
    }

    private void doSearch(ArrayList<FileListEntry> fileList, String keyword, File dir) {
        if (dir.isDirectory()) {
            File files[] = dir.listFiles(this.fileFilter);

            if (files != null && files.length > 0) {
                for (File f : files) {
                    if (f.isFile()) {
                        if (f.getName().contains(keyword)) {
                            fileList.add(new FileListEntry(FileListEntry.NORMAL, -1, f, true));
                        }
                    } else {
                        doSearch(fileList, keyword, f);
                    }
                }
            }
        } else {
            if (dir.getName().contains(keyword)) {
                fileList.add(new FileListEntry(FileListEntry.NORMAL, -1, dir, true));
            }
        }
    }

    private String getHome() {
        String defaultHome = Environment.getExternalStorageDirectory().getAbsolutePath();
        String path = getActivity().getSharedPreferences(ChooseFileFragmentActivity.PREF_TAG, 0).
                getString(ChooseFileFragmentActivity.PREF_HOME, defaultHome);
        if (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 2);
        }

        File pathFile = new File(path);

        if (pathFile.exists() && pathFile.isDirectory())
            return path;
        else
            return defaultHome;
    }

    @Override
    public void onItemClick(AdapterView parent, View v, int position, long id) {
        FileListEntry clickedEntry = (FileListEntry) this.fileListAdapter.getItem(position);
        File clickedFile = clickedEntry.getFile();

        if (null == clickedFile || !clickedFile.exists())
            return;

        pdfView(clickedFile);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        FileListEntry entry = (FileListEntry) this.fileListAdapter.getItem(position);
        showFileInfoDialog(entry);
        return false;
    }

    public void pdfView(File f) {
        Log.i("", "post intent to open file " + f);
        dismiss();
        Intent intent = new Intent();
        intent.setDataAndType(Uri.fromFile(f), "application/pdf");
        //intent.setClass(getActivity(), OpenFileActivity.class);
        intent.setClass(getActivity(), PdfViewerActivity.class);
        intent.setAction("android.intent.action.VIEW");
        getActivity().startActivity(intent);
    }

    protected void showFileInfoDialog(FileListEntry entry) {
        FragmentTransaction ft=getActivity().getSupportFragmentManager().beginTransaction();
        Fragment prev=getActivity().getSupportFragmentManager().findFragmentByTag("dialog");
        if (prev!=null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        // Create and show the dialog.
        FileInfoFragment fileInfoFragment=new FileInfoFragment();
        Bundle bundle=new Bundle();
        bundle.putSerializable(FileInfoFragment.FILE_LIST_ENTRY, entry);
        fileInfoFragment.setArguments(bundle);
        fileInfoFragment.show(ft, "dialog");
    }
}
