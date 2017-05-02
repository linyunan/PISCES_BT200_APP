package com.yundiankj.ble_lock.Resource;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.yundiankj.ble_lock.R;

/**
 * Created by hong on 2016/9/6.
 * <p>
 * 自定义电池框
 */
public class BatteryView extends View {

    /**
     * 画笔信息
     */
    private Paint mBatteryPaint;
    private Paint mPowerPaint;
    private float mBatteryStroke = 2f;
    /**
     * 屏幕高宽
     */
//	 private int measureWidth;
//	 private int measureHeigth;
    /**
     * 电池参数
     */
    private float mBatteryHeight = 30f; // 电池的高度
    private float mBatteryWidth = 60f; // 电池的宽度
    private float mCapHeight = 15f;// 电池头部的高度
    private float mCapWidth = 5f;// 电池头部的宽度
    /**
     * 电池电量
     */
    private float mPowerPadding = 1;
    private float mPowerHeight = mBatteryHeight - mBatteryStroke - mPowerPadding * 2; // 电池身体的高度
    private float mPowerWidth = mBatteryWidth - mBatteryStroke - mPowerPadding * 2;// 电池身体的总宽度
    private float mPower = 100f;
    /**
     * 矩形
     */
    private RectF mBatteryRect;
    private RectF mCapRect;
    private RectF mPowerRect;

    public BatteryView(Context context) {
        super(context);
        initView();
    }

    public BatteryView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public BatteryView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    public void initView() {
        /**
         * 设置电池画笔
         */
        mBatteryPaint = new Paint();
        mBatteryPaint.setColor(Color.GRAY);
        mBatteryPaint.setAntiAlias(true);
        mBatteryPaint.setStyle(Paint.Style.STROKE);
        mBatteryPaint.setStrokeWidth(mBatteryStroke);
        /**
         * 设置电量画笔
         */
        mPowerPaint = new Paint();
        mPowerPaint.setColor(getResources().getColor(R.color.ffffff));
        mPowerPaint.setAntiAlias(true);
        mPowerPaint.setStyle(Paint.Style.FILL);
        mPowerPaint.setStrokeWidth(mBatteryStroke);
        /**
         * 设置电池矩形
         */
        mBatteryRect = new RectF(mCapWidth, 1, mBatteryWidth, mBatteryHeight);
        /**
         * 设置电池盖矩形
         */
        mCapRect = new RectF(0 + mBatteryWidth, (mBatteryHeight - mCapHeight) / 2, mCapWidth + mBatteryWidth,
                (mBatteryHeight - mCapHeight) / 2 + mCapHeight);
        setPowerRect();
    }

    private void setPowerRect() {
        /**
         * 设置电量矩形
         */
        float left=mCapWidth + mPowerPadding * 2;
        float top= mPowerPadding + mBatteryStroke / 2 + mBatteryStroke / 2;
        float right= mCapWidth / 3 + mBatteryStroke / 2 - mPowerPadding + mPowerWidth * ((mPower) / 100f);
        if (mPower<=50){
            right= mCapWidth / 3 + mBatteryStroke / 2 - mPowerPadding + mPowerWidth * ((mPower) / 100f)+5;
        }
        float bottom= mBatteryStroke / 2 + mPowerPadding + mPowerHeight;

        Log.e("url","电池身体的总宽度=="+mPowerWidth+"   left=="+left+"   top=="+top+"  right=="+right+"  bottom=="+bottom);

        mPowerRect = new RectF(mCapWidth + mPowerPadding * 2,
                // mCapWidth + mBatteryStroke / 2 + mPowerPadding
                // + mPowerWidth * ((100f - mPower) / 100f), //left: 需要调整左边的位置
                mPowerPadding + mBatteryStroke / 2 + mBatteryStroke / 2, // top:需要考虑到 画笔的宽度
                // mBatteryWidth - mPowerPadding * 2,//right
                right,
                mBatteryStroke / 2 + mPowerPadding + mPowerHeight);// bottom
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();
//		 canvas.translate(measureWidth / 2, measureHeigth / 2);// 居中显示

        canvas.drawRoundRect(mCapRect, 2f, 2f, mBatteryPaint);// 画电池盖

        canvas.drawRoundRect(mBatteryRect, 2f, 2f, mBatteryPaint); // 画电池轮廓需要考虑
        // 画笔的宽度

        canvas.drawRect(mPowerRect, mPowerPaint);// 画电量
        canvas.restore();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // measureWidth = MeasureSpec.getSize(widthMeasureSpec);
        // measureHeigth = MeasureSpec.getSize(heightMeasureSpec);
        // setMeasuredDimension(measureWidth, measureHeigth);

        /**
         * 设置宽度
         */
        int specModeWidth = MeasureSpec.getMode(widthMeasureSpec);
        int specSizeWidth = MeasureSpec.getSize(widthMeasureSpec);
        int specModeHeight = MeasureSpec.getMode(heightMeasureSpec);
        int specSizeHeight = MeasureSpec.getSize(heightMeasureSpec);

        int mWidth = 0;
        int mHeight = 0;
        // 处理宽度
        if (specModeWidth == MeasureSpec.EXACTLY)// match_parent , accurate
        {
            Log.e("xxx", "EXACTLY");
            mWidth = specSizeWidth;
        } else {
            if (specModeWidth == MeasureSpec.AT_MOST)// wrap_content
            {
                mWidth = (int) (mBatteryRect.width() + mCapRect.width() + mCapWidth + mBatteryStroke);
            }
        }

        // 处理高度

        if (specModeHeight == MeasureSpec.EXACTLY)// match_parent , accurate
        {
            Log.e("xxx", "EXACTLY");
            mHeight = specSizeHeight;
        } else {
            if (specModeHeight == MeasureSpec.AT_MOST)// wrap_content
            {
                mHeight = (int) (mBatteryRect.height() + mBatteryStroke);
            }
        }
//		 measureWidth = mWidth;
//		 measureHeigth = mHeight;
        setMeasuredDimension(mWidth, mHeight);
    }

    /**
     * ]
     *
     * @param power
     * @category 设置电池电量
     */
    public void setPower(float power) {
        mPower = power;
        if (mPower < 0) {
            mPower = 0;
        }
        if (mPower >= 100) {
            mPower = 100;
        }
        if (mPower <= 100 && mPower > 50) {
            mPowerPaint.setColor(getResources().getColor(R.color.A0fb818));
        }
        if (mPower <= 50 && mPower > 25) {
            mPowerPaint.setColor(getResources().getColor(R.color.d2650a));
        }
        if (mPower <= 25 && mPower > 0) {
            mPowerPaint.setColor(getResources().getColor(R.color.ca091c));
        }
        setPowerRect();
        invalidate();
    }

}
