package cn.archko.pdf;

import java.io.File;
import java.io.Serializable;
import java.util.Comparator;

/**
 * @author: archko 2014/4/17 :16:27
 */
public class AKProgress implements Serializable, Comparator<AKProgress> {

    private static final long serialVersionUID = 4899452726203839409L;
    public int _id; //db id
    /**
     * 索引
     */
    public int index;
    /**
     * 文件路径
     */
    public String path;
    /**
     * 进度0-100
     */
    public int progress = -1;
    public int numberOfPages;
    public int page;
    public long size;
    public String ext;
    public long timestampe;
    /**
     * 这是新加的数据,对应BookmarkEntry.toString()这样便于保存数据.
     */
    public String bookmarkEntry;

    public AKProgress() {
    }

    public AKProgress(String path) {
        timestampe = System.currentTimeMillis();
        this.path = path;
        File file = new File(path);
        if (file.exists()) {
            size = file.length();
            ext = "";
        } else {
            size = 0;
        }
    }

    public AKProgress(int _id, int index, String path, int progress, int numberOfPages, int page, long size, String ext, long timestampe, String bookmarkEntry) {
        this._id = _id;
        this.index = index;
        this.path = path;
        this.progress = progress;
        this.numberOfPages = numberOfPages;
        this.page = page;
        this.size = size;
        this.ext = ext;
        this.timestampe = timestampe;
        this.bookmarkEntry = bookmarkEntry;
    }

    @Override
    public String toString() {
        return "AKProgress{" +
                ", _id='" + _id + '\'' +
                ", path='" + path + '\'' +
                ", numberOfPages=" + numberOfPages +
                ", page=" + page +
                ", size=" + size +
                ", ext='" + ext + '\'' +
                ", timestampe=" + timestampe +
                ", bookmarkEntry='" + bookmarkEntry + '\'' +
                '}';
    }

    @Override
    public int compare(AKProgress lhs, AKProgress rhs) {
        if (lhs.timestampe > rhs.timestampe) {    //时间大的放前面
            return -1;
        } else if (lhs.timestampe < rhs.timestampe) {
            return 1;
        }
        return 0;
    }
}
