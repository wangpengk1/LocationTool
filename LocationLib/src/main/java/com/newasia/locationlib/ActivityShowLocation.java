package com.newasia.locationlib;
import android.webkit.WebView;
import androidx.databinding.DataBindingUtil;
import com.newasia.locationlib.databinding.ActivityShowLocationBinding;


public class ActivityShowLocation extends BaseLocationActivity {
    private ActivityShowLocationBinding mBinding;

    public static final String OPEN_PARAM_TITLE = "Title";
    public static final String OPEN_PARAM_ADDRESS = "Address";
    public static final String OPEN_PARAM_LAT = "Lat";
    public static final String OPEN_PARAM_LNG = "Lng";

    private double mLat;
    private double mLng;


    @Override
    protected int getLayouResID() {
        return R.layout.activity_show_location;
    }

    @Override
    protected void initDataBingding() {
        mBinding = DataBindingUtil.bind(mRootView);
    }

    @Override
    protected WebView getmMapView() {
        return mBinding.mapview;
    }

    @Override
    protected void initViews()
    {

        mBinding.cancelLabel.setOnClickListener(v -> {finish();});

        mBinding.locationLabel.setOnClickListener(v -> {
            if(mLat!=0.0 && mLng !=0.0)
            {
                callJs(String.format("javascript:moveTo(%s,%s)",mLat,mLng));
            }
        });

        if(getIntent()!=null)
        {
            mBinding.locationAddr.setText( getIntent().getStringExtra(OPEN_PARAM_ADDRESS)==null?"":getIntent().getStringExtra(OPEN_PARAM_ADDRESS) );
            mBinding.locationTitle.setText( getIntent().getStringExtra(OPEN_PARAM_TITLE)==null?"":getIntent().getStringExtra(OPEN_PARAM_TITLE) );
            mLat = getIntent().getDoubleExtra(OPEN_PARAM_LAT,0.0);
            mLng = getIntent().getDoubleExtra(OPEN_PARAM_LNG,0.0);
        }
        initMapView();
        mBinding.mapview.loadUrl("file:///android_asset/map_show.html");
    }



    @Override
    protected void onWebPageFinsied(WebView view, String url) {
        if(mLat!=0.0f && mLng !=0.0f)
        {
            callJs(String.format("javascript:moveTo(%s,%s)",mLat,mLng));
        }
    }
}
