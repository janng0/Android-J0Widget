package ru.jango.j0widget.test;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import ru.jango.j0widget.R;
import ru.jango.j0widget.camera.fragment.CameraFragment;

public class CameraTestActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_test);
    }

}
