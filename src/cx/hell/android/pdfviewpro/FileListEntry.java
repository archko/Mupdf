package cx.hell.android.pdfviewpro;

import java.io.File;

public class FileListEntry {
	private String label = null;
	private  File file = null;
	private boolean isDirectory = false;
	private int type = NORMAL;
	private int recentNumber = -1;
	static final int NORMAL = 0; 
	static final int HOME   = 1;
	static final int RECENT = 2;
	
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
		
		if (!showPDFExtension && label.length() > 4 && ! file.isDirectory() &&
			label.substring(label.length()-4, label.length()).equalsIgnoreCase(".pdf")) {
			return label.substring(0, label.length()-4);
		}
		else {
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
	
	public String getLabel() {
		return this.label;
	}
	
	public boolean isDirectory() {
		return this.isDirectory;
	}
	
	public boolean isUpFolder() {
		return this.isDirectory && this.label.equals("..");
	}
}
