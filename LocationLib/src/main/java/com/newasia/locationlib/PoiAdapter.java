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

public class PoiAdapter extends BaseQuickAdapter<PoiItem, BaseViewHolder>
{
    private int mSelectedIndex = -1;
    private OnItemSelectListener mSelectListener;

    public interface OnItemSelectListener
    {
        void onSelect(int index);
    }


    public PoiAdapter() {
        super(R.layout.poi_item_layout);

        setOnItemClickListener((adapter, view, position) -> {
            if(position!=mSelectedIndex)
            {
                int last = mSelectedIndex;
                mSelectedIndex = position;
                notifyItemChanged(last);
                notifyItemChanged(position);
                if(mSelectListener!=null)
                {
                    mSelectListener.onSelect(position);
                }
            }
        });
    }



    public void setOnItemSelectListener(OnItemSelectListener listener)
    {
        mSelectListener = listener;
    }


    public void selectItem(int pos)
    {
        if(pos>=0 && pos<getItemCount())
        {
            int last = mSelectedIndex;
            mSelectedIndex = pos;
            notifyItemChanged(pos);
            if(last!=-1) notifyItemChanged(last);
        }
    }




    public PoiItem getSelectItem()
    {
        PoiItem retItem =null;
        if(mSelectedIndex>=0 && mSelectedIndex<getItemCount())
        {
            retItem = getItem(mSelectedIndex);
        }

        return retItem;
    }


    @Override
    protected void convert(@NotNull BaseViewHolder holder, PoiItem item)
    {
        holder.setText(R.id.poi_title,item.title);
        holder.setText(R.id.poi_addr,item.distance+"m|"+item.addr);

        //根据选择 是否显示图标
        if(holder.getAdapterPosition()==mSelectedIndex) holder.setVisible(R.id.img_selected,true);
        else holder.setVisible(R.id.img_selected,false);
    }

    public void clearAllData()
    {
        mSelectedIndex = -1;
        getData().clear();
        notifyDataSetChanged();
    }

    public void addDatas(JSONArray array)
    {
        clearAllData();
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
                item.lng = obj.getDouble("lng");
                item.lat = obj.getDouble("lat");
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

}
