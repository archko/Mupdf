package cx.hell.android.pdfviewpro;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import cn.me.archko.pdf.AKProgress;

import java.util.ArrayList;

/**
 * @author archko
 */
public class RecentManager {

    public static class ProgressTbl implements BaseColumns {

        public static final String TABLE_NAME="progress";
        public static final String KEY_INDEX="pindex";
        public static final String KEY_PATH="path";
        public static final String KEY_PROGRESS="progress";
        public static final String KEY_NUMBEROFPAGES="numberOfPages";
        public static final String KEY_PAGE="page";
        public static final String KEY_SIZE="size";
        public static final String KEY_EXT="ext";
        public static final String KEY_TIMESTAMP="timestamp";
        public static final String KEY_BOOKMARKENTRY="bookmarkEntry";
    }

    private static final int DB_VERSION=1;
    private static final String DB_NAME="recent.db";

    private static final String DATABASE_CREATE="create table "
        +ProgressTbl.TABLE_NAME
        +"("+ProgressTbl._ID+" integer primary key autoincrement,"
        +""+ProgressTbl.KEY_INDEX+" integer,"
        +""+ProgressTbl.KEY_PATH+" text ,"
        +""+ProgressTbl.KEY_PROGRESS+" integer ,"
        +""+ProgressTbl.KEY_NUMBEROFPAGES+" integer ,"
        +""+ProgressTbl.KEY_PAGE+" integer ,"
        +""+ProgressTbl.KEY_SIZE+" integer ,"
        +""+ProgressTbl.KEY_EXT+" text ,"
        +""+ProgressTbl.KEY_TIMESTAMP+" integer ,"
        +""+ProgressTbl.KEY_BOOKMARKENTRY+" text"
        +");";

    private final Context context;

    private DatabaseHelper DBHelper;
    private SQLiteDatabase db;

    public RecentManager(Context ctx) {
        this.context=ctx;
        DBHelper=new DatabaseHelper(context);
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DB_NAME, null, DB_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(DATABASE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        }
    }

    public RecentManager open() throws SQLException {
        db=DBHelper.getWritableDatabase();
        return this;
    }

    public SQLiteDatabase getDb() {
        return db;
    }

    public void close() {
        DBHelper.close();
    }

    public long setProgress(AKProgress progress) {
        ContentValues cv=new ContentValues();
        cv.put(ProgressTbl.KEY_INDEX, progress.index);
        cv.put(ProgressTbl.KEY_PATH, progress.path);
        cv.put(ProgressTbl.KEY_PROGRESS, progress.progress);
        cv.put(ProgressTbl.KEY_NUMBEROFPAGES, progress.numberOfPages);
        cv.put(ProgressTbl.KEY_PAGE, progress.page);
        cv.put(ProgressTbl.KEY_SIZE, progress.size);
        cv.put(ProgressTbl.KEY_EXT, progress.ext);
        cv.put(ProgressTbl.KEY_TIMESTAMP, progress.timestampe);
        cv.put(ProgressTbl.KEY_BOOKMARKENTRY, progress.bookmarkEntry);
        long count=0;
        if (progress._id>0) {
            count=db.update(ProgressTbl.TABLE_NAME, cv, ProgressTbl._ID+"='"+progress._id+"'", null);
        }
        if (count==0) {
            return db.insert(ProgressTbl.TABLE_NAME, null, cv);
        } else {
            return count;
        }
    }

    public AKProgress getProgress(String path) {
        AKProgress entry=null;

        Cursor cur=db.query(true, ProgressTbl.TABLE_NAME, null,
            ProgressTbl.KEY_PATH+"='"+path+"'", null,
            null, null, null, "1");

        if (cur!=null) {
            if (cur.moveToFirst()) {
                entry=fillProgress(cur);
            }
            cur.close();
        }
        return entry;
    }

    private AKProgress fillProgress(Cursor cur) {
        AKProgress entry;
        int _id=cur.getInt(cur.getColumnIndex(ProgressTbl._ID));
        int index=cur.getInt(cur.getColumnIndex(ProgressTbl.KEY_INDEX));
        String path=cur.getString(cur.getColumnIndex(ProgressTbl.KEY_PATH));
        int progress=cur.getInt(cur.getColumnIndex(ProgressTbl.KEY_PROGRESS));
        int numberOfPages=cur.getInt(cur.getColumnIndex(ProgressTbl.KEY_NUMBEROFPAGES));
        int page=cur.getInt(cur.getColumnIndex(ProgressTbl.KEY_PAGE));
        int size=cur.getInt(cur.getColumnIndex(ProgressTbl.KEY_SIZE));
        String ext=cur.getString(cur.getColumnIndex(ProgressTbl.KEY_EXT));
        long timestamp=cur.getLong(cur.getColumnIndex(ProgressTbl.KEY_TIMESTAMP));
        String bookmarkEntry=cur.getString(cur.getColumnIndex(ProgressTbl.KEY_BOOKMARKENTRY));
        entry=new AKProgress(_id, index, path, progress, numberOfPages, page, size, ext, timestamp, bookmarkEntry);
        return entry;
    }

    public ArrayList<AKProgress> getProgresses() {
        ArrayList<AKProgress> list=null;

        Cursor cur=db.query(ProgressTbl.TABLE_NAME, null,
            null, null, null, null, ProgressTbl.KEY_TIMESTAMP+" desc");
        if (cur!=null) {
            list=new ArrayList<AKProgress>();
            if (cur.moveToFirst()) {
                do {
                    list.add(fillProgress(cur));
                } while (cur.moveToNext());
            }
            cur.close();
        }

        //Collections.sort(list);

        return list;
    }

    public void deleteProgress(String path) {
        db.delete(ProgressTbl.TABLE_NAME, ProgressTbl.KEY_PATH+"='"+path+"'", null);
    }
}
