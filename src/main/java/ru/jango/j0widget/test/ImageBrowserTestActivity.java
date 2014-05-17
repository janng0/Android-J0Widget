package ru.jango.j0widget.test;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

import ru.jango.j0widget.R;
import ru.jango.j0widget.imagebrowser.ImageBrowserView;

public class ImageBrowserTestActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.image_browser_test);

        final ImageBrowserView imgb = (ImageBrowserView) findViewById(R.id.image_browser);
        findViewById(R.id.small_pic).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imgb.setImageResource(R.drawable.small);
            }
        });
        findViewById(R.id.large_pic).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imgb.setImageResource(R.drawable.large);
            }
        });
    }

}
