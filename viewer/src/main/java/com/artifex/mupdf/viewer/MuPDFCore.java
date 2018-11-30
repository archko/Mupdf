package com.artifex.mupdf.viewer;

import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.RectF;

import com.artifex.mupdf.fitz.Cookie;
import com.artifex.mupdf.fitz.DisplayList;
import com.artifex.mupdf.fitz.Document;
import com.artifex.mupdf.fitz.Link;
import com.artifex.mupdf.fitz.Matrix;
import com.artifex.mupdf.fitz.Outline;
import com.artifex.mupdf.fitz.Page;
import com.artifex.mupdf.fitz.Rect;
import com.artifex.mupdf.fitz.RectI;
import com.artifex.mupdf.fitz.android.AndroidDrawDevice;

import java.util.ArrayList;

public class MuPDFCore
{
	private int resolution;
	private Document doc;
	private Outline[] outline;
	private int pageCount = -1;
	private int currentPage;
	private Page page;
	private float pageWidth;
	private float pageHeight;
	private DisplayList displayList;

	public MuPDFCore(String filename) {
		doc = Document.openDocument(filename);
		pageCount = doc.countPages();
		resolution = 160;
		currentPage = -1;
	}

	public MuPDFCore(byte buffer[], String magic) {
		doc = Document.openDocument(buffer, magic);
		pageCount = doc.countPages();
		resolution = 160;
		currentPage = -1;
	}

	public String getTitle() {
		return doc.getMetaData(Document.META_INFO_TITLE);
	}

	public int countPages() {
		return pageCount;
	}

    public synchronized void gotoPage(int pageNum) {
		/* TODO: page cache */
		if (pageNum > pageCount-1)
			pageNum = pageCount-1;
		else if (pageNum < 0)
			pageNum = 0;
		if (pageNum != currentPage) {
			currentPage = pageNum;
			if (page != null)
				page.destroy();
			page = null;
			if (displayList != null)
				displayList.destroy();
			displayList = null;
			page = doc.loadPage(pageNum);
			Rect b = page.getBounds();
			pageWidth = b.x1 - b.x0;
			pageHeight = b.y1 - b.y0;
		}
	}

	public synchronized PointF getPageSize(int pageNum) {
		gotoPage(pageNum);
		return new PointF(pageWidth, pageHeight);
	}

	public synchronized void onDestroy() {
		if (displayList != null)
			displayList.destroy();
		displayList = null;
		if (page != null)
			page.destroy();
		page = null;
		if (doc != null)
			doc.destroy();
		doc = null;
	}

	/**
	 * 渲染页面,传入一个Bitmap对象.使用硬件加速,虽然速度影响不大.
	 *
	 * @param bm     需要渲染的位图,配置为ARGB8888
	 * @param page   当前渲染页面页码
	 * @param pageW  页面的宽,由缩放级别计算得到的最后宽,由于这个宽诸页面的裁剪大小,如果不正确,得到的Tile页面是不正确的
	 * @param pageH  页面的宽,由缩放级别计算得到的最后宽,由于这个宽诸页面的裁剪大小,如果不正确,得到的Tile页面是不正确的
	 * @param patchX 裁剪的页面的左顶点
	 * @param patchY 裁剪的页面的上顶点
	 * @param patchW 页面的宽,具体渲染的页面实际大小.显示出来的大小.
	 * @param patchH 页面的高,具体渲染的页面实际大小.显示出来的大小.
	 */
	public synchronized void drawPage(Bitmap bm, int pageNum,
			int pageW, int pageH,
			int patchX, int patchY,
			int patchW, int patchH,
			Cookie cookie) {
		gotoPage(pageNum);

		if (displayList == null)
			displayList = page.toDisplayList(false);

		float zoom = resolution / 72;
		Matrix ctm = new Matrix(zoom);
		RectI bbox = new RectI(page.getBounds().transform(ctm));
		float xscale = (float)pageW / (float)(bbox.x1-bbox.x0);
		float yscale = (float)pageH / (float)(bbox.y1-bbox.y0);
		ctm.scale(xscale, yscale);

		AndroidDrawDevice dev = new AndroidDrawDevice(bm, patchX, patchY);
		displayList.run(dev, ctm, cookie);
		dev.destroy();
	}

	public synchronized void updatePage(Bitmap bm, int pageNum,
			int pageW, int pageH,
			int patchX, int patchY,
			int patchW, int patchH,
			Cookie cookie) {
		drawPage(bm, pageNum, pageW, pageH, patchX, patchY, patchW, patchH, cookie);
	}

	public synchronized Link[] getPageLinks(int pageNum) {
		gotoPage(pageNum);
		return page.getLinks();
	}

	public synchronized RectF[] searchPage(int pageNum, String text) {
		gotoPage(pageNum);
		//Rect[] rs = page.search(text);
		//RectF[] rfs = new RectF[rs.length];
		//for (int i=0; i < rs.length; ++i)
		//	rfs[i] = new RectF(rs[i].x0, rs[i].y0, rs[i].x1, rs[i].y1);
		//return rfs;
        return null;
	}

	public synchronized boolean hasOutline() {
		if (outline == null)
			outline = doc.loadOutline();
		return outline != null;
	}

	private void flattenOutlineNodes(ArrayList<OutlineActivity.Item> result, Outline list[], String indent) {
		for (Outline node : list) {
			if (node.title != null)
				result.add(new OutlineActivity.Item(indent + node.title, node.page));
			if (node.down != null)
				flattenOutlineNodes(result, node.down, indent + "    ");
		}
	}

	public synchronized ArrayList<OutlineActivity.Item> getOutline() {
		ArrayList<OutlineActivity.Item> result = new ArrayList<OutlineActivity.Item>();
		flattenOutlineNodes(result, outline, "");
		return result;
	}

	public synchronized boolean needsPassword() {
		return doc.needsPassword();
	}

	public synchronized boolean authenticatePassword(String password) {
		return doc.authenticatePassword(password);
	}
}
