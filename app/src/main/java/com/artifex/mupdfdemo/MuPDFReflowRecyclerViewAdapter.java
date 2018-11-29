package com.artifex.mupdfdemo;

import android.content.Context;
import android.graphics.Paint;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.Spanned;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.TextView;

import com.artifex.mupdf.fitz.Document;

import cn.archko.pdf.R;
import cn.archko.pdf.ScrollPositionListener;

/**
 * @author: archko 2016/5/13 :11:03
 */
public class MuPDFReflowRecyclerViewAdapter extends RecyclerView.Adapter {
    private final Context mContext;
    private final Document mCore;

    private String TXT_PATTERN = "</?(html|head|body|span|div|p)[^>]*>|(<style>[^<]*</style>)";

    private ScrollPositionListener scrollPositionListener;

    public MuPDFReflowRecyclerViewAdapter(Context c, Document core, ScrollPositionListener scrollPositionListener) {
        mContext = c;
        mCore = core;
        this.scrollPositionListener = scrollPositionListener;
    }

    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return mCore.countPages();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final PDFTextView pdfView;

        pdfView = new PDFTextView(mContext);
        ItemViewHolder holder = new ItemViewHolder(pdfView);
        RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) pdfView.getLayoutParams();
        if (null == lp) {
            lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            pdfView.setLayoutParams(lp);
        }

        return holder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int pos) {
        final int position = pos;
        if (null != scrollPositionListener) {
            scrollPositionListener.onScroll(position);
        }
        byte[] result = mCore.loadPage(position).textAsHtml();

        String text = new String(result);
        text = text.replaceAll("(<style>[^<]*</style>)|(<![^>*])", "").trim();
        Spanned spanned = Html.fromHtml(text);
        PDFTextView reflowView = (PDFTextView) holder.itemView;
        reflowView.setText(spanned);
    }

    class ItemViewHolder extends RecyclerView.ViewHolder {
        public PDFTextView pageView;

        public ItemViewHolder(PDFTextView itemView) {
            super(itemView);
            pageView = itemView;
        }

    }

    private static class PDFTextView extends TextView {

        Paint mPaint;
        float mTextSize = 15;
        float mScale = 1.0f;

        public PDFTextView(Context context) {
            super(context);
            mPaint = getPaint();
            mTextSize = mPaint.getTextSize();
            mPaint.setTextSize(mTextSize * 1.2f);
            //setTextSize(getTextSize()*1.1f);
            setPadding(40, 50, 40, 30);
            setLineSpacing(0, 0.75f);
            setTextColor(context.getResources().getColor(R.color.text_reflow_color));
            setBackgroundColor(context.getResources().getColor(R.color.text_reflow_bg_color));
            setTextIsSelectable(true);
        }

        @Override
        public boolean onTouchEvent(MotionEvent ev) {
            return false;
        }
    }
}
