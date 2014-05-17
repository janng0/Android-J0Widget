package ru.jango.j0widget.test;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.FragmentActivity;
import android.view.View;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;

import ru.jango.j0util.LogUtil;
import ru.jango.j0util.PathUtil;
import ru.jango.j0widget.R;
import ru.jango.j0widget.camera.fragment.CameraFragment;

public class CameraTestActivity extends FragmentActivity implements CameraFragment.CameraFragmentListener {

    private static final String TEST_DIR = "J0Widget_camera_test";

    private File testDir;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_test);

        testDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), TEST_DIR);
        LogUtil.d(CameraTestActivity.class, "Directory '" + testDir + "' for test files created: " + testDir.mkdir());

        final CameraFragment fragment = (CameraFragment) getSupportFragmentManager().findFragmentById(R.id.camera_fragment);
        fragment.setCameraFragmentListener(this);
        fragment.setPictureSize(new Point(1024, 768)); // just in case
        fragment.setThumbnailSize(new Point(50, 50)); // by default thumbnails would not be created - their size is set to (-1,-1)
        fragment.getView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean processStarted = fragment.takePicture();
                LogUtil.d(CameraTestActivity.class, "Taking picture process started: " + processStarted);
            }
        });
    }

    @Override
    public URI onPictureTaken() {
        final URI testURI = URI.create("file://" + testDir.getAbsolutePath() + "/" + System.currentTimeMillis() + ".jpg");
        LogUtil.d(CameraTestActivity.class, "onPictureTaken returns URI: " + testURI);
        return testURI;
    }

    @Override
    public void onProcessingFinished(URI dataID, byte[] data, Bitmap thumbnail) {
        try {
            final File testFile = new File(dataID.getPath());
            //noinspection ResultOfMethodCallIgnored
            testFile.createNewFile();

            final FileOutputStream fos = new FileOutputStream(testFile);
            fos.write(data);
            fos.flush();
            fos.close();

            LogUtil.d(CameraTestActivity.class, "onProcessingFinished data saved into file: " + dataID);
            LogUtil.d(CameraTestActivity.class, "onProcessingFinished thumbnail: " + thumbnail);

            if (thumbnail != null) {
                final File thumbnailFile = new File(testFile.getAbsolutePath().replace(".jpg", "_thumbnail.jpg"));
                final FileOutputStream tfos = new FileOutputStream(thumbnailFile);
                thumbnail.compress(Bitmap.CompressFormat.PNG, 100, tfos);
                tfos.close();

                LogUtil.d(CameraTestActivity.class, "onProcessingFinished thumbnail compressed as: " + thumbnailFile);
            }
        } catch (Exception e) {
            LogUtil.d(CameraTestActivity.class, "onProcessingFinished failed saving file: " + e);
            e.printStackTrace();
        }
    }

    @Override
    public void onProcessingFailed(URI dataID, Exception e) {
        LogUtil.d(CameraTestActivity.class, "onProcessingFailed: " + e);
        e.printStackTrace();
    }

    @Override
    public void onCameraError(Exception e) {
        LogUtil.d(CameraTestActivity.class, "onCameraError: " + e);
        e.printStackTrace();
    }
}
