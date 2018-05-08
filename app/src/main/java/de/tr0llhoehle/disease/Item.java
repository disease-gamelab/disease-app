package de.tr0llhoehle.disease;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

/**
 * Created by kai on 02/05/18.
 */

public class Item implements Parcelable {

    public long itemuid;
    public long owneruid;
    public int itemtype;
    public long timestamp;
    public double lon;
    public double lat;

    public Item(long itemuid, long owneruid, int itemtype, long timestamp, double lon, double lat) {
        this.itemuid = itemuid;
        this.owneruid = owneruid;
        this.itemtype = itemtype;
        this.timestamp = timestamp;
        this.lon = lon;
        this.lat = lat;
    }

    public int describeContents() {
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Item item = (Item) o;
        return itemuid == item.itemuid &&
                owneruid == item.owneruid &&
                itemtype == item.itemtype &&
                timestamp == item.timestamp;
    }

    @Override
    public int hashCode() {

        return Objects.hash(itemuid, owneruid, itemtype, timestamp);
    }

    @Override
    public String toString() {
        return "Item{" +
                "itemuid=" + itemuid +
                ", owneruid=" + owneruid +
                ", itemtype=" + itemtype +
                ", timestamp=" + timestamp +
                ", lon=" + lon +
                ", lat=" + lat +
                '}';
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(itemuid);
        dest.writeLong(owneruid);
        dest.writeInt(itemtype);
        dest.writeLong(timestamp);
        dest.writeDouble(lon);
        dest.writeDouble(lat);
    }

    private Item(Parcel in) {
        this.itemuid = in.readLong();
        this.owneruid = in.readLong();
        this.itemtype = in.readInt();
        this.timestamp = in.readLong();
        this.lon = in.readDouble();
        this.lat = in.readDouble();
    }

    public static final Creator<Item> CREATOR = new Creator<Item>() {
        public Item createFromParcel(Parcel in) {
            return new Item(in);
        }

        public Item[] newArray(int size) {
            return new Item[size];
        }
    };
}
