package com.artifex.mupdfdemo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.artifex.mupdf.fitz.Document;

import cn.archko.pdf.R;
import cn.archko.pdf.ScrollPositionListener;
import cn.archko.pdf.utils.StreamUtils;
import cn.archko.pdf.utils.Util;
import cx.hell.android.pdfviewpro.APVApplication;

/**
 * @author: archko 2016/5/13 :11:03
 */
public class MuPDFReflowRecyclerViewAdapter extends RecyclerView.Adapter {
    private final Context mContext;
    private final Document mCore;
    private int height = 720;
    private float systemScale = Util.getScale();
    private int type = 0;

    private ScrollPositionListener scrollPositionListener;

    public MuPDFReflowRecyclerViewAdapter(Context c, Document core, ScrollPositionListener scrollPositionListener) {
        mContext = c;
        mCore = core;
        this.scrollPositionListener = scrollPositionListener;
        height = APVApplication.getInstance().screenHeight;
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
        byte[] result = mCore.loadPage(position).textAsText("preserve-images=yes,preserve-whitespace=yes");

        ((ItemViewHolder) holder).onBind(result);
    }

    class ItemViewHolder extends RecyclerView.ViewHolder {
        public PDFTextView pageView;

        public ItemViewHolder(PDFTextView itemView) {
            super(itemView);
            pageView = itemView;
        }

        public void onBind(byte[] result) {
            pageView.onBind(result);
        }
    }

    private class PDFTextView extends LinearLayout {

        Paint mPaint;
        float mTextSize = 15;
        float mScale = 1.0f;
        TextView textView;

        public PDFTextView(Context context) {
            super(context);
            setOrientation(VERTICAL);
            setMinimumHeight(height / 3);
            textView = new TextView(context);
            LayoutParams lp = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.gravity = Gravity.CENTER_HORIZONTAL;
            addView(textView, lp);
            setPadding(0, 40, 0, 40);
            setBackgroundColor(context.getResources().getColor(R.color.text_reflow_bg_color));

            mPaint = textView.getPaint();
            mTextSize = mPaint.getTextSize();
            mPaint.setTextSize(mTextSize * 1.2f);
            textView.setLineSpacing(0, 1.35f);
            textView.setPadding(30, 0, 30, 0);
            textView.setTextColor(context.getResources().getColor(R.color.text_reflow_color));
            //textView.setTextIsSelectable(true);
        }

        public void onBind(byte[] result) {
            String text = new String(result).trim();
            //Log.d("text", text = UnicodeDecoder.unescape2(text));
            Html.ImageGetter imageGetter = new Html.ImageGetter() {
                @Override
                public Drawable getDrawable(String source) {
                    //Log.d("text", source);
                    Bitmap bitmap = StreamUtils.base64ToBitmap(source.replaceAll("data:image/(png|jpeg);base64,", "")/*.replaceAll("\\s", "")*/);
                    if (null != bitmap) {
                        Drawable drawable = new BitmapDrawable(null, bitmap);
                        drawable.setBounds(0, 0, (int) (bitmap.getWidth() * systemScale), (int) (bitmap.getHeight() * systemScale));
                        return drawable;
                    }
                    Log.d("text", "bitmap decode failed.");
                    return null;
                }
            };
            Spanned spanned = Html.fromHtml(text, imageGetter, null);
            textView.setText(spanned);
        }
    }
}
