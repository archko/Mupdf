package com.artifex.mupdfdemo;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.Log;
import cx.hell.android.pdfviewpro.APVApplication;

public class MuPDFCore
{
	/* load our native library */
	static {
		System.loadLibrary("mupdf");
	}

	/* Readable members */
	private int numPages = -1;
	private float pageWidth;
	private float pageHeight;
	private long globals;
	private byte fileBuffer[];
	private String file_format;

	/* The native functions */
	private native long openFile(String filename);
	private native long openBuffer();
	private native String fileFormatInternal();
	private native int countPagesInternal();
	private native void gotoPageInternal(int localActionPageNum);
    private native void gotoPageInternal2(int localActionPageNum);
	private native float getPageWidth();
	private native float getPageHeight();
	private native void drawPage(Bitmap bitmap,
			int pageW, int pageH,
			int patchX, int patchY,
			int patchW, int patchH);
    private native void drawPage3(Bitmap bitmap,
        int pageW, int pageH,
        int patchX, int patchY,
        int patchW, int patchH);
	private native void updatePageInternal(Bitmap bitmap,
			int page,
			int pageW, int pageH,
			int patchX, int patchY,
			int patchW, int patchH);
	private native RectF[] searchPage(String text);
	private native TextChar[][][][] text();
	private native byte[] textAsHtml();
	private native void addMarkupAnnotationInternal(PointF[] quadPoints, int type);
	private native void addInkAnnotationInternal(PointF[][] arcs);
	private native void deleteAnnotationInternal(int annot_index);
	private native int passClickEventInternal(int page, float x, float y);
	private native void setFocusedWidgetChoiceSelectedInternal(String [] selected);
	private native String [] getFocusedWidgetChoiceSelected();
	private native String [] getFocusedWidgetChoiceOptions();
	private native int getFocusedWidgetSignatureState();
	private native String checkFocusedSignatureInternal();
	private native boolean signFocusedSignatureInternal(String keyFile, String password);
	private native int setFocusedWidgetTextInternal(String text);
	private native String getFocusedWidgetTextInternal();
	private native int getFocusedWidgetTypeInternal();
	private native LinkInfo [] getPageLinksInternal(int page);
	private native RectF[] getWidgetAreasInternal(int page);
	private native Annotation[] getAnnotationsInternal(int page);
	private native OutlineItem [] getOutlineInternal();
	private native boolean hasOutlineInternal();
	private native boolean needsPasswordInternal();
	private native boolean authenticatePasswordInternal(String password);
	private native MuPDFAlertInternal waitForAlertInternal();
	private native void replyToAlertInternal(MuPDFAlertInternal alert);
	private native void startAlertsInternal();
	private native void stopAlertsInternal();
	private native void destroying();
	private native boolean hasChangesInternal();
	private native void saveInternal();

	public static native boolean javascriptSupported();

	public MuPDFCore(Context context, String filename) throws Exception
	{
		globals = openFile(filename);
		if (globals == 0)
		{
			throw new Exception(String.format(context.getString(R.string.cannot_open_file_Path), filename));
		}
		file_format = fileFormatInternal();
	}

	public MuPDFCore(Context context, byte buffer[]) throws Exception
	{
		fileBuffer = buffer;
		globals = openBuffer();
		if (globals == 0)
		{
			throw new Exception(context.getString(R.string.cannot_open_buffer));
		}
		file_format = fileFormatInternal();
	}

	public  int countPages()
	{
		if (numPages < 0)
			numPages = countPagesSynchronized();

		return numPages;
	}

	public String fileFormat()
	{
		return file_format;
	}

	private synchronized int countPagesSynchronized() {
		return countPagesInternal();
	}

	/* Shim function */
	private void gotoPage(int page)
	{
		if (page > numPages-1)
			page = numPages-1;
		else if (page < 0)
			page = 0;
		gotoPageInternal(page);
		this.pageWidth = getPageWidth();
		this.pageHeight = getPageHeight();
	}

	public synchronized PointF getPageSize(int page) {
		gotoPage(page);
		return new PointF(pageWidth, pageHeight);
	}

    public static native void getMediaBox(int handle, float[] mediabox);

    public RectF getMediaBox(int pageHandle) {
        System.out.println("getMediaBox1:"+pageHandle);
        float[] box=new float[4];
        getMediaBox(pageHandle, box);
        RectF rectF=new RectF(box[0], box[1], box[2], box[3]);
        System.out.println("getMediaBox2:"+rectF);
        return rectF;
    }

    public synchronized PointF getPageSize2(int page) {
        if (page > numPages-1)
            page = numPages-1;
        else if (page < 0)
            page = 0;
        gotoPageInternal2(page);
        this.pageWidth = getPageWidth();
        this.pageHeight = getPageHeight();
        return new PointF(pageWidth, pageHeight);
    }

	public MuPDFAlert waitForAlert() {
		MuPDFAlertInternal alert = waitForAlertInternal();
		return alert != null ? alert.toAlert() : null;
	}

	public void replyToAlert(MuPDFAlert alert) {
		replyToAlertInternal(new MuPDFAlertInternal(alert));
	}

	public void stopAlerts() {
		stopAlertsInternal();
	}

	public void startAlerts() {
		startAlertsInternal();
	}

	public synchronized void onDestroy() {
		destroying();
		globals = 0;
	}

    public synchronized void drawPage(Bitmap bm, int page,
        int pageW, int pageH,
        int patchX, int patchY,
        int patchW, int patchH) {
        gotoPage(page);
        drawPage(bm, pageW, pageH, patchX, patchY, patchW, patchH);
    }

    /**
     * 渲染页面,传入一个Bitmap对象.使用硬件加速,虽然速度影响不大.
     * @param bm 		需要渲染的位图,配置为ARGB8888
     * @param page		当前渲染页面页码
     * @param pageW		页面的宽,由缩放级别计算得到的最后宽,由于这个宽诸页面的裁剪大小,如果不正确,得到的Tile页面是不正确的
     * @param pageH		页面的宽,由缩放级别计算得到的最后宽,由于这个宽诸页面的裁剪大小,如果不正确,得到的Tile页面是不正确的
     * @param patchX	裁剪的页面的左顶点
     * @param patchY	裁剪的页面的上顶点
     * @param patchW	页面的宽,具体渲染的页面实际大小.显示出来的大小.
     * @param patchH	页面的高,具体渲染的页面实际大小.显示出来的大小.
     */
    public synchronized void drawPage3(Bitmap bm, int page,
        int pageW, int pageH,
        int patchX, int patchY,
        int patchW, int patchH) {
        //getMediaBox(page);
        gotoPage(page);
        /*System.out.println(String.format("drawPage3 pageW-:%d, pageH-:%d, patchX:%d, patchY:%d, patchW:%d, patchH:%d",
            pageW, pageH, patchX, patchY, patchW, patchH));*/
        drawPage3(bm, pageW, pageH, patchX, patchY, patchW, patchH);
    }

    /*public synchronized void drawPage(Bitmap bm,
        int n, int zoom, int left, int top,
        int rotation, boolean skipImages, int width, int height) {
        gotoPage(n);
        //drawPage2(bm, n, zoom, left, top, rotation, skipImages, width, height);
    }*/

	public synchronized void updatePage(Bitmap bm, int page,
			int pageW, int pageH,
			int patchX, int patchY,
			int patchW, int patchH) {
		updatePageInternal(bm, page, pageW, pageH, patchX, patchY, patchW, patchH);
	}

	public synchronized PassClickResult passClickEvent(int page, float x, float y) {
		boolean changed = passClickEventInternal(page, x, y) != 0;

		switch (WidgetType.values()[getFocusedWidgetTypeInternal()])
		{
		case TEXT:
			return new PassClickResultText(changed, getFocusedWidgetTextInternal());
		case LISTBOX:
		case COMBOBOX:
			return new PassClickResultChoice(changed, getFocusedWidgetChoiceOptions(), getFocusedWidgetChoiceSelected());
		case SIGNATURE:
			return new PassClickResultSignature(changed, getFocusedWidgetSignatureState());
		default:
			return new PassClickResult(changed);
		}

	}

	public synchronized boolean setFocusedWidgetText(int page, String text) {
		boolean success;
		gotoPage(page);
		success = setFocusedWidgetTextInternal(text) != 0 ? true : false;

		return success;
	}

	public synchronized void setFocusedWidgetChoiceSelected(String [] selected) {
		setFocusedWidgetChoiceSelectedInternal(selected);
	}

	public synchronized String checkFocusedSignature() {
		return checkFocusedSignatureInternal();
	}

	public synchronized boolean signFocusedSignature(String keyFile, String password) {
		return signFocusedSignatureInternal(keyFile, password);
	}

	public synchronized LinkInfo [] getPageLinks(int page) {
		return getPageLinksInternal(page);
	}

	public synchronized RectF [] getWidgetAreas(int page) {
		return getWidgetAreasInternal(page);
	}

	public synchronized Annotation [] getAnnoations(int page) {
		return getAnnotationsInternal(page);
	}

	public synchronized RectF [] searchPage(int page, String text) {
		gotoPage(page);
		return searchPage(text);
	}

	public synchronized byte[] html(int page) {
		gotoPage(page);
		return textAsHtml();
	}

	public synchronized TextWord [][] textLines(int page) {
		gotoPage(page);
		TextChar[][][][] chars = text();

		// The text of the page held in a hierarchy (blocks, lines, spans).
		// Currently we don't need to distinguish the blocks level or
		// the spans, and we need to collect the text into words.
		ArrayList<TextWord[]> lns = new ArrayList<TextWord[]>();

		for (TextChar[][][] bl: chars) {
			if (bl == null)
				continue;
			for (TextChar[][] ln: bl) {
				ArrayList<TextWord> wds = new ArrayList<TextWord>();
				TextWord wd = new TextWord();

				for (TextChar[] sp: ln) {
					for (TextChar tc: sp) {
						if (tc.c != ' ') {
							wd.Add(tc);
						} else if (wd.w.length() > 0) {
							wds.add(wd);
							wd = new TextWord();
						}
					}
				}

				if (wd.w.length() > 0)
					wds.add(wd);

				if (wds.size() > 0)
					lns.add(wds.toArray(new TextWord[wds.size()]));
			}
		}

		return lns.toArray(new TextWord[lns.size()][]);
	}

	public synchronized void addMarkupAnnotation(int page, PointF[] quadPoints, Annotation.Type type) {
		gotoPage(page);
		addMarkupAnnotationInternal(quadPoints, type.ordinal());
	}

	public synchronized void addInkAnnotation(int page, PointF[][] arcs) {
		gotoPage(page);
		addInkAnnotationInternal(arcs);
	}

	public synchronized void deleteAnnotation(int page, int annot_index) {
		gotoPage(page);
		deleteAnnotationInternal(annot_index);
	}

	public synchronized boolean hasOutline() {
		return hasOutlineInternal();
	}

	public synchronized OutlineItem [] getOutline() {
		return getOutlineInternal();
	}

	public synchronized boolean needsPassword() {
		return needsPasswordInternal();
	}

	public synchronized boolean authenticatePassword(String password) {
		return authenticatePasswordInternal(password);
	}

	public synchronized boolean hasChanges() {
		return hasChangesInternal();
	}

	public synchronized void save() {
		saveInternal();
	}
	
	public static class Size implements Cloneable {
		public int width;
		public int height;
		
		public Size() {
			this.width = 0;
			this.height = 0;
		}
		
		public Size(int width, int height) {
			this.width = width;
			this.height = height;
		}
		
		public Size clone() {
			return new Size(this.width, this.height);
		}
	}

    //==========================================================================
    public float getPDFPageWidth(){
        return pageWidth;
    }

    public float getPDFPageHeight(){
        return pageHeight;
    }



    private static Map<String,String> fontNameToFile = null;

    static {
        /* there's also DroidSansFallback, but it's too big and we're handling it specially */
        HashMap<String,String> m = new HashMap<String,String>();

        m.put("Courier", "NimbusMonL-Regu.cff");
        m.put("Courier-Bold", "NimbusMonL-Bold.cff");
        m.put("Courier-Oblique", "NimbusMonL-ReguObli.cff");
        m.put("Courier-BoldOblique", "NimbusMonL-BoldObli.cff");

        m.put("Helvetica", "NimbusSanL-Regu.cff");
        m.put("Helvetica-Bold", "NimbusSanL-Bold.cff");
        m.put("Helvetica-Oblique", "NimbusSanL-ReguItal.cff");
        m.put("Helvetica-BoldOblique", "NimbusSanL-BoldItal.cff");

        m.put("Times-Roman", "NimbusRomNo9L-Regu.cff");
        m.put("Times-Bold", "NimbusRomNo9L-Medi.cff");
        m.put("Times-Italic", "NimbusRomNo9L-ReguItal.cff");
        m.put("Times-BoldItalic", "NimbusRomNo9L-MediItal.cff");

        m.put("Symbol", "StandardSymL.cff");
        m.put("ZapfDingbats", "Dingbats.cff");
        m.put("DroidSans", "droid/DroidSans.ttf");
        m.put("DroidSansMono", "droid/DroidSansMono.ttf");
        MuPDFCore.fontNameToFile = m;
    }

    private final static String TAG = "mupdf";

    public static byte[] getFontData(String name) {
        Log.d(TAG, "getFontData:"+name);

        if (name == null) throw new IllegalArgumentException("name can't be null");
        if (name.equals("")) throw new IllegalArgumentException("name can't be empty");
        if (name.equals("DroidSansFallback")) return MuPDFCore.getDroidSansFallbackData();
        String assetFontName = null;
        if (MuPDFCore.fontNameToFile.containsKey(name)) {
            assetFontName = MuPDFCore.fontNameToFile.get(name);
        } else {
            Log.w(TAG, "font name \""+name+"\" not found in file name mapping");
            assetFontName = name;
        }
        Log.i(TAG, "trying to load font data " + name + " from " + assetFontName);
        return MuPDFCore.getAssetBytes("font/" + assetFontName);
    }

    /**
     * TODO: mmap, because theoretically it might be almost free.
     */
    public static byte[] getDroidSansFallbackData() {
        try {
            InputStream i = new FileInputStream("/system/fonts/DroidSansFallback.ttf");
            ByteArrayOutputStream bytes = new ByteArrayOutputStream(Math.max(i.available(), 1024));
            byte tmp[] = new byte[256 * 1024];
            int read = 0;
            while(true) {
                read = i.read(tmp);
                if (read == -1) {
                    break;
                } else {
                    bytes.write(tmp, 0, read);
                }
            }
            byte d[] = bytes.toByteArray();
            Log.i(TAG, "loaded " + d.length + " bytes for DroidSansFallback.ttf");
            return d;
        } catch (IOException e) {
            Log.e(TAG, "got exception while trying to load DroidSansFallback.ttf: " + e);
            return null;
        }
    }

    /**
     * Get cmap as bytes.
     */
    public static byte[] getCmapData(String name) {
        String cmapPath = "cmap/" + name;
        Log.d(TAG, "getCmapData:"+cmapPath);

        byte[] d =  getAssetBytes(cmapPath);
        Log.d(TAG, "loaded cmap " + name + " (size: " + d.length + ")");
        return d;
    }

    public static byte[] getAssetBytes(String path) {
        AssetManager assets = APVApplication.getInstance().getAssets();
        try {
            InputStream i = assets.open(path, AssetManager.ACCESS_BUFFER);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream(Math.max(i.available(), 1024));
            byte tmp[] = new byte[256 * 1024];
            int read = 0;
            while(true) {
                read = i.read(tmp);
                if (read == -1) {
                    break;
                } else {
                    bytes.write(tmp, 0, read);
                }
            }
            return bytes.toByteArray();
        } catch (IOException e) {
            Log.e(TAG, "failed to read asset \"" + path + "\": " + e);
            return null;
        }
    }
}
