package ru.jango.j0widget.camera.fragment;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.hardware.Camera;

import java.net.URI;

import ru.jango.j0widget.camera.BitmapProcessor;
import ru.jango.j0widget.camera.BitmapProcessor.BitmapProcessorListener;

/**
 * Special camera fragment, that also applies some asynchronous checks and changes after taking
 * pictures. Fragment could be used, if made photo basically will be used in interface and doesn't
 * required to be extremely huge (that is, less than 2048x2048 px).
 */
public class SimpleCameraFragment extends AbstractCameraFragment implements BitmapProcessorListener {

    public static final int DEFAULT_MAX_CACHE_SIZE = 5;

    protected CameraFragmentListener cameraListener;
    private Point thumbnailSize;

    public SimpleCameraFragment() {
        thumbnailSize = null;
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
     * After taking photo, it could be automatically thumbnailed and then passed into
     * {@link SimpleCameraFragment.CameraFragmentListener#onProcessingFinished(java.net.URI, byte[], android.graphics.Bitmap)}.
     * If thumbnail size is not specified - no thumbnail would be created.
     */
    public Point getThumbnailSize() {
        return thumbnailSize;
    }

    /**
     * After taking photo, it could be automatically thumbnailed and then passed into
     * {@link SimpleCameraFragment.CameraFragmentListener#onProcessingFinished(java.net.URI, byte[], android.graphics.Bitmap)}.
     * If thumbnail size is not specified - no thumbnail would be created.
     */
    public void setThumbnailSize(Point size) {
        this.thumbnailSize = size;
    }

    ///////////////////////////////////////////////////////////////
    //
    //						Camera staff
    //
    ///////////////////////////////////////////////////////////////

    private void processBitmap(URI dataID, byte[] data) {
        if (dataID == null || data == null)
            return;

        final BitmapProcessor bmpProc = new BitmapProcessor(data, dataID, this);
        bmpProc.setPictureRotation(getRotation());
        bmpProc.setPictureSize(getPictureSize());
        bmpProc.setThumbnailSize(thumbnailSize);

        new Thread(bmpProc).start();
    }

    @Override
    public void onPictureTaken(byte[] data, final Camera camera) {
        if (cameraListener != null)
            processBitmap(cameraListener.onPictureTaken(), data);

        restartPreview();
    }

    @Override
    public void onProcessingFinished(URI dataID, byte[] data, Bitmap thumbnail) {
        if (cameraListener != null)
            cameraListener.onProcessingFinished(dataID, data, thumbnail);
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
         * @param dataID        {@link java.net.URI} aka photo ID; this object was previously returned
         *                      from {@link #onPictureTaken()}
         * @param data          transformed photo as byte array
         * @param thumbnail     small thumbnail of the photo, that could be actually shown on the screen
         *
         * @see #setThumbnailSize(android.graphics.Point)
         */
        public void onProcessingFinished(URI dataID, byte[] data, Bitmap thumbnail);

        /**
         * Called when processing was stopped due to some error.
         *
         * @param dataID    {@link java.net.URI} aka photo ID; this object was previously returned
         *                   from {@link #onPictureTaken()}
         * @param e         fail reason
         */
        public void onProcessingFailed(URI dataID, Exception e);
    }
}
