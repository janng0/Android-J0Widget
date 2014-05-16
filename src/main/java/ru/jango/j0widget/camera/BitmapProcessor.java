package ru.jango.j0widget.camera;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
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
 * <li>resize by {@link #setPictureWidth(int)} and {@link #setPictureHeight(int)}</li>
 * <li>rotate by {@link #setPictureRotation(int)}</li>
 * <li>create thumbnails sized by {@link #setThumbWidth(int)} and {@link #setThumbHeight(int)}</li>
 * </ul>
 */
public class BitmapProcessor implements Runnable {

    public static final int DEFAULT_PICTURE_WIDTH = 1280;
    public static final int DEFAULT_PICTURE_HEIGHT = 1024;
    public static final int DEFAULT_PICTURE_QUALITY = 70;

    private int picWidth;
    private int picHeight;
    private int picQuality;
    private int thumbWidth;
    private int thumbHeight;
    private int picRotation;

    private byte[] data;
    private URI dataID;

    private BitmapProcessorListener listener;
    private Handler mainTreadHandler;

    public BitmapProcessor(byte[] data, URI dataID, BitmapProcessorListener listener) {
        this.data = data;
        this.dataID = dataID;
        this.listener = listener;

        this.picWidth = DEFAULT_PICTURE_WIDTH;
        this.picHeight = DEFAULT_PICTURE_HEIGHT;
        this.picQuality = DEFAULT_PICTURE_QUALITY;
        this.thumbWidth = -1;
        this.thumbHeight = -1;
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

    public int getPictureWidth() {
        return picWidth;
    }

    public void setPictureWidth(int picWidth) {
        this.picWidth = picWidth;
    }

    public int getPictureHeight() {
        return picHeight;
    }

    public void setPictureHeight(int picHeight) {
        this.picHeight = picHeight;
    }

    public int getPicQuality() {
        return picQuality;
    }

    public void setPicQuality(int picQuality) {
        this.picQuality = picQuality;
    }

    public int getThumbWidth() {
        return thumbWidth;
    }

    public void setThumbWidth(int thumbWidth) {
        this.thumbWidth = thumbWidth;
    }

    public int getThumbHeight() {
        return thumbHeight;
    }

    public void setThumbHeight(int thumbHeight) {
        this.thumbHeight = thumbHeight;
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
     * @param thumb the same bitmap, but resized to {@link BitmapProcessor#getThumbWidth()}x
     *              {@link BitmapProcessor#getThumbHeight()}
     */
    protected void postProcessingFinished(final byte[] pic, final Bitmap thumb) {
        if (listener == null)
            return;

        mainTreadHandler.post(new Runnable() {
            @Override
            public void run() {
                listener.processingFinished(dataID, pic, thumb);
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
                listener.processingFailed(dataID, e);
            }
        });
    }

    /**
     * If you want to add more processing features (except scaling and rotating), you could
     * subclass {@link ru.jango.j0widget.camera.BitmapProcessor} and rewrite this method. Actual
     * bitmap processing work is done here.
     */
    protected void doInBackground() {
        Bitmap bmp = BmpUtil.scale(data, BmpUtil.ScaleType.PROPORTIONAL_FIT, picWidth, picHeight);
        if (picRotation != 0) {
            final Bitmap tmp = bmp;
            bmp = BmpUtil.rotate(bmp, BmpUtil.ScaleType.PROPORTIONAL_CROP, picRotation);
            tmp.recycle();
        }

        final byte[] pic = BmpUtil.bmpToByte(bmp, findFormat(dataID), picQuality);
        bmp.recycle();

        Bitmap thumb = null;
        if (thumbHeight > 0 && thumbWidth > 0)
            thumb = BmpUtil.scale(pic, BmpUtil.ScaleType.PROPORTIONAL_FIT, thumbWidth, thumbHeight);

        postProcessingFinished(pic, thumb);
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
         * @param thumb  the same bitmap, but resized to {@link BitmapProcessor#getThumbWidth()}x
         *               {@link BitmapProcessor#getThumbHeight()}
         */
        public void processingFinished(URI dataID, byte[] data, Bitmap thumb);

        /**
         * Is called on main thread when the data processing failed.
         *
         * @param dataID {@link java.net.URI}, that was passed in {@link BitmapProcessor} constructor
         * @param e      fail reason
         */
        public void processingFailed(URI dataID, Exception e);
    }
}
