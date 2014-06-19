/*
 * The MIT License Copyright (c) 2014 Krayushkin Konstantin (jangokvk@gmail.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package ru.jango.j0widget.imagebrowser;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.OverScroller;

public class ImageBrowserView extends View {

    public static final float DEFAULT_MAX_ZOOM = 15f;

    private static final float AUTOZOOM_AMOUNT = 0.25f;

    private Bitmap bitmap;
    private float maxZoom;

    private final Paint bgPaint = new Paint();
    private final Rect contentRect = new Rect();
    private final Rect viewport = new Rect();

    // special vars for optimization: some methods need such objects (Rect and Point), so to not
    // create them every time, they are created here and reused where needed
    private final Rect rectBuffer = new Rect();
    private final Point pointBuffer = new Point();

    private ScaleGestureDetector scaleGestureDetector;
    private GestureDetectorCompat gestureDetector;
    private OverScroller scroller;
    private Zoomer zoomer;

    public ImageBrowserView(Context context) {
        super(context);
        init(context);
    }

    public ImageBrowserView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ImageBrowserView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context ctx) {
        scaleGestureDetector = new ScaleGestureDetector(ctx, new ScaleListener());
        gestureDetector = new GestureDetectorCompat(ctx, new GestureListener());
        scroller = new OverScroller(ctx);
        zoomer = new Zoomer(getResources().getInteger(android.R.integer.config_shortAnimTime));

        maxZoom = DEFAULT_MAX_ZOOM;
    }

    public void setImageBitmap(Bitmap bmp) {
        bitmap = bmp;
        if (bitmap == null) return;

        viewport.set(0, 0, bitmap.getWidth(), bitmap.getHeight());
        bgPaint.setColor(bmp.getPixel(0, 0));

        ViewCompat.postInvalidateOnAnimation(this);
    }

    public void setImageDrawable(Drawable drawable) {
        if (drawable == null) {
            bitmap = null;
            return;
        }

        setImageBitmap(drawableToBitmap(drawable));
    }

    public void setImageResource(int res) {
        setImageBitmap(BitmapFactory.decodeResource(getResources(), res));
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setMaxZoom(float maxZoom) {
        this.maxZoom = maxZoom;
    }

    public float getMaxZoom() {
        return maxZoom;
    }

    private Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable)
            return ((BitmapDrawable) drawable).getBitmap();

        int width = drawable.getIntrinsicWidth();
        width = width > 0 ? width : 1;
        int height = drawable.getIntrinsicHeight();
        height = height > 0 ? height : 1;

        final Bitmap bitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        contentRect.set(getPaddingLeft(), getPaddingTop(), w - getPaddingRight(), h - getPaddingBottom());
        checkViewportEmpty();
        checkViewportShapeAndBounds();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (bitmap == null) return;

        checkViewportEmpty();
        makeViewportDestRect(rectBuffer);

        canvas.drawRect(contentRect, bgPaint);
        canvas.drawBitmap(bitmap, viewport, rectBuffer, null);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (bitmap == null) return false;

        return scaleGestureDetector.onTouchEvent(event) |
                gestureDetector.onTouchEvent(event) |
                super.onTouchEvent(event);
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        if (bitmap == null) return;

        if (scroller.computeScrollOffset()) {
            viewport.offset(scroller.getCurrX() - viewport.left, 0);
            viewport.offset(0, scroller.getCurrY() - viewport.top);

            checkViewportBounds();
            ViewCompat.postInvalidateOnAnimation(this);
        }

        if (zoomer.computeZoom()) {
            viewport.set(zoomer.getCurrentRect());
            checkViewportShapeAndBounds();
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    /**
     * Viewport determines some special part of the bitmap, that should be shown. But a View
     * itself also has paddings and margins. This method creates a rect, that determines, where
     * View's content should actually be drawn.
     *
     * @param ret object, through witch the result would be returned
     */
    private void makeViewportDestRect(final Rect ret) {
        ret.set(0, 0, 0, 0);
        ret.right = (int) Math.floor(viewport.width() / getViewportScaleFactor());
        ret.bottom = (int) Math.floor(viewport.height() / getViewportScaleFactor());

        if (contentRect.width() - ret.right > 1)
            ret.offset((int) Math.ceil((contentRect.width() - ret.right) / 2), 0);
        if (contentRect.height() - ret.bottom > 1)
            ret.offset(0, (int) Math.ceil((contentRect.height() - ret.bottom) / 2));
    }

    /**
     * Checks if viewport rect was initialized. If it wasn't - method initializes it.
     */
    private void checkViewportEmpty() {
        if (bitmap == null) return;
        if (!viewport.isEmpty()) return;

        viewport.set(0, 0, bitmap.getWidth(), bitmap.getHeight());
    }

    private double getViewportScaleFactor() {
        if (viewport.isEmpty() || contentRect.isEmpty()) return 1d;

        double hScale = ((double) viewport.width()) / ((double) contentRect.width());
        double vScale = ((double) viewport.height()) / ((double) contentRect.height());
        return (viewport.height() > contentRect.height() || viewport.width() > contentRect.width()) ?
                Math.max(vScale, hScale) : Math.min(vScale, hScale);
    }

    /**
     * Checks viewport bounds to be in acceptable range and position.
     * <ul>
     * <li>viewport must have the same proportions, as bitmap has</li>
     * <li>viewport must not be smaller special value, according to {@link #getMaxZoom()}</li>
     * <li>viewport bounds must not be greater than bitmap bounds</li>
     * <li>viewport position must be inside bitmap bounds (to show some part of the bitmap,
     * not just empty space)</li>
     * </ul>
     */
    private void checkViewportShapeAndBounds() {
        if (bitmap == null) return;

        calculateSize(viewport.width() * viewport.height(),
                ((float) contentRect.width()) / ((float) contentRect.height()),
                pointBuffer);

        viewport.set(viewport.centerX() - pointBuffer.x / 2,
                viewport.centerY() - pointBuffer.y / 2,
                viewport.centerX() + pointBuffer.x / 2,
                viewport.centerY() + pointBuffer.y / 2);

        checkViewportBounds();
    }

    /**
     * Checks viewport bounds to be in acceptable range and position.
     * <ul>
     * <li>viewport must not be smaller special value, according to {@link #getMaxZoom()}</li>
     * <li>viewport bounds must not be greater than bitmap bounds</li>
     * <li>viewport position must be inside bitmap bounds (to show some part of the bitmap,
     * not just empty space)</li>
     * </ul>
     */
    private void checkViewportBounds() {
        if (bitmap == null) return;

        if (viewport.width() < bitmap.getWidth() / maxZoom || viewport.height() < bitmap.getHeight() / maxZoom) {
            int spread = (int) Math.max(bitmap.getWidth() / maxZoom - viewport.width(), (bitmap.getHeight() / maxZoom - viewport.height()));
            viewport.inset(-spread, -spread);
        }

        if (viewport.width() > bitmap.getWidth()) {
            viewport.left = 0;
            viewport.right = bitmap.getWidth();
        } else if (viewport.left < 0) viewport.offset(-viewport.left, 0);
        else if (viewport.right > bitmap.getWidth())
            viewport.offset(bitmap.getWidth() - viewport.right, 0);

        if (viewport.height() > bitmap.getHeight()) {
            viewport.top = 0;
            viewport.bottom = bitmap.getHeight();
        } else if (viewport.top < 0) viewport.offset(0, -viewport.top);
        else if (viewport.bottom > bitmap.getHeight())
            viewport.offset(0, bitmap.getHeight() - viewport.bottom);
    }

    /**
     * Calculates rectangle sides based on it's square and proportions. The point is to create a
     * new rect with the same square, but with the appropriate proportions of sides. This method
     * is a part of this process.
     *
     * @param square square of the source rectangle (width * height)
     * @param pro    sides factor of the source rectangle (width / height)
     * @param ret    object, through witch the result would be returned
     */
    private void calculateSize(float square, float pro, final Point ret) {
        float height = (float) Math.sqrt(square / pro);
        float width = height * pro;
        ret.set((int) width, (int) height);
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        private float initSpan;
        private final Rect initViewport = new Rect();

        @Override
        public boolean onScaleBegin(ScaleGestureDetector sgd) {
            initSpan = sgd.getCurrentSpan();
            initViewport.set(viewport);
            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector sgd) {
            int dx = (int) ((sgd.getFocusX() - contentRect.exactCenterX()) * getViewportScaleFactor() * (1f - 1 / sgd.getScaleFactor()));
            int dy = (int) ((sgd.getFocusY() - contentRect.exactCenterY()) * getViewportScaleFactor() * (1f - 1 / sgd.getScaleFactor()));

            int centerX = viewport.centerX() + dx;
            int centerY = viewport.centerY() + dy;
            float scale = initSpan / sgd.getCurrentSpan();
            viewport.set(centerX - ((int) ((initViewport.width() * scale) / 2)),
                    centerY - ((int) ((initViewport.height() * scale) / 2)),
                    centerX + ((int) ((initViewport.width() * scale) / 2)),
                    centerY + ((int) ((initViewport.height() * scale) / 2)));

            checkViewportShapeAndBounds();
            ViewCompat.postInvalidateOnAnimation(ImageBrowserView.this);
            return true;
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDown(MotionEvent e) {
            scroller.forceFinished(true);
            ViewCompat.postInvalidateOnAnimation(ImageBrowserView.this);
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            makeViewportDestRect(rectBuffer);

            int viewportX = viewport.left + ((int) ((e.getX() - rectBuffer.left) * getViewportScaleFactor()));
            int viewportY = viewport.top + ((int) ((e.getY() - rectBuffer.top) * getViewportScaleFactor()));
            if (viewportX < 0) viewportX = 0;
            if (viewportY < 0) viewportY = 0;

            zoomer.forceFinished(true);
            zoomer.startZoom(viewport, new Point(viewportX, viewportY), AUTOZOOM_AMOUNT);

            ViewCompat.postInvalidateOnAnimation(ImageBrowserView.this);
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            viewport.offset((int) (getViewportScaleFactor() * distanceX), (int) (getViewportScaleFactor() * distanceY));
            checkViewportBounds();

            ViewCompat.postInvalidateOnAnimation(ImageBrowserView.this);
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            scroller.forceFinished(true);
            scroller.fling(viewport.left, viewport.top, (int) (-velocityX), (int) (-velocityY),
                    0, bitmap.getWidth() - viewport.width(), 0, bitmap.getHeight() - viewport.height(),
                    0, 0);

            ViewCompat.postInvalidateOnAnimation(ImageBrowserView.this);
            return true;
        }
    }
}
