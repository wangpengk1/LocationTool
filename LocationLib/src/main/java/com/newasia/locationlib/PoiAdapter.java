package com.newasia.locationlib;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class PoiAdapter extends BaseQuickAdapter<PoiAdapter.PoiItem, BaseViewHolder>
{
    public PoiAdapter() {
        super(R.layout.poi_item_layout);
    }

    @Override
    protected void convert(@NotNull BaseViewHolder holder, PoiItem item)
    {
        holder.setText(R.id.poi_title,item.title);
        holder.setText(R.id.poi_addr,item.distance+"m|"+item.addr);
    }

    public void addDatas(JSONArray array)
    {
        getData().clear();
        ArrayList<PoiItem> dataList = new ArrayList<>();
        try
        {
            for(int i=0;i<array.length();++i)
            {
                JSONObject obj = array.getJSONObject(i);
                PoiItem item = new PoiItem();
                item.title = obj.getString("title");
                item.addr = obj.getString("addr");
                item.distance = (int)obj.getDouble("distance");
                dataList.add(item);
            }
        }catch (JSONException e){e.printStackTrace();}

        if(dataList.size()>0)
        {
            Collections.sort(dataList, new Comparator<PoiItem>() {
                @Override
                public int compare(PoiItem o1, PoiItem o2) {
                    return o1.distance-o2.distance;
                }
            });
        }
        addData(dataList);
        notifyDataSetChanged();
    }

    public static class PoiItem
    {
        public String title = "";
        public String addr = "";
        public int distance = 0;
    }
}
