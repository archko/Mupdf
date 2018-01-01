package org.vudroid.core.views;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import org.vudroid.core.events.BringUpZoomControlsListener;
import org.vudroid.core.models.CurrentPageModel;

import cn.archko.pdf.R;

/**
 * page seek controls
 *
 * @author archko
 */
public class PageSeekBarControls extends LinearLayout implements BringUpZoomControlsListener {

    private final static String TAG = "PageSeekBarControls";

    private SeekBar mPageSlider;
    private int mPageSliderRes;
    private TextView mPageNumberView;
    private Runnable gotoPageRunnable = null;
    CurrentPageModel mPageModel;

    public PageSeekBarControls(Context context, CurrentPageModel pageModel) {
        super(context);
        this.mPageModel = pageModel;
        onCreate(context);
        int smax = Math.max(mPageModel.getPageCount() - 1, 1);
        mPageSliderRes = ((10 + smax - 1) / smax) * 2;
    }

    public PageSeekBarControls(Context context, AttributeSet attrs) {
        super(context, attrs);
        onCreate(context);
    }

    public void onCreate(Context context) {
        setOrientation(LinearLayout.VERTICAL);
        LayoutParams lp;

        mPageSlider = new SeekBar(context);
        //mPageSlider.setId(10000);
        //mPageSlider.setThumb(getResources().getDrawable(R.drawable.seek_thumb));
        mPageSlider.setProgressDrawable(getResources().getDrawable(R.drawable.seek_progress));
        mPageSlider.setBackgroundResource(R.color.toolbar);
        lp = new LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        //lp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        lp.leftMargin = 16;
        lp.rightMargin = 16;
        lp.topMargin = 8;
        lp.bottomMargin = 16;
        addView(this.mPageSlider, lp);

        mPageNumberView = new TextView(context);
        mPageNumberView.setBackgroundResource(R.drawable.page_num);
        mPageNumberView.setTextColor(getResources().getColor(android.R.color.white));
        //mPageNumberView.setTextAppearance(context, android.R.attr.textAppearanceMedium);
        lp = new LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        //lp.addRule(RelativeLayout.ABOVE, 10000);
        //lp.addRule(RelativeLayout.CENTER_IN_PARENT);
        lp.topMargin = 20;
        lp.bottomMargin = 16;
        lp.gravity = Gravity.CENTER_HORIZONTAL;
        addView(this.mPageNumberView, lp);

        //mPageSlider.setVisibility(View.GONE);
        //mPageNumberView.setVisibility(View.GONE);

        mPageSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onStopTrackingTouch(SeekBar seekBar) {
                PageSeekBarControls.this.gotoPage((seekBar.getProgress() + mPageSliderRes / 2) / mPageSliderRes);
                showPageSlider(false);
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (null != mPageModel) {
                    int index = (progress + mPageSliderRes / 2) / mPageSliderRes;
                    mPageNumberView.setText(String.format("%d / %d", index + 1, mPageModel.getPageCount()));
                    showPageSlider(false);
                }
            }
        });
        this.gotoPageRunnable = new Runnable() {
            public void run() {
                fadePageSlider();
            }
        };
    }

    private void fadePageSlider() {
        //mPageSlider.setVisibility(View.GONE);
        mPageNumberView.setVisibility(View.GONE);
    }

    public void showPageSlider(boolean force) {
        //mPageSlider.setVisibility(View.VISIBLE);
        mPageNumberView.setVisibility(View.VISIBLE);

        if (!force) {
            return;
        }

        int index = null != mPageModel ? mPageModel.getCurrentPageIndex() : 0;
        int count = null != mPageModel ? mPageModel.getPageCount() : 0;
        mPageNumberView.setText(String.format("%d / %d", index + 1, count));
        mPageSlider.setMax((count - 1) * mPageSliderRes);
        mPageSlider.setProgress(index * mPageSliderRes);
    }

    public void showGotoPageView() {
        if (null != mPageModel) {
            int smax = Math.max(mPageModel.getPageCount() - 1, 1);
            mPageSliderRes = ((10 + smax - 1) / smax) * 2;
            showPageSlider(true);
        }
    }

    private void gotoPage(int page) {
        Log.i(TAG, "rewind to page " + page);
        if (null != mPageModel && mPageModel.getCurrentPageIndex() != page) {
            mPageModel.goToPageIndex(page);
        }
    }

    public void toggleZoomControls() {
        if (getVisibility() == View.VISIBLE) {
            hide();
        } else {
            show();
        }
    }

    public void show() {
        setVisibility(VISIBLE);
        showGotoPageView();
    }

    public void hide() {
        setVisibility(GONE);
    }

    public void fade() {
        show();
        postDelayed(new Runnable() {
            @Override
            public void run() {
                hide();
            }
        }, 3000);
    }
}
