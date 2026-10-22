package br.com.polegar.ndimonitor;

import android.content.Context;
import android.annotation.SuppressLint;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;

/** Keeps a desktop-sized stage intact and fits it inside the available display. */
@SuppressLint("ViewConstructor") // Created with its renderer in code; never inflated from XML.
final class HtmlStageViewport extends ViewGroup {
    static final int CSS_WIDTH = 1920;
    static final int CSS_HEIGHT = 1080;
    private final boolean stageLayout;

    HtmlStageViewport(Context context, View renderer, boolean stageLayout) {
        super(context);
        this.stageLayout = stageLayout;
        setBackgroundColor(Color.BLACK);
        addView(renderer, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }

    boolean usesStageLayout() { return stageLayout; }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        int width = MeasureSpec.getSize(widthSpec);
        int height = MeasureSpec.getSize(heightSpec);
        setMeasuredDimension(width, height);
        float density = getChildAt(0).getResources().getDisplayMetrics().density;
        int childWidth = stageLayout ? Math.max(1, Math.round(CSS_WIDTH * density)) : width;
        int childHeight = stageLayout ? Math.max(1, Math.round(CSS_HEIGHT * density)) : height;
        getChildAt(0).measure(MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.EXACTLY));
    }

    @Override protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        View renderer = getChildAt(0);
        int width = renderer.getMeasuredWidth(), height = renderer.getMeasuredHeight();
        renderer.layout(0, 0, width, height);
        float scale = stageLayout && width > 0 && height > 0
                ? Math.min((right - left) / (float) width, (bottom - top) / (float) height) : 1f;
        renderer.setPivotX(0);
        renderer.setPivotY(0);
        renderer.setScaleX(scale);
        renderer.setScaleY(scale);
        renderer.setTranslationX(((right - left) - width * scale) / 2f);
        renderer.setTranslationY(((bottom - top) - height * scale) / 2f);
    }
}
