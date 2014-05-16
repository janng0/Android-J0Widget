package ru.jango.j0widget.camera;

import java.net.URI;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Handler;

import ru.jango.j0util.BmpUtil;
import ru.jango.j0util.LogUtil;
import ru.jango.j0util.PathUtil;

/**
 * Вспомогательный класс-обработчик картинок. Должен работать в отдельном потоке. 
 * Получает в конструктор картинку в виде массива байт и слушателя, куда будет 
 * отослан результат. Так же в конструкторе указывается {@link java.net.URI} для идентификации
 * данных (эта же {@link java.net.URI} будет отослана слушателю после обработки).
 * <br><br>
 * Умеет: <br>
 * - изменять размер картинки ({@link #setPictureWidth(int)} и {@link #setPictureHeight(int)}) <br>
 * - крутить картинку ({@link #setPictureRotation(int)}) <br>
 * - делать превью картинки ({@link #setThumbWidth(int)} и {@link #setThumbHeight(int)})
 */
public class BitmapProcessor implements Runnable {
	
	public static final int DEFAULT_PICTURE_WIDTH = 1280;
	public static final int DEFAULT_PICTURE_HEIGHT = 1024;
	public static final int DEFAULT_PICTURE_QUALITY = 70;
	public static final int DEFAULT_THUMB_WIDTH = 50;
	public static final int DEFAULT_THUMB_HEIGHT = 50;
	public static final int DEFAULT_PICTURE_ROTATION = 0;
	
	private int picWidth;
	private int picHeight;
	private int picQuality;
	private int thumbWidth;
	private int thumbHeight;
	private int picRotation;

	private byte[] data;
	private URI dataID;
	private CompressFormat format;

	private BitmapProcessorListener listener;
	private Handler mainTreadHandler;
	
	public BitmapProcessor(byte[] data, URI dataID, BitmapProcessorListener listener) {
		this.data = data;
		this.dataID = dataID;
		this.listener = listener;
		this.format = findFormat(dataID);
		
		this.picWidth = DEFAULT_PICTURE_WIDTH;
		this.picHeight = DEFAULT_PICTURE_HEIGHT;
		this.picQuality = DEFAULT_PICTURE_QUALITY;
		this.thumbWidth = DEFAULT_THUMB_WIDTH;
		this.thumbHeight = DEFAULT_THUMB_HEIGHT;
		this.picRotation = DEFAULT_PICTURE_ROTATION;
		
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
	 * Возвращает кол-во раз, в которое нужно скукожить картинку, чтобы она
	 * влезла в размеры picWidth*picHeight. Причем картинка задается массивом
	 * байт, а не Bitmap.
	 */
	private int getScale() {
		final BitmapFactory.Options options = new BitmapFactory.Options();
	    options.inJustDecodeBounds = true;
	    BitmapFactory.decodeByteArray(data, 0, data.length, options);
	    
	    final int vScale = Math.round(options.outHeight/picHeight + 0.5f);
	    final int hScale = Math.round(options.outWidth/picWidth + 0.5f);
	    
	    return Math.max(1, Math.max(vScale, hScale));
	 	}

	/**
	 * Крутит заданную картинку на picRotation градусов.
	 */
	private Bitmap rotateBmp(Bitmap bmp) {
		if (picRotation == 0)
			return bmp;
		
		final Matrix matrix = new Matrix();
		matrix.postRotate(picRotation);

		final Bitmap newBmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(),
				bmp.getHeight(), matrix, false);

		bmp.recycle();
		return newBmp;
	}
	
	/**
	 * Скукоживает заданную картинку до размеров thumbWidth*thumbHeight,
	 * сохраняя пропорции.
	 */
	private Bitmap createThumb(Bitmap src) {
	    final int vScale = Math.round(src.getHeight()/thumbHeight + 0.5f);
	    final int hScale = Math.round(src.getWidth()/thumbWidth + 0.5f);
	    final int scale = Math.max(1, Math.max(vScale, hScale));
	    
	    return Bitmap.createScaledBitmap(src, src.getWidth()/scale, src.getHeight()/scale, false);
	}
	
	private void doProcess() {
		final BitmapFactory.Options options = new BitmapFactory.Options();
	    options.inSampleSize = getScale();
		
		final Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length, options);
		final Bitmap rotatedBmp = rotateBmp(bmp);
		
		final byte[] pic = BmpUtil.bmpToByte(rotatedBmp, format, picQuality);
		final Bitmap thumb = createThumb(rotatedBmp);
		
		bmp.recycle();
		rotatedBmp.recycle();
		
		mainTreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (listener != null)
                    listener.bitmapProcessed(dataID, pic, thumb);
            }
        });
	}
	
	@Override
	public void run() {
		try { doProcess(); }
		catch(Exception e) {
			LogUtil.e(BitmapProcessor.class, "Bitmap processing failed: " + e);
		}
	}
	
	public interface BitmapProcessorListener {
		
		/**
		 * Вызывается в главном потоке, когда картинка обработана.
		 * 
		 * @param dataID	{@link java.net.URI}, которая передавалась в конструкторе {@link BitmapProcessor}
		 * @param data		массив байт, в котором лежит преобразованная картинка
		 * @param thumb		исходная картинка, скукоженная до {@link BitmapProcessor#getThumbWidth()}*
		 * 					{@link BitmapProcessor#getThumbHeight()}
		 */
		public void bitmapProcessed(URI dataID, byte[] data, Bitmap thumb);
	}
}
