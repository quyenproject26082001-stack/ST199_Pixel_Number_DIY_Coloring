package com.skin.rbx.clothes.makek.ui.outfit.c253.colorpicker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import com.skin.rbx.clothes.makek.R;

public abstract class ColorSliderView extends View implements ColorObservable, Updatable {
    protected int baseColor = Color.WHITE;
    private Paint colorPaint;
    private Paint borderPaint;
    private Paint selectorPaint;

    //    private Path selectorPath;
//    private Path currentSelectorPath = new Path();
    Paint thumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    Paint thumbBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    protected float selectorSize;
    protected float currentValue = 1f;
    private boolean onlyUpdateOnTouchEventUp;

    private ColorObservableEmitter emitter = new ColorObservableEmitter();
    private ThrottledTouchEventHandler handler = new ThrottledTouchEventHandler(this);

    public ColorSliderView(Context context) {
        this(context, null);
    }

    public ColorSliderView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ColorSliderView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        colorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(0);
        borderPaint.setColor(Color.BLACK);
        selectorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        selectorPaint.setColor(Color.BLACK);

        thumbPaint.setColor(Color.WHITE);
        thumbBorderPaint.setStyle(Paint.Style.STROKE);
        thumbBorderPaint.setStrokeWidth(4);
        thumbBorderPaint.setColor(Color.BLACK);
//        selectorPath = new Path();
//        selectorPath.setFillType(Path.FillType.WINDING);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        configurePaint(colorPaint);

        selectorSize = w * 0.25f;
//        selectorPath.reset();
//        selectorPath.moveTo(0, 0);
//        selectorPath.lineTo(selectorSize, selectorSize * 2);
//        selectorPath.lineTo(selectorSize * 2, selectorSize);
//        selectorPath.close();
    }


    @SuppressLint("RestrictedApi")
    @Override
    protected void onDraw(Canvas canvas) {
        float width = getWidth();
        float height = getHeight();

        // track dọc
        canvas.drawRect(
                0,
                selectorSize,
                width - selectorSize,
                height - selectorSize,
                colorPaint
        );
        canvas.drawRect(
                0,
                selectorSize,
                width - selectorSize,
                height - selectorSize,
                borderPaint
        );

//        currentSelectorPath.reset();
//        currentSelectorPath.addPath(
//                selectorPath,
//                0,
//                selectorSize + currentValue * (height - 2 * selectorSize)
//        );
//
//        canvas.drawPath(currentSelectorPath, selectorPaint);

        // ==== VỊ TRÍ HÌNH TRÒN ====
        float cx = width / 2f; // nằm giữa ngang
        float cy = selectorSize * 3 + currentValue * (height - 2 * selectorSize * 3);
        float radius = selectorSize * 3f / 2f - getResources().getDisplayMetrics().density;

        float thumbOffset = 3f * getResources().getDisplayMetrics().density;
        canvas.drawCircle(cx - thumbOffset, cy, radius, selectorPaint);
        canvas.drawCircle(cx - thumbOffset, cy, radius, thumbPaint);
        canvas.drawCircle(cx - thumbOffset, cy, radius, thumbBorderPaint);
    }


    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                handler.onTouchEvent(event);
                return true;
            case MotionEvent.ACTION_UP:
                update(event);
                return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public void update(MotionEvent event) {
        updateValue(event.getY());
        boolean isUp = event.getActionMasked() == MotionEvent.ACTION_UP;
        if (!onlyUpdateOnTouchEventUp || isUp) {
            emitter.onColor(assembleColor(), true, isUp);
        }
    }

    void setBaseColor(int color, boolean fromUser, boolean shouldPropagate) {
        baseColor = color;
        configurePaint(colorPaint);
        int targetColor = color;
        if (!fromUser) {
            // if not set by user (means programmatically), resolve currentValue from color value
            currentValue = resolveValue(color);
        } else {
            targetColor = assembleColor();
        }

        if (!onlyUpdateOnTouchEventUp) {
            emitter.onColor(targetColor, fromUser, shouldPropagate);
        } else if (shouldPropagate) {
            emitter.onColor(targetColor, fromUser, true);
        }
        invalidate();
    }

    private void updateValue(float eventY) {
        float top = selectorSize;
        float bottom = getHeight() - selectorSize;

        if (eventY < top) eventY = top;
        if (eventY > bottom) eventY = bottom;

        currentValue = (eventY - top) / (bottom - top);
        invalidate();
    }

    protected abstract float resolveValue(int color);

    protected abstract void configurePaint(Paint colorPaint);

    protected abstract int assembleColor();

    @Override
    public void subscribe(ColorObserver observer) {
        emitter.subscribe(observer);
    }

    @Override
    public void unsubscribe(ColorObserver observer) {
        emitter.unsubscribe(observer);
    }

    @Override
    public int getColor() {
        return emitter.getColor();
    }

    public void setOnlyUpdateOnTouchEventUp(boolean onlyUpdateOnTouchEventUp) {
        this.onlyUpdateOnTouchEventUp = onlyUpdateOnTouchEventUp;
    }

    private ColorObserver bindObserver = new ColorObserver() {
        @Override
        public void onColor(int color, boolean fromUser, boolean shouldPropagate) {
            setBaseColor(color, fromUser, shouldPropagate);
        }
    };

    private ColorObservable boundObservable;

    public void bind(ColorObservable colorObservable) {
        if (colorObservable != null) {
            colorObservable.subscribe(bindObserver);
            setBaseColor(colorObservable.getColor(), true, true);
        }
        boundObservable = colorObservable;
    }

    public void unbind() {
        if (boundObservable != null) {
            boundObservable.unsubscribe(bindObserver);
            boundObservable = null;
        }
    }
}
