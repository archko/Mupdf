package com.artifex.mupdfdemo;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.text.Html;
import android.text.Spanned;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import cn.archko.pdf.R;
import cn.archko.pdf.utils.Util;

public class MuPDFReflowAdapter extends BaseAdapter {
	private final Context mContext;
	private final MuPDFCore mCore;

	public MuPDFReflowAdapter(Context c, MuPDFCore core) {
		mContext = c;
		mCore = core;
	}

	public int getCount() {
		return mCore.countPages();
	}

	public Object getItem(int arg0) {
		return null;
	}

	public long getItemId(int arg0) {
		return 0;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		/*final MuPDFReflowView reflowView;
		if (convertView == null) {
			reflowView = new MuPDFReflowView(mContext, mCore, new Point(parent.getWidth(), parent.getHeight()));
		} else {
			reflowView = (MuPDFReflowView) convertView;
		}

		reflowView.setPage(position, new PointF());*/
		PDFTextView reflowView = null;
		if (convertView == null) {
			reflowView = new PDFTextView(mContext, this);
		} else {
			reflowView = (PDFTextView) convertView;
		}

		byte[] result=mCore.text(position);

		String text =new String(result);
        Spanned spanned=Html.fromHtml(text);
		reflowView.setText(spanned);

		return reflowView;
	}

	private static class PDFTextView extends TextView implements MuPDFView {

		Paint mPaint;
		float mTextSize=16;
		float mScale=1.0f;
		MuPDFReflowAdapter mAdapter;

		public PDFTextView(Context context, MuPDFReflowAdapter adapter) {
			super(context);
			mAdapter=adapter;
			mPaint = getPaint();
			mTextSize = mPaint.getTextSize();
			mPaint.setTextSize(mTextSize * 1.3f);
			//setTextSize(getTextSize()*1.1f);
			setPadding(20, 20, 20, 20);
			setLineSpacing(0, 1.3f);
			setTextColor(context.getResources().getColor(R.color.text_reflow_color));
			setBackgroundColor(context.getResources().getColor(R.color.text_reflow_bg_color));
			setWidth(Util.getScreenWidthPixelWithOrientation(context));
			setTextIsSelectable(true);
		}

		@Override
		public void setPage(int page, PointF size) {

		}

		@Override
		public void setScale(float scale) {
			if (scale != mScale) {
				if (scale > 2f) {
					scale = 2f;
				}
				if (scale < 0.5f) {
					scale = 0.5f;
				}
				mScale = scale;
				float textSize = mTextSize * scale;
				mPaint.setTextSize(textSize);
				setText(getText());
				//invalidate();
			}
		}

		@Override
		public int getPage() {
			return 0;
		}

		@Override
		public void blank(int page) {

		}

		@Override
		public Hit passClickEvent(float x, float y) {
			return Hit.Nothing;
		}

		@Override
		public LinkInfo hitLink(float x, float y) {
			return null;
		}

		@Override
		public void selectText(float x0, float y0, float x1, float y1) {

		}

		@Override
		public void deselectText() {

		}

		@Override
		public boolean copySelection() {
			return false;
		}

		@Override
		public boolean markupSelection(Annotation.Type type) {
			return false;
		}

		@Override
		public void deleteSelectedAnnotation() {

		}

		@Override
		public void setSearchBoxes(RectF[] searchBoxes) {

		}

		@Override
		public void setLinkHighlighting(boolean f) {

		}

		@Override
		public void deselectAnnotation() {

		}

		@Override
		public void startDraw(float x, float y) {

		}

		@Override
		public void continueDraw(float x, float y) {

		}

		@Override
		public void cancelDraw() {

		}

		@Override
		public boolean saveDraw() {
			return false;
		}

		@Override
		public void setChangeReporter(Runnable reporter) {

		}

		@Override
		public void update() {

		}

		@Override
		public void updateHq(boolean update) {

		}

		@Override
		public void removeHq() {

		}

		@Override
		public void releaseResources() {

		}

		@Override
		public void releaseBitmaps() {

		}

		@Override
		public boolean onTouchEvent(MotionEvent ev) {
			return false;
		}
	}
}
