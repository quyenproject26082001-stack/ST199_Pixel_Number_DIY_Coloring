package com.skin.rbx.clothes.makek.ui.outfit.c253.colorpicker;

import static android.view.Gravity.CENTER;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.annotation.Nullable;

import com.skin.rbx.clothes.makek.R;

import java.util.ArrayList;
import java.util.List;

public class ColorPickerView extends LinearLayout implements ColorObservable {
    private LinearLayout sliderContainer;
    private ColorWheelView colorWheelView;
    private BrightnessSliderView brightnessSliderView;
    private AlphaSliderView alphaSliderView;
    private ColorObservable observableOnDuty;
    private boolean onlyUpdateOnTouchEventUp;

    private int initialColor = Color.BLACK;

    private int sliderMargin;
    private int sliderHeight;

    public ColorPickerView(Context context) {
        this(context, null);
    }

    public ColorPickerView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ColorPickerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(HORIZONTAL);
        setGravity(CENTER);
        sliderContainer = new LinearLayout(context);
        sliderContainer.setOrientation(HORIZONTAL);
        LayoutParams sliderParams = new LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
//
        addView(sliderContainer, sliderParams);

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.ColorPickerView);
        boolean enableAlpha = typedArray.getBoolean(R.styleable.ColorPickerView_enableAlpha, false);
        boolean enableBrightness = typedArray.getBoolean(R.styleable.ColorPickerView_enableBrightness, true);
        onlyUpdateOnTouchEventUp = typedArray.getBoolean(R.styleable.ColorPickerView_onlyUpdateOnTouchEventUp, false);
        typedArray.recycle();

        colorWheelView = new ColorWheelView(context);
        float density = getResources().getDisplayMetrics().density;
        int margin = (int) (8 * density);
        sliderMargin = 2 * margin;
        sliderHeight = (int) (24 * density);

        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        addView(colorWheelView,0, params);

        setEnabledBrightness(enableBrightness);
        setEnabledAlpha(enableAlpha);

        setPadding(margin, margin, margin, margin);
    }

    public void setOnlyUpdateOnTouchEventUp(boolean onlyUpdateOnTouchEventUp) {
        this.onlyUpdateOnTouchEventUp = onlyUpdateOnTouchEventUp;
        updateObservableOnDuty();
    }

//    @Override
//    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        int maxWidth = MeasureSpec.getSize(widthMeasureSpec);
//        int maxHeight = MeasureSpec.getSize(heightMeasureSpec);
//
//
//        int desiredWidth = maxHeight - (getPaddingTop() + getPaddingBottom()) + (getPaddingLeft() + getPaddingRight());
//        if (brightnessSliderView != null) {
//            desiredWidth -= (sliderMargin + sliderHeight);
//        }
//        if (alphaSliderView != null) {
//            desiredWidth -= (sliderMargin + sliderHeight);
//        }
//
//
//        int width = Math.min(maxWidth, desiredWidth);
//        int height = width - (getPaddingLeft() + getPaddingRight()) + (getPaddingTop() + getPaddingBottom());
//        if (brightnessSliderView != null) {
//            height += (sliderMargin + sliderHeight);
//        }
//        if (alphaSliderView != null) {
//            height += (sliderMargin + sliderHeight);
//        }
//
//        super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.getMode(widthMeasureSpec)),
//                MeasureSpec.makeMeasureSpec(height, MeasureSpec.getMode(heightMeasureSpec)));
//    }

    public void setInitialColor(int color) {
        initialColor = color;
        colorWheelView.setColor(color, true);
    }

    public void setEnabledBrightness(boolean enable) {
        if (enable) {
            if (brightnessSliderView == null) {
                brightnessSliderView = new BrightnessSliderView(getContext());
                LayoutParams params = new LayoutParams(sliderHeight,ViewGroup.LayoutParams.MATCH_PARENT );
                params.topMargin = sliderMargin;
//                addView(brightnessSliderView, 1, params);
                brightnessSliderView.setRotation(180);
                sliderContainer.addView(brightnessSliderView, params);
            }

            brightnessSliderView.bind(colorWheelView);
            updateObservableOnDuty();
        } else {
            if (brightnessSliderView != null) {
                brightnessSliderView.unbind();
                sliderContainer.removeView(brightnessSliderView);
                brightnessSliderView = null;
            }
            updateObservableOnDuty();
        }

        if (alphaSliderView != null) {
            setEnabledAlpha(true);
        }
    }

    public void setEnabledAlpha(boolean enable) {
        if (enable) {
            if (alphaSliderView == null) {
                alphaSliderView = new AlphaSliderView(getContext());
                LayoutParams params = new LayoutParams(sliderHeight,ViewGroup.LayoutParams.MATCH_PARENT);
                params.topMargin = sliderMargin;
                alphaSliderView.setRotation(180);
                sliderContainer.addView(alphaSliderView, params);
            }

            ColorObservable bindTo = brightnessSliderView;
            if (bindTo == null) {
                bindTo = colorWheelView;
            }
            alphaSliderView.bind(bindTo);
            updateObservableOnDuty();
        } else {
            if (alphaSliderView != null) {
                alphaSliderView.unbind();
                sliderContainer.removeView(alphaSliderView);
                alphaSliderView = null;
            }
            updateObservableOnDuty();
        }
    }

    private void updateObservableOnDuty() {
        if (observableOnDuty != null) {
            for (ColorObserver observer : observers) {
                observableOnDuty.unsubscribe(observer);
            }
        }

        colorWheelView.setOnlyUpdateOnTouchEventUp(false);
        if (brightnessSliderView != null) {
            brightnessSliderView.setOnlyUpdateOnTouchEventUp(false);
        }
        if (alphaSliderView != null) {
            alphaSliderView.setOnlyUpdateOnTouchEventUp(false);
        }

        if (brightnessSliderView == null && alphaSliderView == null) {
            observableOnDuty = colorWheelView;
            colorWheelView.setOnlyUpdateOnTouchEventUp(onlyUpdateOnTouchEventUp);
        } else {
            if (alphaSliderView != null) {
                observableOnDuty = alphaSliderView;
                alphaSliderView.setOnlyUpdateOnTouchEventUp(onlyUpdateOnTouchEventUp);
            } else {
                observableOnDuty = brightnessSliderView;
                brightnessSliderView.setOnlyUpdateOnTouchEventUp(onlyUpdateOnTouchEventUp);
            }
        }

        if (observers != null) {
            for (ColorObserver observer : observers) {
                observableOnDuty.subscribe(observer);
                observer.onColor(observableOnDuty.getColor(), false, true);
            }
        }
    }

    public void reset() {
        colorWheelView.setColor(initialColor, true);
    }

    List<ColorObserver> observers = new ArrayList<>();

    @Override
    public void subscribe(ColorObserver observer) {
        observableOnDuty.subscribe(observer);
        observers.add(observer);
    }

    @Override
    public void unsubscribe(ColorObserver observer) {
        observableOnDuty.unsubscribe(observer);
        observers.remove(observer);
    }

    @Override
    public int getColor() {
        return observableOnDuty.getColor();
    }
}
