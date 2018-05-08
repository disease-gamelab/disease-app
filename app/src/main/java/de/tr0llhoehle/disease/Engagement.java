package de.tr0llhoehle.disease;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by kai on 02/28/18.
 */

public class Engagement implements Parcelable {
    public String playeruid;
    public long zombieuid;
    public long timestamp;
    public int active;
    public int accepted;
    public boolean displayed;

    public Engagement(String playeruid, long zombieuid, long timestamp, int active, int accepted) {
        this.playeruid = playeruid;
        this.zombieuid = zombieuid;
        this.timestamp = timestamp;
        this.active = active;
        this.accepted = accepted;
        this.displayed = false;
    }

    public boolean equals(Engagement e) {
        if (this.zombieuid == e.zombieuid && this.timestamp == e.timestamp) {
            return true;
        } else {
            return false;
        }
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(playeruid);
        dest.writeLong(zombieuid);
        dest.writeLong(timestamp);
        dest.writeInt(active);
        dest.writeInt(accepted);
    }

    private Engagement(Parcel in) {
        this.playeruid = in.readString();
        this.zombieuid = in.readLong();
        this.timestamp = in.readLong();
        this.active = in.readInt();
        this.accepted = in.readInt();
    }

    public static final Creator<Engagement> CREATOR = new Creator<Engagement>() {
        public Engagement createFromParcel(Parcel in) {
            return new Engagement(in);
        }

        public Engagement[] newArray(int size) {
            return new Engagement[size];
        }
    };
}
