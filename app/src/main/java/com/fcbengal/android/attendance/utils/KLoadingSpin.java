package com.fcbengal.android.attendance.utils;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Region;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;
import com.fcbengal.android.attendance.R;

import org.apache.poi.util.StringUtil;

public class KLoadingSpin extends View {

    // Paint
    Paint mBackgroundPaint = new Paint();
    Paint mPrimaryCircleColor = new Paint();
    Paint mSecondaryCircleColor = new Paint();
    Paint mTextPaint = new Paint();


    // Metrics
    Integer mTotalWidth;
    Integer mTotalHeight;
    Integer mLeftGrid;
    Integer mTopGrid;
    Integer mRightGrid;
    Integer mBottomGrid;
    Integer mMargin;
    float mRotateDegrees = 0f;


    Integer backGroundColor;
    Integer spinnerPrimaryColor;
    Integer spinnerSecondaryColor;
    Integer textColor;
    Integer textSize;


    private ValueAnimator mValueAnimator;


    // Flags
    public boolean mIsVisible = false;

    // Speed
    Integer mRotationSpeedInMs = 1200;


    String text = "Please wait...";
    private Canvas canvas;

    // Path
    Path mCircleHolePath = new Path();
    Path mRectanglePath = new Path();


    public KLoadingSpin(Context context) {
        super(context);
    }

    public KLoadingSpin(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.KLoadingSpin,
                0, 0);


        try {
            text = a.getString(R.styleable.KLoadingSpin_text);
            mRotationSpeedInMs = a.getInteger(R.styleable.KLoadingSpin_rotationSpeedInMs, 1300);
            backGroundColor = a.getColor(R.styleable.KLoadingSpin_backgroundColor, 0x88094373);
            spinnerPrimaryColor = a.getColor(R.styleable.KLoadingSpin_primarySpinnerColor, 0xffffff);
            spinnerSecondaryColor = a.getColor(R.styleable.KLoadingSpin_secondarySpinnerColor, 0xff5723);
            textColor = a.getColor(R.styleable.KLoadingSpin_textColor, 0xffffff);
            textSize = a.getInteger(R.styleable.KLoadingSpin_textSize, 70);
        } finally {
            a.recycle();
        }


        mBackgroundPaint.setColor(backGroundColor);
        mBackgroundPaint.setStyle(Paint.Style.FILL);
        mBackgroundPaint.setAntiAlias(true);

        mPrimaryCircleColor.setColor(spinnerPrimaryColor);
        mPrimaryCircleColor.setStyle(Paint.Style.FILL);
        mPrimaryCircleColor.setAntiAlias(true);

        mSecondaryCircleColor.setColor(spinnerSecondaryColor);
        mSecondaryCircleColor.setStyle(Paint.Style.FILL);
        mSecondaryCircleColor.setAntiAlias(true);

        mTextPaint.setColor(textColor);
        mTextPaint.setStyle(Paint.Style.FILL);
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mTextPaint.setTextSize(textSize);
    }

    public KLoadingSpin(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        this.canvas = canvas;
        if (!mIsVisible) return;

        init();
        canvas.translate(mLeftGrid, mTopGrid);
        canvas.drawPaint(mBackgroundPaint); // Draw background

        canvas.save();
        drawHoledCircle(canvas);
        canvas.restore();
    }

    private void init() {
        mMargin = 40;
        mTotalHeight = getHeight();
        mTotalWidth = getWidth();

        mLeftGrid = mMargin;
        mTopGrid = mMargin;
        mRightGrid = mTotalWidth - mMargin;
        mBottomGrid = mTotalHeight - mMargin;
    }

    protected void drawHoledCircle(Canvas canvas) {

        Integer workingWidth = (mRightGrid - mLeftGrid) / 2;
        Integer workingHeight = (mBottomGrid - mTopGrid) / 2;

        canvas.translate(workingWidth, workingHeight); //Move it to center

        canvas.save();
        canvas.rotate(mRotateDegrees);
        mCircleHolePath.reset();
        mCircleHolePath.addCircle(0, 0, Math.min(workingHeight, workingWidth) / 5, Path.Direction.CW);
        canvas.clipPath(mCircleHolePath, Region.Op.DIFFERENCE);

        canvas.drawCircle(0, 0, Math.min(workingHeight, workingWidth) / 4, mPrimaryCircleColor);

        mRectanglePath.addRect(0, -Math.min(workingHeight, workingWidth) / 4,
                Math.min(workingHeight, workingWidth) / 4,
                0, Path.Direction.CCW);
        canvas.clipPath(mRectanglePath, Region.Op.INTERSECT);

        canvas.drawCircle(0, 0, Math.min(workingHeight, workingWidth) / 4, mSecondaryCircleColor);
        canvas.restore();

        if (text != null) {
            String[] lines = splitStringInLines(text);

            int i = 1;
            for (String line : lines) {
                canvas.drawText(line, 0, Math.min(workingHeight, workingWidth) / 4 +
                        i * ((mTextPaint.descent() - mTextPaint.ascent()) / 2) + 40 + i * 20, mTextPaint);
                i++;
            }
        }
    }

    public void setText(String text) {
//        stopAnimation();
//        this.text = text;
//        startAnimation();
        if(null != canvas && !TextUtils.isEmpty(text)){

            canvas.save();
            String[] lines = splitStringInLines(text);

            int i = 1;
            int workingWidth = (mRightGrid - mLeftGrid) / 2;
            int workingHeight = (mBottomGrid - mTopGrid) / 2;
            for (String line : lines) {
                canvas.drawText(line, 0, Math.min(workingHeight, workingWidth) / 4 +
                        i * ((mTextPaint.descent() - mTextPaint.ascent()) / 2) + 40 + i * 20, mTextPaint);
                i++;
            }
            canvas.restore();
            startAnimation();
        }
    }

    // Determines if the loading spin in visible or not
    public void setIsVisible(Boolean isVisible) {
        mIsVisible = isVisible;
        invalidate();

    }

    public void stopAnimation() {
        mValueAnimator.removeAllUpdateListeners();
        setIsVisible(false);
        invalidate();
    }


    public void startAnimation() {
        if (mValueAnimator != null) {
            mValueAnimator.removeAllUpdateListeners();
        }
        mValueAnimator = ValueAnimator.ofFloat(0, 360);
        mValueAnimator.removeAllUpdateListeners();
        mValueAnimator.setDuration(mRotationSpeedInMs);
        mValueAnimator.setInterpolator(new LinearInterpolator());
        mValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                mRotateDegrees = (float) animation.getAnimatedValue();
                invalidate();


                if (mRotateDegrees == 360) {
                    startAnimation();
                }


            }
        });
        mValueAnimator.start();
    }

    public static String[] splitStringInLines(String string) {
        return string.split("\n");
    }
}
