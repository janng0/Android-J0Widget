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

package ru.jango.j0widget.camera.fragment;

import android.graphics.Color;
import android.graphics.Point;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import java.util.List;

import ru.jango.j0util.LogUtil;
import ru.jango.j0util.RotationUtil;
import ru.jango.j0widget.camera.CameraPreview;

/**
 * Base fragment for managing {@link ru.jango.j0widget.camera.CameraPreview}. Creates layout, does
 * basic configurations, cares for basic camera trouble - <b>monkey</b> users Oo See
 * {@link #setTakePictureFrequency(int)}.
 */
public abstract class AbstractCameraFragment extends Fragment implements Camera.PictureCallback {

    public static final boolean DEFAULT_RESTART_ON_RESUME = true;
    public static final int DEFAULT_TAKE_PICTURE_FREQUENCY = 3000;
    public static final Point DEFAULT_PICTURE_SIZE = new Point(800, 600);

    protected boolean restartOnResume;
    protected int cameraId;
    protected Camera camera;
    protected CameraPreview preview;

    private int takePictureFrequency;
    private long lastPictureTaken;
    private Point picSize;

    protected RelativeLayout root;

    public AbstractCameraFragment() {
        cameraId = getBackwardCameraId();
        lastPictureTaken = 0;

        restartOnResume = DEFAULT_RESTART_ON_RESUME;
        takePictureFrequency = DEFAULT_TAKE_PICTURE_FREQUENCY;
        picSize = DEFAULT_PICTURE_SIZE;
    }

    ///////////////////////////////////////////////////////////////
    //
    //						Fragment staff
    //
    ///////////////////////////////////////////////////////////////

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        root = createRelativeLayout();
        preview = createCameraPreview();

        root.addView(preview);
        return root;
    }

    private RelativeLayout createRelativeLayout() {
        final RelativeLayout layout = new RelativeLayout(getActivity());
        layout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));
        layout.setBackgroundColor(Color.BLACK);

        return layout;
    }

    private CameraPreview createCameraPreview() {
        final CameraPreview preview = new CameraPreview(getActivity());
        preview.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));

        return preview;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (restartOnResume) restartPreview();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopPreview();
    }

    ///////////////////////////////////////////////////////////////
    //
    // 					Getters and setters
    //
    ///////////////////////////////////////////////////////////////

    /**
     * Returns min acceptable time between taking photos in milliseconds.
     */
    public int getTakePictureFrequency() {
        return takePictureFrequency;
    }

    /**
     * Sets min acceptable time between taking photos in milliseconds.
     */
    public void setTakePictureFrequency(int takePictureFrequency) {
        this.takePictureFrequency = takePictureFrequency;
    }

    /**
     * After taking photo, it would be automatically rotated and scaled down and then passed into
     * {@link SimpleCameraFragment.CameraFragmentListener#onProcessingFinished(java.net.URI, byte[], android.graphics.Bitmap)}.
     */
    public Point getPictureSize() {
        return picSize;
    }

    /**
     * After taking photo, it would be automatically rotated and scaled down and then passed into
     * {@link SimpleCameraFragment.CameraFragmentListener#onProcessingFinished(java.net.URI, byte[], android.graphics.Bitmap)}.
     */
    public void setPictureSize(Point size) {
        this.picSize = size;
    }

    public boolean shouldRestartOnResume() {
        return restartOnResume;
    }

    public void setRestartOnResume(boolean restartOnResume) {
        this.restartOnResume = restartOnResume;
    }

    ///////////////////////////////////////////////////////////////
    //
    //						Camera staff
    //
    ///////////////////////////////////////////////////////////////

    protected Camera.Size getOptimalSize(List<Camera.Size> sizes) {
        if (sizes == null)
            return null;

        // optimal size should be the closest to the required; it could be checked by squares
        Camera.Size optimalSize = sizes.get(0);
        for (Camera.Size size : sizes) {
            final boolean squareCheck = Math.abs(picSize.x * picSize.y - size.width * size.height) <=
                    Math.abs(picSize.x * picSize.y - optimalSize.width * optimalSize.height);
            final boolean widthCheck = picSize.x <= size.width;
            final boolean heightCheck = picSize.y <= size.height;

            if (squareCheck && (widthCheck || heightCheck))
                optimalSize = size;
        }

        return optimalSize;
    }

    /**
     * Place, where {@link android.hardware.Camera} object is obtained and configured. Reload it for
     * more configurations.
     */
    protected void openCamera() {
        camera = Camera.open(cameraId);

        final List<Camera.Size> sizes = camera.getParameters().getSupportedPictureSizes();
        final Camera.Size optimal = getOptimalSize(sizes);
        final Camera.Parameters params = camera.getParameters();

        params.setPictureSize(optimal.width, optimal.height);
        camera.setParameters(params);
    }

    /**
     * Returns angle in degrees that taken picture should be rotated by.
     */
    protected int getRotation() {
        final Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, cameraInfo);

        int displayOrientation = (cameraInfo.orientation -
                RotationUtil.getDisplayRotation(getActivity()) + 360) % 360;

        return (displayOrientation + 360) % 360;
    }

    /**
     * Checks if the cooldown have passed. Protection against macaque.
     *
     * @see #setTakePictureFrequency(int)
     */
    protected boolean cooldownOk() {
        return System.currentTimeMillis() > lastPictureTaken + takePictureFrequency;
    }

    private int getBackwardCameraId() {
        for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
            final Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK)
                return i;
        }

        return 0;
    }

    /**
     * Opens a {@link android.hardware.Camera} and passes it into {@link CameraPreview}
     */
    public void startPreview() {
        try {
            openCamera();
            preview.setCamera(camera);
        } catch (Exception e) {
            LogUtil.e(SimpleCameraFragment.class, "Picture taking failed: " + e);
        }
    }

    /**
     * Stops preview and releases {@link android.hardware.Camera}.
     */
    public void stopPreview() {
        if (camera == null) return;

        try {
            preview.stopPreview();
            preview.setCamera(null);

            camera.release();
            camera = null;
        } catch (Exception e) {
            LogUtil.e(SimpleCameraFragment.class, "Picture taking failed: " + e);
        }
    }

    /**
     * Restarts preview. Stops preview correctly, releases resources, than grabs resources back and
     * starts preview again.
     * <p/>
     * <b>NOTE:</b> calling {@link #stopPreview()} and than {@link #startPreview()} is NOT the
     * same and may fail.
     */
    public void restartPreview() {
        stopPreview();

        try {
            openCamera();
            preview.switchCamera(camera);
        } catch (Exception e) {
            LogUtil.e(SimpleCameraFragment.class, "Picture taking failed: " + e);
        }
    }

    /**
     * Checks if photo can be taken at the moment. It could not, because:
     * <ul>
     * <li>cooldown has not passed yet ({@link #setTakePictureFrequency(int)})</li>
     * </ul>
     */
    public boolean canTakePicture() {
        return cooldownOk();
    }

    /**
     * Starts taking photo process. It could fail if {@link #canTakePicture()} returns FALSE.
     *
     * @return TRUE, if taking photo process have actually began
     */
    public boolean takePicture() {
        if (!canTakePicture())
            return false;

        lastPictureTaken = System.currentTimeMillis();
        try {
            camera.takePicture(null, null, this);
            return true;
        } catch (Exception e) {
            LogUtil.e(SimpleCameraFragment.class, "Picture taking failed: " + e);
        }

        return false;
    }
}
