/*
 * Copyright 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.jango.j0widget.imagebrowser;

import android.graphics.Point;
import android.graphics.Rect;
import android.os.SystemClock;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

/**
 * A simple class that animates double-touch zoom gestures. Functionally similar to a {@link
 * android.widget.Scroller}.
 */
public class Zoomer {
    /**
     * The interpolator, used for making zooms animate 'naturally.'
     */
    private Interpolator mInterpolator;

    /**
     * The total animation duration for a zoom.
     */
    private int mAnimationDurationMillis;

    /**
     * Whether or not the current zoom has finished.
     */
    private boolean mFinished = true;

    /**
     * The current zoom value; computed by {@link #computeZoom()}.
     */
    private float mCurrentZoom;

    /**
     * The time the zoom started, computed using {@link android.os.SystemClock#elapsedRealtime()}.
     */
    private long mStartRTC;

    /**
     * The destination zoom factor.
     */
    private float mEndZoom;

    /**
     * The initial zooming rectangle
     */
    private Rect mSourceRect;
    
    /**
     * The currently zoomed rectangle - subset or superset of the {@link #mSourceRect} 
     * generally with the same center. If during zooming left or top parameters become 
     * negative - they would be fixed.
     */
    private Rect mCurrentRect;
    
    /**
     * The focal point of the current zoom 
     */
    private Point mFocalPoint;
    
    public Zoomer() {
        mInterpolator = new DecelerateInterpolator();
    }

    public Zoomer(int animationDurationMillis) {
    	this();
    	mAnimationDurationMillis = animationDurationMillis;
    }
    
    private void fixCurrentRectBounds() {
    	if (mCurrentRect==null || mCurrentRect.isEmpty()) return;
    	
    	if (mCurrentRect.left < 0) mCurrentRect.offset(-mCurrentRect.left, 0);
    	if (mCurrentRect.top < 0) mCurrentRect.offset(0, -mCurrentRect.top);
    }
    
    private void applyZoomToCurrentRect(float animationProgress) {
    	if (mSourceRect == null) return;
    	if (mCurrentRect == null) mCurrentRect = new Rect();
    	
    	mCurrentRect.set(mSourceRect.left + (((int) (mCurrentZoom*mSourceRect.width())/2)),
    					mSourceRect.top + (((int) (mCurrentZoom*mSourceRect.height())/2)),
    					mSourceRect.right - (((int) (mCurrentZoom*mSourceRect.width())/2)),
    					mSourceRect.bottom - (((int) (mCurrentZoom*mSourceRect.height())/2)));
    	
    	if (mFocalPoint != null)
    		mCurrentRect.offset(((int) ((mFocalPoint.x - mSourceRect.exactCenterX()) * animationProgress)), 
    				 ((int) ((mFocalPoint.y - mSourceRect.exactCenterY()) * animationProgress)));
    	
    	fixCurrentRectBounds();
    }
    
    /**
     * Forces the zoom finished state to the given value. Unlike {@link #abortAnimation()}, the
     * current zoom value isn't set to the ending value.
     *
     * @see android.widget.Scroller#forceFinished(boolean)
     */
    public void forceFinished(boolean finished) {
        mFinished = finished;
    }

    /**
     * Aborts the animation, setting the current zoom value to the ending value.
     *
     * @see android.widget.Scroller#abortAnimation()
     */
    public void abortAnimation() {
        mFinished = true;
        mCurrentZoom = mEndZoom;
        applyZoomToCurrentRect(1f);
    }

    /**
     * Starts a zoom from 1.0 to (1.0 + endZoom). That is, to zoom from 100% to 125%, endZoom should
     * be 0.25f.
     *
     * @see android.widget.Scroller#startScroll(int, int, int, int)
     */
    public void startZoom(Rect sourceRect, Point focalPoint, float endZoom) {
    	if (mSourceRect == null) mSourceRect = new Rect(); 
    	if (mFocalPoint == null) mFocalPoint = new Point();
    	
        mStartRTC = SystemClock.elapsedRealtime();
        mEndZoom = endZoom;

        mFinished = false;
        mCurrentZoom = 1f;
        mSourceRect.set(sourceRect);
        mFocalPoint.set(focalPoint.x, focalPoint.y);
    }

    /**
     * Computes the current zoom level, returning true if the zoom is still active and false if the
     * zoom has finished.
     *
     * @see android.widget.Scroller#computeScrollOffset()
     */
    public boolean computeZoom() {
        if (mFinished) {
            return false;
        }

        long tRTC = SystemClock.elapsedRealtime() - mStartRTC;
        if (tRTC >= mAnimationDurationMillis) {
            mFinished = true;
            mCurrentZoom = mEndZoom;
            applyZoomToCurrentRect(1f);
            return false;
        }

        float t = tRTC * 1f / mAnimationDurationMillis;
        mCurrentZoom = mEndZoom * mInterpolator.getInterpolation(t);
        applyZoomToCurrentRect(mInterpolator.getInterpolation(t));
        return true;
    }

    /**
     * Returns the current zoom level.
     *
     * @see android.widget.Scroller#getCurrX()
     */
    public float getCurrZoom() {
        return mCurrentZoom;
    }
    
    /**
     * Returns initial rectangle for zooming
     * 
     * @see #startZoom(Rect, Point, float)
     */
    public Rect getSourceRect() {
    	return mSourceRect;
    }
    
    /**
     * Returns source rectangle with applied current zoom level  
     */
    public Rect getCurrentRect() {
    	return mCurrentRect;
    }
    
    /**
     * Returns focal point of the current zoom 
     */
    public Point getFocalPoint() {
    	return mFocalPoint;
    }
    
    /**
     * Sets the duration of zooming
     * 
     * @param animationDurationMillis duration in milliseconds
     */
    public void setAnimationDurationMillis(int animationDurationMillis) {
    	mAnimationDurationMillis = animationDurationMillis;
    }
    
    /**
     * Returns the duration of zoom animation in milliseconds 
     */
    public int getAnimationDurationMillis() {
    	return mAnimationDurationMillis;
    }
}
