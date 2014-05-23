package ru.jango.j0widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * Simple widget that draws an arc (part of torus) and could be used as round loading indicator.
 * Also it can indicate some wrong situations by passing in {@link #setProgress(int)}
 * anything outside range [0, 100] (for example -1).
 */
public class TorusIndicator extends View {

    public static final float DEFAULT_BOLDNESS = 7;
    public static final int DEFAULT_BACKGROUND_COLOR = Color.DKGRAY;
    public static final int DEFAULT_COLOR = Color.LTGRAY;
    public static final int DEFAULT_FAIL_COLOR = Color.rgb(255, 100, 100);
    public static final int DEFAULT_MAX_SIZE = 120;

    private final RectF drawRect = new RectF();
    private final Paint bgPaint = new Paint();
    private final Paint paint = new Paint();
    private final Paint failPaint = new Paint();

    private float boldness;
    private int bgColor;
    private int color;
    private int failColor;
    private int maxSize;

    private int progress;

    public TorusIndicator(Context context) { super(context); init(); }
    public TorusIndicator(Context context, AttributeSet attrs) { super(context, attrs); init(); }
    public TorusIndicator(Context context, AttributeSet attrs, int defStyle) { super(context, attrs, defStyle); init(); }

    private void init() {
        boldness = DEFAULT_BOLDNESS;
        bgColor = DEFAULT_BACKGROUND_COLOR;
        color = DEFAULT_COLOR;
        failColor = DEFAULT_FAIL_COLOR;
        maxSize = DEFAULT_MAX_SIZE;

        initPaints();
    }

    private void initPaints() {
        fillBGPaint(bgPaint);
        fillPaint(paint);
        fillFailPaint(failPaint);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (progress >= 0 && progress <= 100) {
            canvas.drawArc(drawRect, 0, 360, false, bgPaint);
            if (progress != 0)
                canvas.drawArc(drawRect, 0, progress * 360 / 100, false, paint);
        } else canvas.drawArc(drawRect, 0, 360, false, failPaint);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        initPaints();

        getRect(drawRect);
    }

    ////////////////////////////////////////////////////////////////////////
    //
    // Drawing methods
    //
    ////////////////////////////////////////////////////////////////////////

    private int getSize() {
        return Math.min(Math.min(getWidth(), getHeight()), maxSize);
    }

    private RectF getRect(final RectF ret) {
        ret.set((getMeasuredWidth() - getSize()) / 2,
                (getMeasuredHeight() - getSize()) / 2,
                (getMeasuredWidth() + getSize()) / 2,
                (getMeasuredHeight() + getSize()) / 2);
        ret.inset(boldness / 2, boldness / 2);

        return ret;
    }

    private void fillStandardPaint(float strokeWidth, int color, final Paint paint) {
        paint.setStrokeWidth(strokeWidth);
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setDither(true);
        paint.setAntiAlias(true);
    }

    private Paint fillBGPaint(final Paint paint) {
        fillStandardPaint(boldness, bgColor, paint);
        return paint;
    }

    private Paint fillPaint(final Paint paint) {
        fillStandardPaint(boldness, color, paint);
        return paint;
    }

    private Paint fillFailPaint(final Paint paint) {
        fillStandardPaint(boldness, failColor, paint);
        return paint;
    }

    ////////////////////////////////////////////////////////////////////////
    //
    // Setters and getters
    //
    ////////////////////////////////////////////////////////////////////////

    /**
     * Passing something out of range [0, 100] indicates that something went wrong
     * (exceptional situation).
     *
     */
    public void setProgress(int progress) {
        this.progress = progress;
        invalidate();
    }

    public int getProgress() {
        return progress;
    }

    /**
     * Sets boldness of the line of the indicator torus.
     */
    public void setBoldness(float boldness) {
        this.boldness = boldness;
        getRect(drawRect);
        invalidate();
    }

    /**
     * Returns boldness of the line of the indicator torus.
     */
    public float getBoldness() {
        return boldness;
    }

    /**
     * Sets color of the background torus.
     */
    public void setIndicatorBackgroundColor(int color) {
        bgColor = color;
        fillBGPaint(bgPaint);
        invalidate();
    }

    /**
     * Returns color of the background torus.
     */
    public int getIndicatorBackgroundColor() {
        return bgColor;
    }

    /**
     * Sets color of the foreground torus.
     */
    public void setColor(int color) {
        this.color = color;
        fillPaint(paint);
        invalidate();
    }

    /**
     * Returns color of the foreground torus.
     */
    public int getColor() {
        return color;
    }

    /**
     * Sets color of the fail indicator torus.
     */
    public int getFailColor() {
        return failColor;
    }

    /**
     * Returns color of the fail indicator torus.
     */
    public void setFailColor(int failIndColor) {
        this.failColor = failIndColor;
    }

    /**
     * Indicator size is calculated based on current view size, but it could be
     * limited by this method.
     */
    public void setMaxSize(int sizePx) {
        maxSize = sizePx;
        getRect(drawRect);
        invalidate();
    }

    /**
     * Indicator size is calculated based on current view size, but it could be
     * limited by this value.
     */
    public int getMaxSize() {
        return maxSize;
    }

}
