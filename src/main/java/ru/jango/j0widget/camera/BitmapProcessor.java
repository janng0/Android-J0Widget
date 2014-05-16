package ru.jango.j0widget.camera;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.Handler;

import java.net.URI;

import ru.jango.j0util.BmpUtil;
import ru.jango.j0util.LogUtil;
import ru.jango.j0util.PathUtil;

/**
 * Helper class for asynchronous processing images. In constructor should be passed image as byte
 * array, {@link ru.jango.j0widget.camera.BitmapProcessor.BitmapProcessorListener} for receiving
 * result and a {@link java.net.URI} to identify result after processing.
 * <p/>
 * Capabilities:
 * <ul>
 * <li>resize by {@link #setPictureSize(android.graphics.Point)}</li>
 * <li>rotate by {@link #setPictureRotation(int)}</li>
 * <li>create thumbnails sized by {@link #setThumbSize(android.graphics.Point)}</li>
 * </ul>
 */
public class BitmapProcessor implements Runnable {

    private Point picSize;
    private Point thumbSize;
    private int picQuality;
    private int picRotation;

    private byte[] data;
    private URI dataID;

    private BitmapProcessorListener listener;
    private Handler mainTreadHandler;

    public BitmapProcessor(byte[] data, URI dataID, BitmapProcessorListener listener) {
        this.data = data;
        this.dataID = dataID;
        this.listener = listener;

        this.picSize = null;
        this.thumbSize = null;
        this.picQuality = 70;
        this.picRotation = 0;

        mainTreadHandler = new Handler();
    }

    ///////////////////////////////////////////////////////////////
    //
    // 					Getters and setters
    //
    ///////////////////////////////////////////////////////////////

    public byte[] getData() {
        return data;
    }

    public URI getDataIdentifier() {
        return dataID;
    }

    public Point getPictureSize() {
        return picSize;
    }

    public void setPictureSize(Point size) {
        this.picSize = size;
    }

    public Point getThumbSize() {
        return thumbSize;
    }

    public void setThumbSize(Point size) {
        this.thumbSize = size;
    }

    public int getPicQuality() {
        return picQuality;
    }

    public void setPicQuality(int picQuality) {
        this.picQuality = picQuality;
    }

    public int getPictureRotation() {
        return picRotation;
    }

    public void setPictureRotation(int picRotation) {
        this.picRotation = picRotation;
    }

    public BitmapProcessorListener getBitmapProcessorListener() {
        return listener;
    }

    public void setBitmapProcessorListener(BitmapProcessorListener listener) {
        this.listener = listener;
    }

    ///////////////////////////////////////////////////////////////
    //
    // 					Processor staff
    //
    ///////////////////////////////////////////////////////////////

    private CompressFormat findFormat(URI uri) {
        if (PathUtil.getExt(uri).equals("png"))
            return CompressFormat.PNG;

        return CompressFormat.JPEG;
    }

    /**
     * Invokes listener, that bitmap was processed successfully, if the listener was previously set.
     * Method will be called on main thread.
     *
     * @param pic   processed bitmap as byte array
     * @param thumb the same bitmap, but resized to {@link BitmapProcessor#getThumbSize()}
     */
    protected void postProcessingFinished(final byte[] pic, final Bitmap thumb) {
        if (listener == null)
            return;

        mainTreadHandler.post(new Runnable() {
            @Override
            public void run() {
                listener.onProcessingFinished(dataID, pic, thumb);
            }
        });
    }

    /**
     * Invokes listener, that bitmap processing failed, if the listener was previously set.
     * Method will be called on main thread.
     *
     * @param e fail reason
     */
    protected void postProcessingFailed(final Exception e) {
        if (listener == null)
            return;

        mainTreadHandler.post(new Runnable() {
            @Override
            public void run() {
                listener.onProcessingFailed(dataID, e);
            }
        });
    }

    /**
     * Smart picture decoding - checks specified picture size and max Android texture size (2048x2048).
     */
    protected Bitmap decodeData() {
        if (picSize != null)
            return BmpUtil.scale(data, BmpUtil.ScaleType.PROPORTIONAL_FIT, picSize.x, picSize.y);
        else if (BmpUtil.isTooBig(data))
            return BmpUtil.scale(data, BmpUtil.ScaleType.PROPORTIONAL_FIT, BmpUtil.MAX_TEXTURE_SIZE, BmpUtil.MAX_TEXTURE_SIZE);
        else return BitmapFactory.decodeByteArray(data, 0, data.length);
    }

    /**
     * Actually processes picture. Checks max texture size (2048x2048 in Android), scales data if
     * needed and rotates it if needed.
     */
    protected byte[] preparePicture() {
        Bitmap bmp = decodeData();
        if (picRotation != 0) {
            final Bitmap tmp = bmp;
            bmp = BmpUtil.rotate(bmp, BmpUtil.ScaleType.PROPORTIONAL_CROP, picRotation);
            tmp.recycle();
        }

        byte[] pic = BmpUtil.bmpToByte(bmp, findFormat(dataID), picQuality);
        bmp.recycle();

        return pic;
    }

    /**
     * Actually processes thumbnail. Checks specified thumbnail size and does scaling, or returns NULL.
     */
    protected Bitmap prepareThumbnail(byte[] preparedPicture) {
        if (thumbSize != null)
            return BmpUtil.scale(preparedPicture, BmpUtil.ScaleType.PROPORTIONAL_FIT, thumbSize.x, thumbSize.y);
        else return null;
    }

    /**
     * If you want to add more processing features (except scaling and rotating), you could
     * subclass {@link ru.jango.j0widget.camera.BitmapProcessor} and rewrite this method. Actual
     * bitmap processing work is done here.
     */
    protected void doInBackground() {
        final byte[] pic = preparePicture();
        postProcessingFinished(pic, prepareThumbnail(pic));
    }

    @Override
    public void run() {
        try {
            doInBackground();
        } catch (Exception e) {
            postProcessingFailed(e);
            LogUtil.e(BitmapProcessor.class, "Bitmap processing failed: " + e);
        }
    }

    public interface BitmapProcessorListener {

        /**
         * Is called on main thread when the data was successfully processed.
         *
         * @param dataID {@link java.net.URI}, that was passed in {@link BitmapProcessor} constructor
         * @param data   processed bitmap as byte array
         * @param thumb  the same bitmap, but resized to {@link BitmapProcessor#getThumbSize()}
         */
        public void onProcessingFinished(URI dataID, byte[] data, Bitmap thumb);

        /**
         * Is called on main thread when the data processing failed.
         *
         * @param dataID {@link java.net.URI}, that was passed in {@link BitmapProcessor} constructor
         * @param e      fail reason
         */
        public void onProcessingFailed(URI dataID, Exception e);
    }
}
