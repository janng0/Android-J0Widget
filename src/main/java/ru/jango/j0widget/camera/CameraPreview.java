package ru.jango.j0widget.camera;

import java.util.List;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import ru.jango.j0util.LogUtil;
import ru.jango.j0util.RotationUtil;

public class CameraPreview extends ViewGroup implements SurfaceHolder.Callback {
    
    private SurfaceView surfaceView;
    private Size previewSize;
    private Camera camera;
    private boolean previewStarted;

    public CameraPreview(Context context) { super(context); init(context); }
    public CameraPreview(Context context, AttributeSet attrs) { super(context, attrs); init(context); }
    public CameraPreview(Context context, AttributeSet attrs, int style) { super(context, attrs, style); init(context); }

    private void init(Context context) {
    	setBackgroundColor(Color.BLACK);
    	previewStarted = false;
    	
        surfaceView = new SurfaceView(context);
        surfaceView.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
        surfaceView.getHolder().addCallback(this);
        surfaceView.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        surfaceView.setZOrderMediaOverlay(false);
        surfaceView.setZOrderOnTop(false);

        addView(surfaceView);
    }

    public void setCamera(Camera camera) {
        this.camera = camera;
        calculatePreviewSize();
    }

    /**
     * Starts preview. {@link android.hardware.Camera} object should be already set by
     * {@link #setCamera(android.hardware.Camera)}. Otherwise method will do nothing.
     */
    public void startPreview() {
    	if (camera == null || previewSize == null || previewStarted)
    		return;

   		configCamera();
   		try { 
   			surfaceView.setVisibility(View.VISIBLE);
   			camera.startPreview();
   			previewStarted = true;
   		} catch(Exception e) {
   			LogUtil.e(CameraPreview.class, "Starting preview failed: " + e);
   		}
    }

    /**
     * Stops preview. {@link android.hardware.Camera} object should be already set by
     * {@link #setCamera(android.hardware.Camera)}. Otherwise method will do nothing.
     */
    public void stopPreview() {
    	if (camera == null || !previewStarted)
    		return;
    	
   		try { 
   			previewStarted = false;
   			surfaceView.setVisibility(View.INVISIBLE);
   			camera.stopPreview();
   		} catch(Exception e) {
   			LogUtil.e(CameraPreview.class, "Stopping preview failed: " + e);
   		}
    }
    
    /**
     * Stops preview by {@link #stopPreview()} (it releases old camera), than remembers new camera
     * and starts preview from it.
     * <p>
     * <b>NOTE</b>: switch camera is NOT the same as stop preview, set new camera and start preview.
     * 
     * @param camera	new camera
     */
    public void switchCamera(Camera camera) {
    	stopPreview();
   		try { camera.setPreviewDisplay(surfaceView.getHolder()); } 
   		catch(Exception e) { LogUtil.e(CameraPreview.class, "Setting holder for new camera failed: " + e); }
   		
    	setCamera(camera);
    	startPreview();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    	super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        calculatePreviewSize();
        startPreview();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
    	if (!changed || getChildCount() <= 0)
    		return;

        if (previewSize == null) surfaceView.layout(0, 0, r - l, b - t);
    	else if (RotationUtil.getLayoutOrientation(getContext()) == Configuration.ORIENTATION_PORTRAIT)
            surfaceView.layout(0, 0, previewSize.height, previewSize.width);
        else surfaceView.layout(0, 0, previewSize.width, previewSize.height);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
   		try { camera.setPreviewDisplay(surfaceView.getHolder()); } 
   		catch(Exception e) { LogUtil.e(CameraPreview.class, "Setting SurfaceHolder for camera failed: " + e); }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    	stopPreview();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
    	startPreview();
    }

    private void calculatePreviewSize() {
        if (camera == null || getMeasuredWidth() == 0 || getMeasuredHeight() == 0)
            return;

        final List<Size> previewSizes = camera.getParameters().getSupportedPreviewSizes();
        if (previewSizes != null) {
            previewSize = getOptimalSize(previewSizes, getMeasuredWidth(), getMeasuredHeight());
            surfaceView.requestLayout();
        }
    }

    private void configCamera() {
    	if (camera == null || previewSize == null || previewStarted)
    		return;

        final Camera.Parameters params = camera.getParameters();
    	params.setPreviewSize(previewSize.width, previewSize.height);

        camera.setParameters(params);
		camera.setDisplayOrientation(RotationUtil.getCameraRotation(getContext()));
    }

    /**
     * Looks through collection of supported by camera sizes and returns the most suitable.
     */
    private Size getOptimalSize(List<Size> sizes, int w, int h) {
        if (sizes == null) 
        	return null;

        // optimal size should be the closest to the required; it could be checked by squares
        Size optimalSize = sizes.get(0);
        for (Size size : sizes)
        	if (Math.abs(w*h - size.width*size.height) <= Math.abs(w*h - optimalSize.width*optimalSize.height))
        		optimalSize = size; 

        return optimalSize;
    }
}
