package cx.hell.android.pdfviewpro;

import java.io.File;
import java.io.Serializable;

import cn.archko.pdf.entity.AKProgress;

public class FileListEntry implements Serializable, Cloneable {
    private String label = null;
    private File file = null;
    private boolean isDirectory = false;
    private int type = NORMAL;
    private int recentNumber = -1;
    public static final int NORMAL = 0;
    public static final int HOME = 1;
    public static final int RECENT = 2;
    private AKProgress mAkProgress;

    public AKProgress getAkProgress() {
        return mAkProgress;
    }

    public void setAkProgress(AKProgress mAkProgress) {
        this.mAkProgress = mAkProgress;
    }

    public FileListEntry(int type, int recentNumber, File file, String label) {
        this.file = file;
        this.label = file.getName();
        this.isDirectory = file.isDirectory();
        this.type = type;
        this.label = label;

        this.recentNumber = recentNumber;
    }

    public FileListEntry(int type, int recentNumber, File file, Boolean showPDFExtension) {
        this(type, recentNumber, file, getLabel(file, showPDFExtension));
    }

    public FileListEntry(int type, String label) {
        this.type = type;
        this.label = label;
    }

    private static String getLabel(File file, boolean showPDFExtension) {
        String label = file.getName();

        if (!showPDFExtension && label.length() > 4 && !file.isDirectory()
            /*&& label.substring(label.length()-4, label.length()).equalsIgnoreCase(".pdf")*/) {
            return label.substring(0, label.length() - 4);
        } else {
            return label;
        }
    }

    public int getRecentNumber() {
        return this.recentNumber;
    }

    public int getType() {
        return this.type;
    }

    public File getFile() {
        return this.file;
    }

    public long getFileSize() {
        if (null != mAkProgress) {
            return mAkProgress.size;
        }
        return getFile() != null ? getFile().length() : 0;
    }

    public String getLabel() {
        return this.label;
    }

    public boolean isDirectory() {
        return this.isDirectory;
    }

    public boolean isUpFolder() {
        return this.isDirectory && this.label.equals("..");
    }

    @Override
    public FileListEntry clone() {
        try {
            return (FileListEntry) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return null;
    }
}
