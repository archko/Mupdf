package com.artifex.mupdfdemo;

import android.content.Context;
import android.graphics.Paint;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.Spanned;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.TextView;

import cn.archko.pdf.R;
import cn.archko.pdf.utils.Util;

/**
 * @author: archko 2016/5/13 :11:03
 */
public class MuPDFReflowRecyclerViewAdapter extends RecyclerView.Adapter {
    private final Context mContext;
    private final MuPDFCore mCore;

    public MuPDFReflowRecyclerViewAdapter(Context c, MuPDFCore core) {
        mContext = c;
        mCore = core;
    }


    public long getItemId(int arg0) {
        return 0;
    }

    @Override
    public int getItemCount() {
        return mCore.countPages();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final PDFTextView pageView;

        pageView = new PDFTextView(mContext, this);
        ItemViewHolder holder = new ItemViewHolder(pageView);

        return holder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int pos) {
        final int position = pos;
        PDFTextView reflowView = (PDFTextView) holder.itemView;
        byte[] result = mCore.text(position);

        String text = new String(result);
        Spanned spanned = Html.fromHtml(text);
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
        MuPDFReflowRecyclerViewAdapter mAdapter;

        public PDFTextView(Context context, MuPDFReflowRecyclerViewAdapter adapter) {
            super(context);
            mAdapter = adapter;
            mPaint = getPaint();
            mTextSize = mPaint.getTextSize();
            mPaint.setTextSize(mTextSize * 1.2f);
            //setTextSize(getTextSize()*1.1f);
            setPadding(40, 50, 40, 30);
            setLineSpacing(0, 1.2f);
            setTextColor(context.getResources().getColor(R.color.text_reflow_color));
            setBackgroundColor(context.getResources().getColor(R.color.text_reflow_bg_color));
            setWidth(Util.getScreenWidthPixelWithOrientation(context));
            setTextIsSelectable(true);
        }

        @Override
        public boolean onTouchEvent(MotionEvent ev) {
            return false;
        }
    }
}
