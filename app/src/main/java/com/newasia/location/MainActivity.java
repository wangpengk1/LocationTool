package com.newasia.location;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.newasia.locationlib.ActivityLocationGrabber;
import com.newasia.locationlib.ActivityShowLocation;
import com.newasia.locationlib.PoiItem;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        findViewById(R.id.center_label).setOnClickListener(v -> {
//            Bitmap bitmap = createViewSnapshot(findViewById(R.id.test_image));
//            ImageView imageView = findViewById(R.id.show_image);
//            imageView.setImageBitmap(bitmap);

            Intent intent = new Intent(this, ActivityLocationGrabber.class);
            startActivityForResult(intent,ActivityLocationGrabber.REQUEST_LOCATION_OPEN);


        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==ActivityLocationGrabber.REQUEST_LOCATION_OPEN && resultCode==ActivityLocationGrabber.REQUEST_LOCATION_RESULT)
        {
            PoiItem item = (PoiItem)data.getParcelableExtra(ActivityLocationGrabber.RETURN_RESULT_PARAM);
            if(ActivityLocationGrabber.s_mapBitmap!=null)
            {
                ImageView imageView = findViewById(R.id.show_image);
                imageView.setImageBitmap(ActivityLocationGrabber.s_mapBitmap);
            }

            Intent intent = new Intent(this, ActivityShowLocation.class);
            intent.putExtra(ActivityShowLocation.OPEN_PARAM_TITLE,item.title);
            intent.putExtra(ActivityShowLocation.OPEN_PARAM_ADDRESS,item.addr);
            intent.putExtra(ActivityShowLocation.OPEN_PARAM_LAT,item.lat);
            intent.putExtra(ActivityShowLocation.OPEN_PARAM_LNG,item.lng);
            Log.e("test",item.lat+" "+item.lng);
            startActivity(intent);

        }
    }

    private Bitmap createViewSnapshot(View view) {
        if (view == null) {
            return null;
        }
        Bitmap screenshot;
        screenshot = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_4444);
        Canvas c = new Canvas(screenshot);
        c.translate(-view.getScrollX(), -view.getScrollY());
        view.draw(c);
        return screenshot;
    }
}