package ru.jango.j0widget.camera;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import ru.jango.j0widget.camera.BitmapProcessor.BitmapProcessorListener;
import ru.jango.j0util.LogUtil;
import ru.jango.j0util.RotationUtil;

import android.os.Bundle;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.RelativeLayout;

public class CameraFragment extends Fragment implements Camera.PictureCallback, BitmapProcessorListener {
	
	public static final boolean DEFAULT_RESTART_ON_RESUME = true;
	public static final int DEFAULT_TAKE_PICTURE_FREQUENCY = 3000;
	public static final int DEFAULT_PICTURE_WIDTH = 800;
	public static final int DEFAULT_PICTURE_HEIGHT = 600;
	public static final int DEFAULT_MAX_CACHE_SIZE = 5;
	
	protected boolean restartOnResume;
	protected int cameraId;
	protected Camera camera;
	protected CameraPreview preview;
	protected CameraFragmentListener cameraListener;
	
	private int takePictureFrequency;
	private long lastPictureTaken;
	protected Map<URI, byte[]> cache;
	protected int photosCount;
	
	private int maxCacheSize;
	private int picWidth;
	private int picHeight;
	
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
		picWidth = DEFAULT_PICTURE_WIDTH; 
		picHeight = DEFAULT_PICTURE_HEIGHT;
		maxCacheSize = DEFAULT_MAX_CACHE_SIZE;
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
	
	public int getTakePictureFrequency() {
		return takePictureFrequency;
	}

	public void setTakePictureFrequency(int takePictureFrequency) {
		this.takePictureFrequency = takePictureFrequency;
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

	public int getMaxCacheSize() {
		return maxCacheSize;
	}

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
	
	public boolean cacheFull() {
		return cache.size() >= maxCacheSize;
	}
	
	///////////////////////////////////////////////////////////////
	//
	//						Camera staff
	//
	///////////////////////////////////////////////////////////////
	
	/**
	 * Захватывает камеру и передает управление ею в {@link CameraPreview}
	 */
	public void startPreview() {
		try {
			camera = Camera.open(cameraId);
			preview.setCamera(camera);
		} catch (Exception e) {
			if (cameraListener != null) 
				cameraListener.onCameraError(e);
		}
	}

	/**
	 * Тормозит видоискатель, тормозит все остальное, отпускает камеру.
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
	 * Перезапускает превью. Правильно останавливает его, освобождает 
	 * ресурсы и потом заново запускает.
	 */
	public void restartPreview() {
		stopPreview();
		
		try {
			camera = Camera.open(cameraId);
			preview.switchCamera(camera);
		} catch (Exception e) {
			if (cameraListener != null) 
				cameraListener.onCameraError(e);
		}
	}
	
	/**
	 * Проверяет, можно ли делать фотографию в данный момент.
	 * Нельзя может быть по двум причинам: <br>
 	 * - не прошел cooldown <br>
	 * - кэш переполнен 
	 */
	public boolean canTakePhoto() {
		return !cacheFull() && cooldownOk();
	}
	
	/**
	 * Проверка частоты съемки (защита от макаки)
	 */
	public boolean cooldownOk() {
		return System.currentTimeMillis() > lastPictureTaken + takePictureFrequency;
	}
	
	private int getBackwardCameraId() {
		for (int i=0; i<Camera.getNumberOfCameras(); i++) {
			final CameraInfo info = new CameraInfo(); 
			Camera.getCameraInfo(i, info);
			if (info.facing == CameraInfo.CAMERA_FACING_BACK)
				return i;
		}
		
		return 0;
	}
	
	/**
	 * Возвращает угол в градусах, на который нужно повернуть картинку в данный момент. 
	 */
	private int getRotation() {
		final CameraInfo cameraInfo = new CameraInfo();
		Camera.getCameraInfo(cameraId, cameraInfo);

		int displayOrientation = (cameraInfo.orientation - 
				RotationUtil.getDisplayRotation(getActivity()) + 360) % 360;
		
		return (displayOrientation + 360) % 360;
	}
	
	private void processBitmap(URI dataID, byte[] data) {
		final BitmapProcessor bmpProc = new BitmapProcessor(data, dataID, this);
		bmpProc.setPictureRotation(getRotation());
		bmpProc.setPictureWidth(picWidth);
		bmpProc.setPictureHeight(picHeight);
		
		new Thread(bmpProc).start();
	}

	/**
	 * Запускает процесс создания фотографии. Процесс может не начаться по 
	 * двум причинам: <br>
	 * - не прошел cooldown <br>
	 * - кэш переполнен 
	 * 
	 * @return TRUE, если процесс фотографирования реально начался
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
		}
		catch(Exception e) { LogUtil.e(CameraFragment.class, "take picture fail: " + e); }
		
		return false;
	}

	@Override
	public void onPictureTaken(byte[] data, final Camera camera) {
		if (cameraListener != null)
			processBitmap(cameraListener.onPictureTaken(), data);
		
		restartPreview();
	}

	@Override
	public void bitmapProcessed(URI dataID, byte[] data, Bitmap thumb) {
		cache.put(dataID, data);
		photosCount++;

		if (cameraListener != null)
			cameraListener.onPictureProcessed(dataID, data);
	}

	public interface CameraFragmentListener {

		/**
		 * Вызывается сразу после того, как фотографий была сделана; до того, 
		 * как передать ее на обработку в {@link BitmapProcessor}.
		 * 
		 * @return {@link java.net.URI}, с которой в дальнейшем будет ассоциироваться
		 * 			сделанная фотография; эта же {@link java.net.URI} будет передана в
		 * 			{@link #onPictureProcessed(java.net.URI, byte[])}
		 */
		public URI onPictureTaken();
		
		/**
		 * Вызывается после преобразования фотографии в нужный размер.
		 * 
		 * @param dataID	{@link java.net.URI}, с которым ассоциируется фотография; то,
		 * 					что было возвращено из {@link #onPictureTaken()} 
		 * @param data		преобразованная фотография
		 */
		public void onPictureProcessed(URI dataID, byte[] data);

		/**
		 * Вызывается при ошибках.
		 * 
		 * @param e	подкласс от {@link Exception} с ошибкой
		 */
		public void onCameraError(Exception e);

	}
}
