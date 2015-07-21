package cx.hell.android.pdfviewpro;

// #ifdef pro

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

/**
 * Similar to Preferences/Options, but uses document (file) id as part of the key.
 * Implemented as database.
 * Keys can expire.
 * Each instance of this class represents a connection to database. 
 */
public class DocumentOptions {
	
	private final static String TAG = "cx.hell.android.pdfviewpro";
	
//	/**
//	 * Database holds at most this many documents.
//	 */
//	private final static int MAX_ENTRIES = 1024;
	
//	/**
//	 * Database deletes entries older than MAX_AGE_SEC seconds.
//	 */
//	private final static long MAX_AGE_SEC = 3600 * 24 * 30 * 6; // 6 months
	
	/**
	 * Map of sql statements used to create database objects.
	 */
	private final static Map<String,List<String>> CREATE_TABLE_SQL;
	static {
		Map<String,List<String>> m = new HashMap<String,List<String>>();
		ArrayList<String> l = new ArrayList<String>();
		l.add("create table document_options (\n" +
				"o_id integer, o_created timestamp not null default current_timestamp,\n" +
				"o_modified timestamp not null default current_timestamp,\n" +
				"o_doc varchar(4096) not null, o_key varchar(4096) not null, o_value varchar(4096)\n" +
				")");
		l.add("create unique index document_options_i on document_options (o_doc, o_key)");
		m.put("document_options", l);
		CREATE_TABLE_SQL = Collections.unmodifiableMap(m);
	}
	
	private final static String UPSERT_VALUE_SQL = "insert or replace into document_options\n" +
		"(o_doc, o_key, o_modified, o_created, o_value)\n" +
		"values (?, ?, current_timestamp, (select o_created from document_options where o_doc = ? and o_key = ?), ?)";
	
	/**
	 * Database access.
	 */
	private SQLiteDatabase sqliteDatabase = null;
	
	/**
	 * Compiled statement for value update (upsert) that uses insert or replace.
	 */
	private SQLiteStatement upsertValueStatement = null;
	
	/**
	 * Create new database access instance.
	 */
	public DocumentOptions(Context context) {
		this.sqliteDatabase = context.openOrCreateDatabase("document_options", Context.MODE_PRIVATE, null);
		this.setupDatabase();
		this.upsertValueStatement = this.sqliteDatabase.compileStatement(UPSERT_VALUE_SQL);
	}
	
	/**
	 * Check if table exists.
	 * @param tableName table name
	 * @return true if table tableName exists
	 */
	private boolean tableExists(String tableName) {
		int cnt = -1;
		Cursor curs = null;
		try {
			curs = this.sqliteDatabase.query("sqlite_master", new String[]{"name"}, "name = ?", new String[]{tableName}, null, null, null);
			cnt = curs.getCount();
		} finally {
			if (curs != null) curs.close();
		}
		
		return cnt == 1;
	}
	
	/**
	 * Create table by executing create table sql statements from CREATE_TABLE_SQL.
	 * @param tableName table name
	 */
	private void createTable(String tableName) {
		Log.d(TAG, "creating table " + tableName);
		List<String> table_sql_statements = DocumentOptions.CREATE_TABLE_SQL.get(tableName);
		Iterator<String> i = table_sql_statements.iterator();
		while(i.hasNext()) {
			String sql = i.next();
			Log.d(TAG, "executing sql \"" + sql + "\"");
			this.sqliteDatabase.execSQL(sql);
		}
	}
	
	/**
	 * Create or upgrade database schema.
	 */
	private synchronized void setupDatabase() {
		if (!this.tableExists("document_options")) {
			this.createTable("document_options");
		}
	}
	
	/**
	 * Save value.
	 */
	public void setValue(String documentId, String key, String value) {
		this.upsertValueStatement.clearBindings();
		/* o_doc, o_key, o_doc, o_key, o_value */
		this.upsertValueStatement.bindString(1, documentId);
		this.upsertValueStatement.bindString(2, key);
		this.upsertValueStatement.bindString(3, documentId);
		this.upsertValueStatement.bindString(4, key);
		this.upsertValueStatement.bindString(5, value);
		this.upsertValueStatement.execute();
	}
	
	public void setValues(String documentId, Map<String,String> values) {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Get all preferences for given document.
	 */
	public Map<String, String> getValues(String documentId) {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Get one document preference value for given documentId and given key.
	 */
	public String getValue(String documentId, String key) {
		Cursor c = this.sqliteDatabase.query("document_options", new String[]{"o_value"}, "o_doc = ? and o_key = ?", new String[]{documentId, key}, null, null, null);
		try {
			int cnt = c.getCount();
			if (cnt == 0) return null;
			c.moveToFirst();
			String value = c.getString(0);
			return value;
		} finally {
			c.close();
		}
	}
	
	/**
	 * Close underlying database.
	 */
	public void close() {
		if (this.upsertValueStatement != null) {
			this.upsertValueStatement.close();
			this.upsertValueStatement = null;
		}
		if (this.sqliteDatabase != null) {
			this.sqliteDatabase.close();
			this.sqliteDatabase = null;
		}
	}
}

// #endif