package com.newasia.locationlib;


import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.Log;

import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.databinding.DataBindingUtil;

import com.newasia.locationlib.databinding.ActivityLocationLimitLayoutBinding;
import com.xuexiang.xutil.common.StringUtils;
import com.xuexiang.xutil.tip.ToastUtils;

import org.json.JSONArray;
import org.json.JSONException;


public class ActivityLocationLimit extends BaseLocationActivity
{
    public static Bitmap s_ResultBitmap;

    public static final int REQUEST_LOCATION_OPEN = 1078;
    public static final int REQUEST_LOCATION_RESULT = 1089;
    public static final String RETURN_RESULT_PARAM = "PoiItem";



    private ActivityLocationLimitLayoutBinding mBinding;
    private JsBridgeInterface mJsBridge = new JsBridgeInterface();
    private PoiAdapter mAdapter;
    private int mSearchRadius = 100;

    @Override
    protected int getLayouResID() {
        return R.layout.activity_location_limit_layout;
    }

    @Override
    protected WebView getmMapView() {
        return mBinding.mapview;
    }

    @Override
    protected void initDataBingding() {
        mBinding = DataBindingUtil.bind(mRootView);
    }



    private Bitmap createViewSnapshot(View view) {
        if (view == null) {
            return null;
        }
        Bitmap screenshot;
        int width = view.getWidth();
        int height = view.getHeight();

        screenshot = Bitmap.createBitmap(width, height/2, Bitmap.Config.ARGB_4444);
        Canvas c = new Canvas(screenshot);
        c.translate(-view.getScrollX(), -(view.getScrollY()+height/4));

        view.draw(c);
        return screenshot;
    }


    public void returnResult(PoiItem item)
    {
        s_ResultBitmap = createViewSnapshot(mMapView);
        Intent retIntent = new Intent();
        retIntent.putExtra(RETURN_RESULT_PARAM,item);
        setResult(REQUEST_LOCATION_RESULT,retIntent);
        finish();
    }



    @Override
    protected void initViews() {
        mAdapter = new PoiAdapter();
        mBinding.poiList.setAdapter(mAdapter);

        mBinding.cancelLabel.setOnClickListener(v -> {finish();});

        mBinding.btnOk.setOnClickListener(v -> {
            PoiItem item = mAdapter.getSelectItem();
            if(item!=null)   returnResult(item);
        });

        mAdapter.setOnItemSelectListener(index -> {
            mBinding.btnOk.setEnabled(true);
            PoiItem item = mAdapter.getItem(index);
            callJs(String.format("javascript:moveTo(%s,%s)",item.lat,item.lng));
        });

        initMapView();
        mMapView.addJavascriptInterface(mJsBridge,"bridge");
        mBinding.mapview.loadUrl("file:///android_asset/map_limit.html");
    }


    //JS调用Java的接口
    private class JsBridgeInterface {
        // js代码通过stone_bridge.searchNearby()调用到这里
        @JavascriptInterface
        public void searchNearbyResult(String message){
            mBinding.mapview.post(()->{
                try {
                    if(StringUtils.isEmpty(message)) {
                        mBinding.btnOk.setEnabled(false);
                        mAdapter.clearAllData();
                        mSearchRadius +=100;
                        if(mSearchRadius<1000)
                        {
                            callJs(String.format("javascript:searchLocal(%s)",mSearchRadius));
                        }
                        else ToastUtils.toast("附近没有查询到任何定位参照物");

                    }
                    else {
                        mAdapter.addDatas(new JSONArray(message));
                        mAdapter.selectItem(0);
                        mBinding.btnOk.setEnabled(true);
                    }
                }catch (JSONException e){e.printStackTrace();}
            });
        }

        @JavascriptInterface
        public void locationSucceeded()
        {
            mBinding.mapview.post(()->{
                callJs(String.format("javascript:searchLocal(%s)",mSearchRadius));
            });
        }

        @JavascriptInterface
        public void log(String message){
            Log.d("test",message);
        }

        @JavascriptInterface
        public void toast(String content)
        {
            mBinding.mapview.post(()->{
                ToastUtils.toast(content);
            });
        }
    }

}
