package com.newasia.locationlib;


import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import android.animation.ValueAnimator;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.LinearLayout;


import com.bumptech.glide.Glide;
import com.newasia.locationlib.databinding.ActivityLocationGrabberBinding;

import com.xuexiang.xutil.common.StringUtils;
import com.xuexiang.xutil.display.DensityUtils;
import com.xuexiang.xutil.system.KeyboardUtils;
import com.xuexiang.xutil.tip.ToastUtils;

import org.json.JSONArray;
import org.json.JSONException;

public class ActivityLocationGrabber extends BaseLocationActivity {

    private ActivityLocationGrabberBinding mBinding;
    private JsBridgeInterface mJsBridge = new JsBridgeInterface();
    private PoiAdapter mPoiAdapter;
    private PoiAdapter mNamePoiAdapter;
    private GestureDetector mGesture;

    private Boolean bScrolling = false;

    private Handler mHandler = new Handler(Looper.getMainLooper());
    private Runnable mEditRun;

    private static final int REQUEST_PERMISSION_CODE = 1025;
    private static final int SEARCH_MODE_NEARBY = 1021;
    private static final int SEARCH_MODE_NAME = 1022;


    public static final int REQUEST_LOCATION_OPEN = 1077;
    public static final int REQUEST_LOCATION_RESULT = 1066;
    public static final String RETURN_RESULT_PARAM = "PoiItem";



    private int mSearchMode = SEARCH_MODE_NEARBY;

    public static Bitmap s_mapBitmap;


    @Override
    protected int getLayouResID() {
        return R.layout.activity_location_grabber;
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
        if(((ConstraintLayout.LayoutParams)mBinding.viewLayout.getLayoutParams()).matchConstraintPercentHeight>0.35)
        {
            c.translate(-view.getScrollX(), -view.getScrollY());
        }else {
            c.translate(-view.getScrollX(), -(view.getScrollY()+height/4));
        }

        view.draw(c);
        return screenshot;
    }

    public void returnResult(PoiItem item)
    {
        s_mapBitmap = createViewSnapshot(mBinding.mapArea);
        Intent retIntent = new Intent();
        retIntent.putExtra(RETURN_RESULT_PARAM,item);
        setResult(REQUEST_LOCATION_RESULT,retIntent);
        finish();
    }


    @Override
    protected void initDataBingding() {
        mBinding  = DataBindingUtil.bind(mRootView);
    }

    @Override
    protected WebView getmMapView() {
        return mBinding.mapview;
    }

    @Override
    protected void initViews()
    {
        //点击关闭该页面
        mBinding.cancelLabel.setOnClickListener(v -> {
            finish();
        });

        //发送按钮监听
        mBinding.btnSend.setOnClickListener(v -> {
            if(mSearchMode==SEARCH_MODE_NEARBY)
            {
                PoiItem item = mPoiAdapter.getSelectItem();
                if(item!=null)   returnResult(item);
            }else {
                PoiItem item = mNamePoiAdapter.getSelectItem();
                if(item!=null)   returnResult(item);
            }
        });

        //给RecyclerView添加滑动监听 来改变它的高度
        mGesture = new GestureDetector(this,mGestureListener);
        mBinding.poiList.setOnTouchListener((v, event) -> {
            return mGesture.onTouchEvent(event);
        });


        //如果弹出软键盘拉伸下方View区域
        KeyboardUtils.registerSoftInputChangedListener(this,height -> {
            if(height>10)
            {
                expandViewArea();
            }
        });


        //搜索框的监听
        mBinding.searchEdit.addTextChangedListener(mEditListener);



        //如果当前不是搜索地点模式，则跳转到搜索地点模式
        mBinding.searchLayout.setOnClickListener(v -> {
            if(!mBinding.searchEdit.isEnabled())
            {
                mNamePoiAdapter.clearAllData();
                mBinding.searchLayout.setGravity(Gravity.LEFT);
                mBinding.searchEdit.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                mBinding.searchEdit.setEnabled(true);
                expandViewArea();
                KeyboardUtils.showSoftInput(mBinding.searchEdit);
                mSearchMode = SEARCH_MODE_NAME;
                mBinding.poiList.setAdapter(mNamePoiAdapter);
                mBinding.btnSend.setEnabled(false);
            }
        });


        //关闭搜索地点模式
        mBinding.downLabel.setOnClickListener(v -> {
            mBinding.searchEdit.setText("");
            mBinding.searchLayout.setGravity(Gravity.CENTER);
            mBinding.searchEdit.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
            mBinding.searchEdit.setEnabled(false);
            shrinkViewArea();
            KeyboardUtils.hideSoftInput(mBinding.searchEdit);
            mSearchMode = SEARCH_MODE_NEARBY;
            mBinding.poiList.setAdapter(mPoiAdapter);
            if(mPoiAdapter.getItemCount()!=0)
                mBinding.btnSend.setEnabled(true);
            else mBinding.btnSend.setEnabled(false);
        });




        initMapView();
        mBinding.poiList.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL,false));
        mPoiAdapter = new PoiAdapter();
        mNamePoiAdapter = new PoiAdapter();
        mBinding.poiList.setAdapter(mPoiAdapter);

        Glide.with(mBinding.poiList).asGif().load(R.drawable.location).into(mBinding.centerMarker);


        mNamePoiAdapter.setOnItemSelectListener(index -> {
            mBinding.btnSend.setEnabled(true);
            PoiItem item = mNamePoiAdapter.getItem(index);
            callJs(String.format("javascript:changeCenter(%s,%s)",item.lng,item.lat));
        });

        mPoiAdapter.setOnItemSelectListener(index -> {
            mBinding.btnSend.setEnabled(true);
            PoiItem item = mPoiAdapter.getItem(index);
            callJs(String.format("javascript:changeCenter(%s,%s)",item.lng,item.lat));
        });


        mMapView.addJavascriptInterface(mJsBridge,"bridge");

        mMapView.loadUrl("file:///android_asset/map.html");

    }





    //JS调用Java的接口
    private class JsBridgeInterface {
        // js代码通过stone_bridge.searchNearby()调用到这里
        @JavascriptInterface
        public void searchNearbyResult(String message){
            mBinding.poiList.post(()->{
                try {
                    mBinding.progress.setVisibility(View.GONE);
                    if(StringUtils.isEmpty(message)) mPoiAdapter.clearAllData();
                    else {
                        mPoiAdapter.addDatas(new JSONArray(message));
                        mPoiAdapter.selectItem(0);
                        mBinding.btnSend.setEnabled(true);
                    }
                }catch (JSONException e){e.printStackTrace();}
            });
        }


        @JavascriptInterface
        public void searchNameResult(String message){
            mBinding.poiList.post(()->{
                try {
                    mBinding.progress.setVisibility(View.GONE);
                    if(StringUtils.isEmpty(message)) mNamePoiAdapter.clearAllData();
                    else {
                        mNamePoiAdapter.addDatas(new JSONArray(message));
                    }
                }catch (JSONException e){e.printStackTrace();}
            });
        }


        @JavascriptInterface
        public void beginSearch()
        {
            mBinding.btnSend.setEnabled(false);
            mBinding.poiList.post(()->{
                if(mSearchMode==SEARCH_MODE_NAME) mNamePoiAdapter.clearAllData();
                else mPoiAdapter.clearAllData();
                mBinding.progress.setVisibility(View.VISIBLE);
            });
        }

        @JavascriptInterface
        public void log(String message){
            Log.d("test",message);
        }

        @JavascriptInterface
        public void toast(String content)
        {
            mBinding.poiList.post(()->{
                ToastUtils.toast(content);
            });
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
                if(expandViewArea()) return true;
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
                        if(shrinkViewArea()) return true;
                    }
                }


            }

            return false;
        }
    };



    private TextWatcher mEditListener = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable s) {
            if(mEditRun!=null) mHandler.removeCallbacks(mEditRun);
            mEditRun = new Runnable() {
                @Override
                public void run() {
                    if(!StringUtils.isEmpty(s.toString()))
                    callJs(String.format("javascript:searchByName('%s')",s.toString()));
                }
            };
            mHandler.postDelayed(mEditRun,500);
        }
    };


    //缩小View区域高度
    private boolean shrinkViewArea()
    {
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams)mBinding.viewLayout.getLayoutParams();
        if(params.matchConstraintPercentHeight>0.35f)
        {
            if(!bScrolling)
            {
                if(KeyboardUtils.isSoftInputVisible(this))
                {
                    KeyboardUtils.hideSoftInput(mBinding.searchEdit);
                }
                animationScrollViewLayout(0.35f);
            }
            return true;
        }
        else return false;
    }

    //扩大View区域高度
    private boolean expandViewArea()
    {
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams)mBinding.viewLayout.getLayoutParams();
        if(params.matchConstraintPercentHeight<=0.35f)
        {
            if(!bScrolling)
            {
                animationScrollViewLayout(0.65f);
            }
            return true;
        }
        else return false;
    }


    //改变地图高度的时候 使用Animator  结束的时候根据是缩小还是扩大 来隐藏和显示DownLabel
    private void animationScrollViewLayout(float height)
    {
        bScrolling = true;
        ValueAnimator animator = ValueAnimator.ofFloat(((ConstraintLayout.LayoutParams)mBinding.viewLayout.getLayoutParams()).matchConstraintPercentHeight,height);
        animator.addUpdateListener(animation -> {
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams)mBinding.viewLayout.getLayoutParams();
            params.matchConstraintPercentHeight = (float)animation.getAnimatedValue();
            mBinding.viewLayout.setLayoutParams(params);
            if((float)animation.getAnimatedValue()==height)
            {
                bScrolling = false;
                if(height==0.35f)
                {
                    mBinding.downLabel.setVisibility(View.GONE);
                    mBinding.mapview.setTranslationY(0);
                    mBinding.centerMarker.setTranslationY(0);
                }
                else {
                    mBinding.downLabel.setVisibility(View.VISIBLE);
                    mBinding.mapview.setTranslationY(-(DensityUtils.getScreenHeight()*0.2f));
                    mBinding.centerMarker.setTranslationY(-(DensityUtils.getScreenHeight()*0.2f));
                }
            }
        });
        animator.setDuration(250);
        animator.start();
    }
}