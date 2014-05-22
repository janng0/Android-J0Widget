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

    public static final float DEFAULT_LOADING_INDICATOR_BOLDNESS = 7;
    public static final int DEFAULT_LOADING_INDICATOR_BACKGROUND_COLOR = Color.DKGRAY;
    public static final int DEFAULT_LOADING_INDICATOR_COLOR = Color.LTGRAY;
    public static final int DEFAULT_FAIL_INDICATOR_COLOR = Color.rgb(255, 100, 100);
    public static final int DEFAULT_INDICATOR_MAX_SIZE = 120;

    private final RectF loadingIndicatorRect = new RectF();
    private final Paint loadingIndicatorBGPaint = new Paint();
    private final Paint loadingIndicatorPaint = new Paint();
    private final Paint failIndicatorPaint = new Paint();

    private float loadingIndBoldness;
    private int loadingIndBGColor;
    private int loadingIndColor;
    private int failIndColor;
    private int indMaxSize;

    private int progress;

    public TorusIndicator(Context context) { super(context); init(); }
    public TorusIndicator(Context context, AttributeSet attrs) { super(context, attrs); init(); }
    public TorusIndicator(Context context, AttributeSet attrs, int defStyle) { super(context, attrs, defStyle); init(); }

    private void init() {
        loadingIndBoldness = DEFAULT_LOADING_INDICATOR_BOLDNESS;
        loadingIndBGColor = DEFAULT_LOADING_INDICATOR_BACKGROUND_COLOR;
        loadingIndColor = DEFAULT_LOADING_INDICATOR_COLOR;
        failIndColor = DEFAULT_FAIL_INDICATOR_COLOR;
        indMaxSize = DEFAULT_INDICATOR_MAX_SIZE;

        initPaints();
    }

    private void initPaints() {
        fillLoadingIndicatorBGPaint(loadingIndicatorBGPaint);
        fillLoadingIndicatorPaint(loadingIndicatorPaint);
        fillFailIndicatorPaint(failIndicatorPaint);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (progress >= 0 && progress <= 100) {
            canvas.drawArc(loadingIndicatorRect, 0, 360, false, loadingIndicatorBGPaint);
            if (progress != 0)
                canvas.drawArc(loadingIndicatorRect, 0, progress * 360 / 100, false, loadingIndicatorPaint);
        } else canvas.drawArc(loadingIndicatorRect, 0, 360, false, failIndicatorPaint);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        initPaints();

        getLoadingIndicatorRect(loadingIndicatorRect);
    }

    ////////////////////////////////////////////////////////////////////////
    //
    // Drawing methods
    //
    ////////////////////////////////////////////////////////////////////////

    private int getIndicatorSize() {
        return Math.min(Math.min(getWidth(), getHeight()), indMaxSize);
    }

    /**
     * Just centers rect inside view.
     */
    private RectF getIndicatorRect(final RectF ret) {
        ret.set((getMeasuredWidth() - getIndicatorSize()) / 2,
                (getMeasuredHeight() - getIndicatorSize()) / 2,
                (getMeasuredWidth() + getIndicatorSize()) / 2,
                (getMeasuredHeight() + getIndicatorSize()) / 2);

        return ret;
    }

    private RectF getLoadingIndicatorRect(final RectF ret) {
        getIndicatorRect(ret);
        ret.inset(loadingIndBoldness / 2, loadingIndBoldness / 2);

        return ret;
    }

    private void fillStandardPaint(float strokeWidth, int color, final Paint paint) {
        paint.setStrokeWidth(strokeWidth);
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setDither(true);
        paint.setAntiAlias(true);
    }

    private Paint fillLoadingIndicatorBGPaint(final Paint paint) {
        fillStandardPaint(loadingIndBoldness, loadingIndBGColor, paint);
        return paint;
    }

    private Paint fillLoadingIndicatorPaint(final Paint paint) {
        fillStandardPaint(loadingIndBoldness, loadingIndColor, paint);
        return paint;
    }

    private Paint fillFailIndicatorPaint(final Paint paint) {
        fillStandardPaint(loadingIndBoldness, failIndColor, paint);
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
    public void setLoadingIndicatorBoldness(float boldness) {
        loadingIndBoldness = boldness;
        getLoadingIndicatorRect(loadingIndicatorRect);
        invalidate();
    }

    /**
     * Returns boldness of the line of the indicator torus.
     */
    public float getLoadingIndicatorBoldness() {
        return loadingIndBoldness;
    }

    /**
     * Sets color of the background torus.
     */
    public void setLoadingIndicatorBackgroundColor(int color) {
        loadingIndBGColor = color;
        fillLoadingIndicatorBGPaint(loadingIndicatorBGPaint);
        invalidate();
    }

    /**
     * Returns color of the background torus.
     */
    public int getLoadingIndicatorBackgroundColor() {
        return loadingIndBGColor;
    }

    /**
     * Sets color of the foreground torus.
     */
    public void setLoadingIndicatorColor(int color) {
        loadingIndColor = color;
        fillLoadingIndicatorPaint(loadingIndicatorPaint);
        invalidate();
    }

    /**
     * Returns color of the foreground torus.
     */
    public int getLoadingIndicatorColor() {
        return loadingIndColor;
    }

    /**
     * Sets color of the fail indicator torus.
     */
    public int getFailIndicatorColor() {
        return failIndColor;
    }

    /**
     * Returns color of the fail indicator torus.
     */
    public void setFailIndicatorColor(int failIndColor) {
        this.failIndColor = failIndColor;
    }

    /**
     * Indicator size is calculated based on current view size, but it could be
     * limited by this method.
     */
    public void setMaxIndicatorSize(int sizePx) {
        indMaxSize = sizePx;
        getLoadingIndicatorRect(loadingIndicatorRect);
        invalidate();
    }

    /**
     * Indicator size is calculated based on current view size, but it could be
     * limited by this value.
     */
    public int getMaxIndicatorSize() {
        return indMaxSize;
    }

}
