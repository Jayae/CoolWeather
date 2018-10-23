package com.coolweather.android.util;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class ViewPagerView extends ViewPager {
    int preX = 0;

    public ViewPagerView(@NonNull Context context) {
        super(context);
    }

    public ViewPagerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent even) {
        if (even.getAction() == MotionEvent.ACTION_DOWN) {
            preX = (int) even.getX();
        } else {
            if (Math.abs((int) even.getX() - preX) > 20) {
                return true;
            } else {
                preX = (int) even.getX();
            }
        }
        return super.onInterceptTouchEvent(even);
    }

}
