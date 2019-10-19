package com.fcbengal.android.attendance.utils.animation;

import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;

public class ResizeAnimation extends Animation {
    final int targetHeight;
    View view;
    int startHeight;
    final String mTAG = this.getClass().getSimpleName();

    public ResizeAnimation(View view, int targetHeight, int startHeight) {
        this.view = view;
        this.targetHeight = targetHeight;
        this.startHeight = startHeight;
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
//        int newHeight = (int) (startHeight + targetHeight * interpolatedTime);
        //to support decent animation, change new heigt as Nico S. recommended in comments
        int newHeight = (int) (startHeight+(targetHeight - startHeight) * interpolatedTime);
        view.getLayoutParams().height = newHeight;
        view.requestLayout();
    }

    @Override
    public void initialize(int width, int height, int parentWidth, int parentHeight) {
        Log.e(mTAG, "initialize height " + height + " width " + width + " parent height " + parentHeight + " width " + parentWidth);
        super.initialize(width, height, parentWidth, parentHeight);
    }

    @Override
    public boolean willChangeBounds() {
        return true;
    }
}
