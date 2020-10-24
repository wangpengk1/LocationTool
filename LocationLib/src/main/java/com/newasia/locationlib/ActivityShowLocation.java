package com.newasia.locationlib;

import android.Manifest;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.GeolocationPermissions;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;

import com.newasia.locationlib.databinding.ActivityShowLocationBinding;
import com.xuexiang.xutil.common.StringUtils;
import com.xuexiang.xutil.net.NetworkUtils;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class ActivityShowLocation extends AppCompatActivity {
    private static final int REQUEST_PERMISSION_CODE = 1025;
    private ActivityShowLocationBinding mBinding;

    public static final String OPEN_PARAM_TITLE = "Title";
    public static final String OPEN_PARAM_ADDRESS = "Address";
    public static final String OPEN_PARAM_LAT = "Lat";
    public static final String OPEN_PARAM_LNG = "Lng";

    private double mLat;
    private double mLng;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getSupportActionBar()!=null) getSupportActionBar().hide();
        setStatusBarColor(Color.parseColor("#40000000"));
        mBinding = DataBindingUtil.setContentView(this,R.layout.activity_show_location);
        checkPermissions();
    }

    public  void setStatusBarColor(int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(color);
        }
    }


    private void checkPermissions()
    {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!=PERMISSION_GRANTED)
        {
            requestPermissions();
            return;
        }
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PERMISSION_GRANTED)
        {
            requestPermissions();
            return;
        }
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)!=PERMISSION_GRANTED)
        {
            requestPermissions();
            return;
        }
        initViews();
    }

    private void requestPermissions()
    {
        ActivityCompat.requestPermissions(this,new String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        },REQUEST_PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode==REQUEST_PERMISSION_CODE)
        {
            checkPermissions();
        }
    }


    private void initViews()
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
    }

    private void initMapView()
    {

        WebSettings webSettings = mBinding.mapview.getSettings();

        if (NetworkUtils.isHaveInternet()) {
            webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        } else {
            webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        }
        webSettings.setJavaScriptEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setAppCacheEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setSavePassword(true);
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NARROW_COLUMNS);
        webSettings.setUseWideViewPort(true);
        webSettings.setBlockNetworkImage(false);

        enableLoactionAssist();

        mBinding.mapview.loadUrl("file:///android_asset/map_show.html");
    }


    private void enableLoactionAssist()
    {
        mBinding.mapview.setWebChromeClient(new WebChromeClient() {
            // 处理定位权限请求
            @Override
            public void onGeolocationPermissionsShowPrompt(String origin,
                                                           GeolocationPermissions.Callback callback) {
                callback.invoke(origin, true, false);
                super.onGeolocationPermissionsShowPrompt(origin, callback);
            }
            @Override
            // 设置网页加载的进度条
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
            }
            // 设置应用程序的标题title
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
            }

        });

        mBinding.mapview.setWebViewClient(new WebViewClient(){
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if(mLat!=0.0f && mLng !=0.0f)
                {
                    callJs(String.format("javascript:moveTo(%s,%s)",mLat,mLng));
                }
            }
        });
    }


    private void callJs(String script) {
        if(StringUtils.isEmpty(script)) return;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            mBinding.mapview.loadUrl(script);
        } else {
            mBinding.mapview.evaluateJavascript(script, new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String value) {
                    //此处为 js 返回的结果
                }
            });

        }
    }
}
