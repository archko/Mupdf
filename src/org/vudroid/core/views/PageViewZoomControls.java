package org.vudroid.core.views;

import android.content.Context;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.LinearLayout;
import org.vudroid.core.events.BringUpZoomControlsListener;
import org.vudroid.core.models.ZoomModel;

public class PageViewZoomControls extends LinearLayout implements BringUpZoomControlsListener
{
    public PageViewZoomControls(Context context, final ZoomModel zoomModel)
    {
        super(context);
        show();
        setOrientation(LinearLayout.HORIZONTAL);
        setGravity(Gravity.BOTTOM);
        addView(new ZoomRoll(context, zoomModel));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        return false;
    }

    public void toggleZoomControls()
    {
        if (getVisibility() == View.VISIBLE)
        {
            hide();
        }
        else
        {
            show();
        }
    }

    private void show()
    {
        fade(View.VISIBLE, getWidth(), 0.0f);
    }

    private void hide()
    {
        fade(View.GONE, 0.0f, getWidth());
    }

    private void fade(int visibility, float startDelta, float endDelta)
    {
        Animation anim = new TranslateAnimation(0,0, startDelta, endDelta);
        anim.setDuration(500);
        startAnimation(anim);
        setVisibility(visibility);
    }
}
