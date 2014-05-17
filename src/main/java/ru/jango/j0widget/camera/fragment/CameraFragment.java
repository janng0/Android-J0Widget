package ru.jango.j0widget.camera.fragment;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.RelativeLayout;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.jango.j0util.LogUtil;
import ru.jango.j0util.RotationUtil;
import ru.jango.j0widget.camera.BitmapProcessor;
import ru.jango.j0widget.camera.BitmapProcessor.BitmapProcessorListener;
import ru.jango.j0widget.camera.CameraPreview;

public class CameraFragment extends Fragment implements Camera.PictureCallback, BitmapProcessorListener {

    public static final boolean DEFAULT_RESTART_ON_RESUME = true;
    public static final int DEFAULT_TAKE_PICTURE_FREQUENCY = 3000;
    public static final int DEFAULT_MAX_CACHE_SIZE = 5;
    public static final Point DEFAULT_PICTURE_SIZE = new Point(800, 600);

    protected boolean restartOnResume;
    protected int cameraId;
    protected Camera camera;
    protected CameraPreview preview;
    protected CameraFragmentListener cameraListener;

    private int takePictureFrequency;
    private long lastPictureTaken;
    protected Map<URI, byte[]> cache;
    protected int photosCount;

    private Point picSize;
    private Point thumbSize;
    private int maxCacheSize;

    protected RelativeLayout root;

    public CameraFragment() {
        initVars();
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
        layout.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
        layout.setBackgroundColor(Color.BLACK);

        return layout;
    }

    private CameraPreview createCameraPreview() {
        final CameraPreview preview = new CameraPreview(getActivity());
        preview.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));

        return preview;
    }

    private void initVars() {
        cameraId = getBackwardCameraId();
        cache = new HashMap<URI, byte[]>();
        lastPictureTaken = 0;
        photosCount = 0;

        restartOnResume = DEFAULT_RESTART_ON_RESUME;
        takePictureFrequency = DEFAULT_TAKE_PICTURE_FREQUENCY;
        maxCacheSize = DEFAULT_MAX_CACHE_SIZE;
        picSize = DEFAULT_PICTURE_SIZE;
        thumbSize = null;
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

    public void setCameraFragmentListener(CameraFragmentListener listener) {
        this.cameraListener = listener;
    }

    public CameraFragmentListener getCameraFragmentListener() {
        return cameraListener;
    }

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
     * {@link CameraFragment.CameraFragmentListener#onProcessingFinished(java.net.URI, byte[], android.graphics.Bitmap)}.
     */
    public Point getPictureSize() {
        return picSize;
    }

    /**
     * After taking photo, it would be automatically rotated and scaled down and then passed into
     * {@link CameraFragment.CameraFragmentListener#onProcessingFinished(java.net.URI, byte[], android.graphics.Bitmap)}.
     */
    public void setPictureSize(Point size) {
        this.picSize = size;
    }

    /**
     * After taking photo, it could be automatically thumbnailed and then passed into
     * {@link CameraFragment.CameraFragmentListener#onProcessingFinished(java.net.URI, byte[], android.graphics.Bitmap)}.
     * If thumbnail size is not specified - no thumbnail would be created.
     */
    public Point getThumbSize() {
        return thumbSize;
    }

    /**
     * After taking photo, it could be automatically thumbnailed and then passed into
     * {@link CameraFragment.CameraFragmentListener#onProcessingFinished(java.net.URI, byte[], android.graphics.Bitmap)}.
     * If thumbnail size is not specified - no thumbnail would be created.
     */
    public void setThumbSize(Point size) {
        this.thumbSize = size;
    }

    /**
     * Returns max photos count, that could be cached. NOT sum of their sizes - just count.
     */
    public int getMaxCacheSize() {
        return maxCacheSize;
    }

    /**
     * Sets max photos count, that could be cached. NOT sum of their sizes - just count.
     */
    public void setMaxCacheSize(int maxCacheSize) {
        this.maxCacheSize = maxCacheSize;
    }

    public boolean shouldRestartOnResume() {
        return restartOnResume;
    }

    public void setRestartOnResume(boolean restartOnResume) {
        this.restartOnResume = restartOnResume;
    }

    ///////////////////////////////////////////////////////////////
    //
    //						Cache staff
    //
    ///////////////////////////////////////////////////////////////

    public byte[] getCachedData(URI uri) {
        return cache.get(uri);
    }

    public int getCacheSize() {
        return cache.size();
    }

    public void clearCache() {
        cache.clear();
        photosCount = 0;
    }

    public void removeFromCache(URI uri) {
        if (cache.remove(uri) != null)
            photosCount--;
    }

    public boolean isCacheFull() {
        return cache.size() >= maxCacheSize;
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
     * Opens a {@link android.hardware.Camera} and passes it into {@link CameraPreview}
     */
    public void startPreview() {
        try {
            openCamera();
            preview.setCamera(camera);
        } catch (Exception e) {
            if (cameraListener != null)
                cameraListener.onCameraError(e);
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
            if (cameraListener != null)
                cameraListener.onCameraError(e);
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
            if (cameraListener != null)
                cameraListener.onCameraError(e);
        }
    }

    /**
     * Checks if photo can be taken at the moment. It could not, because:
     * <ul>
     * <li>cooldown has not passed yet ({@link #setTakePictureFrequency(int)})</li>
     * <li>cache is full ({@link #setMaxCacheSize(int)})</li>
     * </ul>
     */
    public boolean canTakePhoto() {
        return !isCacheFull() && cooldownOk();
    }

    /**
     * Checks if the cooldown have passed. Protection against macaque.
     *
     * @see #setTakePictureFrequency(int)
     */
    public boolean cooldownOk() {
        return System.currentTimeMillis() > lastPictureTaken + takePictureFrequency;
    }

    private int getBackwardCameraId() {
        for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
            final CameraInfo info = new CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == CameraInfo.CAMERA_FACING_BACK)
                return i;
        }

        return 0;
    }

    /**
     * Returns angle in degrees that taken picture should be rotated by.
     */
    protected int getRotation() {
        final CameraInfo cameraInfo = new CameraInfo();
        Camera.getCameraInfo(cameraId, cameraInfo);

        int displayOrientation = (cameraInfo.orientation -
                RotationUtil.getDisplayRotation(getActivity()) + 360) % 360;

        return (displayOrientation + 360) % 360;
    }

    private void processBitmap(URI dataID, byte[] data) {
        final BitmapProcessor bmpProc = new BitmapProcessor(data, dataID, this);
        bmpProc.setPictureRotation(getRotation());
        bmpProc.setPictureSize(picSize);
        bmpProc.setThumbSize(thumbSize);

        new Thread(bmpProc).start();
    }

    /**
     * Starts taking photo process. It could fail if:
     * <ul>
     * <li>cooldown has not passed yet ({@link #setTakePictureFrequency(int)})</li>
     * <li>cache is full ({@link #setMaxCacheSize(int)})</li>
     * </ul>
     *
     * @return TRUE, if taking photo process have actually began
     *
     * @see #getTakePictureFrequency()
     * @see #getMaxCacheSize()
     * @see #getCacheSize()
     */
    public boolean takePicture() {
        if (!canTakePhoto())
            return false;

        lastPictureTaken = System.currentTimeMillis();
        try {
            camera.takePicture(null, null, this);
            return true;
        } catch (Exception e) {
            LogUtil.e(CameraFragment.class, "Picture taking failed: " + e);
        }

        return false;
    }

    @Override
    public void onPictureTaken(byte[] data, final Camera camera) {
        if (cameraListener != null)
            processBitmap(cameraListener.onPictureTaken(), data);

        restartPreview();
    }

    @Override
    public void onProcessingFinished(URI dataID, byte[] data, Bitmap thumb) {
        cache.put(dataID, data);
        photosCount++;

        if (cameraListener != null)
            cameraListener.onProcessingFinished(dataID, data, thumb);
    }

    @Override
    public void onProcessingFailed(URI dataID, Exception e) {
        if (cameraListener != null)
            cameraListener.onProcessingFailed(dataID, e);
    }

    public interface CameraFragmentListener {

        /**
         * Called when Camera API has finished it's work. That is, photo has been created,
         * but has not been passed for processing into {@link ru.jango.j0widget.camera.BitmapProcessor}.
         *
         * @return  {@link java.net.URI}, that would be treated as photo ID in future - it would be
         * passed into {@link #onProcessingFinished(java.net.URI, byte[], android.graphics.Bitmap)} and
         * {@link #onProcessingFailed(java.net.URI, Exception)}.
         */
        public URI onPictureTaken();

        /**
         * Called when processing was successfully finished.
         *
         * @param dataID    {@link java.net.URI} aka photo ID; this object was previously returned
         *                   from {@link #onPictureTaken()}
         * @param data      transformed photo as byte array
         * @param thumb     small thumbnail of the photo, that could be actually shown on the screen
         *
         * @see #setThumbSize(android.graphics.Point)
         */
        public void onProcessingFinished(URI dataID, byte[] data, Bitmap thumb);

        /**
         * Called when processing was stopped due to some error.
         *
         * @param dataID    {@link java.net.URI} aka photo ID; this object was previously returned
         *                   from {@link #onPictureTaken()}
         * @param e         fail reason
         */
        public void onProcessingFailed(URI dataID, Exception e);

        /**
         * Called when some error occurred while manipulating the {@link android.hardware.Camera}.
         *
         * @param e подкласс от {@link Exception} с ошибкой
         */
        public void onCameraError(Exception e);

    }
}
