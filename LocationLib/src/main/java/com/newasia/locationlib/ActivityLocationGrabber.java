package com.newasia.locationlib;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.LinearLayout;

import com.bumptech.glide.Glide;
import com.newasia.locationlib.databinding.ActivityLocationGrabberBinding;
import com.xuexiang.xutil.XUtil;
import com.xuexiang.xutil.net.NetworkUtils;
import com.xuexiang.xutil.system.KeyboardUtils;

import org.json.JSONArray;
import org.json.JSONException;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class ActivityLocationGrabber extends AppCompatActivity {

    private ActivityLocationGrabberBinding mBinding;
    private JsBridgeInterface mJsBridge = new JsBridgeInterface();
    private PoiAdapter mPoiAdapter;
    private GestureDetector mGesture;

    private Boolean bScrolling = false;

    public static final int REQUEST_PERMISSION_CODE = 1025;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        XUtil.init(this);
        getSupportActionBar().hide();
        setStatusBarTranslucent();
        mBinding = DataBindingUtil.setContentView(this,R.layout.activity_location_grabber);
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

    public void setStatusBarTranslucent()
    {
        WindowManager.LayoutParams localLayoutParams = getWindow().getAttributes();
        localLayoutParams.flags = (WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS | localLayoutParams.flags);
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
        mGesture = new GestureDetector(this,mGestureListener);
        mBinding.poiList.setOnTouchListener((v, event) -> {
            return mGesture.onTouchEvent(event);
        });

        KeyboardUtils.registerSoftInputChangedListener(this,height -> {
            if(height>10)
            {
                shrinkMapArea();
            }
        });

        //如果当前不是搜索地点模式，则跳转到搜索地点模式
        mBinding.searchLayout.setOnClickListener(v -> {
            if(!mBinding.searchEdit.isEnabled())
            {
                mBinding.searchLayout.setGravity(Gravity.LEFT);
                mBinding.searchEdit.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                mBinding.searchEdit.setEnabled(true);
                shrinkMapArea();
                KeyboardUtils.showSoftInput(mBinding.searchEdit);
            }
        });


        //关闭搜索地点模式
        mBinding.downLabel.setOnClickListener(v -> {
            mBinding.searchEdit.setText("");
            mBinding.searchLayout.setGravity(Gravity.CENTER);
            mBinding.searchEdit.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
            mBinding.searchEdit.setEnabled(false);
            expandMapArea();
            KeyboardUtils.hideSoftInput(mBinding.searchEdit);
        });




        initMapView();
        mBinding.poiList.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL,false));
        mPoiAdapter = new PoiAdapter();
        mBinding.poiList.setAdapter(mPoiAdapter);

        Glide.with(mBinding.poiList).asGif().load(R.drawable.location).into(mBinding.centerMarker);
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

        mBinding.mapview.addJavascriptInterface(mJsBridge,"bridge");

        enableLoactionAssist();

        mBinding.mapview.loadUrl("file:///android_asset/map.html");
        //BaiduLocation.Instance(getApplication()).client().start(
        // );
    }


    private void enableLoactionAssist()
    {
        mBinding.mapview.setWebChromeClient(new WebChromeClient() {
            // 处理javascript中的alert
            public boolean onJsAlert(WebView view, String url, String message,
                                     final JsResult result) {
                return false;
            }
            // 处理javascript中的confirm
            public boolean onJsConfirm(WebView view, String url,
                                       String message, final JsResult result) {
                return false;
            }
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


    }


    private void callJs(String script) {

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


    //JS调用Java的接口
    private class JsBridgeInterface {
        // js代码通过stone_bridge.searchNearby()调用到这里
        @JavascriptInterface
        public void searchNearby(String message){
            mBinding.poiList.post(()->{
                try {
                    mBinding.progress.setVisibility(View.GONE);
                    mPoiAdapter.addDatas(new JSONArray(message));
                }catch (JSONException e){e.printStackTrace();}
            });

        }

        @JavascriptInterface
        public void beginSearch()
        {
            mBinding.poiList.post(()->{
                mPoiAdapter.getData().clear();
                mPoiAdapter.notifyDataSetChanged();
                mBinding.progress.setVisibility(View.VISIBLE);
            });
        }

        @JavascriptInterface
        public void log(String message){
            Log.d("test",message);
        }
    }



    private GestureDetector.OnGestureListener mGestureListener = new GestureDetector.OnGestureListener() {
        @Override
        public boolean onDown(MotionEvent e) {return false;}
        @Override
        public void onShowPress(MotionEvent e) {}
        @Override
        public boolean onSingleTapUp(MotionEvent e) {return false;}
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {return false; }
        @Override
        public void onLongPress(MotionEvent e) {}

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            //从下往上
            if(e1.getY()-e2.getY()>50)
            {
                if(shrinkMapArea()) return true;
            }

            //从上往下
            if(e1.getY()-e2.getY()<-50)
            {
                RecyclerView.LayoutManager layoutManager = mBinding.poiList.getLayoutManager();
                if (layoutManager instanceof LinearLayoutManager) {
                    LinearLayoutManager linearManager = (LinearLayoutManager) layoutManager;
                    int firstItemPosition = linearManager.findFirstVisibleItemPosition();
                    if(firstItemPosition==0)
                    {
                        if(expandMapArea()) return true;
                    }
                }


            }

            return false;
        }
    };


    //缩小地图高度
    private boolean shrinkMapArea()
    {
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams)mBinding.mapview.getLayoutParams();
        if(params.matchConstraintPercentHeight>0.3f)
        {
            if(!bScrolling)
            {
                animationScrollMapView(0.3f);
            }
            return true;
        }
        else return false;
    }

    //扩大地图高度
    private boolean expandMapArea()
    {
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams)mBinding.mapview.getLayoutParams();
        if(params.matchConstraintPercentHeight<=0.3f)
        {

            if(!bScrolling)
            {
                if(KeyboardUtils.isSoftInputVisible(this))
                {
                    KeyboardUtils.hideSoftInput(mBinding.searchEdit);
                }
                animationScrollMapView(0.65f);
            }
            return true;
        }
        else return false;
    }


    //改变地图高度的时候 使用Animator  结束的时候根据是缩小还是扩大 来隐藏和显示DownLabel
    private void animationScrollMapView(float height)
    {
        bScrolling = true;
        ValueAnimator animator = ValueAnimator.ofFloat(((ConstraintLayout.LayoutParams)mBinding.mapview.getLayoutParams()).matchConstraintPercentHeight,height);
        animator.addUpdateListener(animation -> {
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams)mBinding.mapview.getLayoutParams();
            params.matchConstraintPercentHeight = (float)animation.getAnimatedValue();
            mBinding.mapview.setLayoutParams(params);
            if((float)animation.getAnimatedValue()==height)
            {
                bScrolling = false;
                if(height==0.65f)
                {
                    mBinding.downLabel.setVisibility(View.GONE);
                }
                else {
                    mBinding.downLabel.setVisibility(View.VISIBLE);
                }
            }
        });
        animator.setDuration(250);
        animator.start();
    }
}