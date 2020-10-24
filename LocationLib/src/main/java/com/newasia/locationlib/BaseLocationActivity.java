package com.newasia.locationlib;

import android.Manifest;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.GeolocationPermissions;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.xuexiang.xutil.XUtil;
import com.xuexiang.xutil.common.StringUtils;
import com.xuexiang.xutil.net.NetworkUtils;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public abstract class BaseLocationActivity  extends AppCompatActivity
{
    private static final int REQUEST_PERMISSION_CODE = 1025;
    protected View mRootView;
    protected WebView mMapView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        XUtil.init(this);
        if(getSupportActionBar()!=null) getSupportActionBar().hide();
        setStatusBarColor(Color.parseColor("#40000000"));
        setContentView(getLayouResID());
        mRootView = ((ViewGroup)findViewById(android.R.id.content)).getChildAt(0);
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

        initDataBingding();
        mMapView = getmMapView();
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


    protected void initMapView()
    {

        WebSettings webSettings = mMapView.getSettings();

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
    }


    private void enableLoactionAssist()
    {
        mMapView.setWebChromeClient(new WebChromeClient() {
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


        mMapView.setWebViewClient(new WebViewClient(){
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                onWebPageFinsied(view,url);
            }
        });


    }


    protected void callJs(String script) {
        if(StringUtils.isEmpty(script)) return;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            mMapView.loadUrl(script);
        } else {
            mMapView.evaluateJavascript(script, new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String value) {
                    //此处为 js 返回的结果
                }
            });

        }
    }





    protected void onWebPageFinsied(WebView view,String url)
    {

    }



    protected abstract void initViews();

    protected abstract  int getLayouResID();
    protected abstract  void  initDataBingding();
    protected abstract WebView getmMapView();

}
