package com.newasia.locationlib;


import android.os.Parcel;
import android.os.Parcelable;


public class PoiItem  implements Parcelable
{
    public String title = "";
    public String addr = "";
    public int distance = 0;
    public double lng;
    public double lat;
    public PoiItem(){}
    public PoiItem(Parcel in) {
        this.title = in.readString();
        this.addr = in.readString();
        this.distance = in.readInt();
        this.lng = in.readDouble();
        this.lat = in.readDouble();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(title);
        dest.writeString(addr);
        dest.writeInt(distance);
        dest.writeDouble(lng);
        dest.writeDouble(lat);
    }

    public static final Creator<PoiItem> CREATOR = new Creator<PoiItem>() {
        @Override
        public PoiItem createFromParcel(Parcel in) {
            return new PoiItem(in);
        }

        @Override
        public PoiItem[] newArray(int size) {
            return new PoiItem[size];
        }
    };
}
