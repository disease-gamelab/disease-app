package de.tr0llhoehle.disease;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by kai on 02/05/18.
 */

public class Zombie implements Parcelable {

    @Override
    public String toString() {
        return "Zombie{" +
                "uid='" + uid + '\'' +
                ", lon=" + lon +
                ", lat=" + lat +
                ", last_timestamp=" + last_timestamp +
                ", health=" + health +
                ", bearing=" + bearing +
                '}';
    }

    public long uid;
    public double lon;
    public double lat;
    public long last_timestamp;
    public int health;
    public double bearing;

    Zombie(long uid, double lon, double lat, long last_timestamp, int health, double bearing) {
        this.uid = uid;
        this.lon = lon;
        this.lat = lat;
        this.last_timestamp = last_timestamp;
        this.health = health;
        this.bearing = bearing;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(uid);
        dest.writeDouble(lon);
        dest.writeDouble(lat);
        dest.writeLong(last_timestamp);
        dest.writeInt(health);
        dest.writeDouble(bearing);
    }

    private Zombie(Parcel in) {
        this.uid = in.readLong();
        this.lon = in.readDouble();
        this.lat = in.readDouble();
        this.last_timestamp = in.readLong();
        this.health = in.readInt();;
        this.bearing = in.readDouble();;
    }

    public static final Creator<Zombie> CREATOR = new Creator<Zombie>() {
        public Zombie createFromParcel(Parcel in) {
            return new Zombie(in);
        }

        public Zombie[] newArray(int size) {
            return new Zombie[size];
        }
    };
}
