package de.tr0llhoehle.disease;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by kai on 02/28/18.
 */

public class PlayerEngagement implements Parcelable {
    public String player1uid;
    public String player2uid;
    public long timestamp;
    public int active;
    public int state;

    public PlayerEngagement(String player1uid, String player2uid, long timestamp, int active, int state) {
        this.player1uid = player1uid;
        this.player2uid = player2uid;
        this.timestamp = timestamp;
        this.active = active;
        this.state = state;
    }

    @Override
    public String toString() {
        return "PlayerEngagement{" +
                "player1uid='" + player1uid + '\'' +
                ", player2uid='" + player2uid + '\'' +
                ", timestamp=" + timestamp +
                ", active=" + active +
                ", state=" + state +
                '}';
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(player1uid);
        dest.writeString(player2uid);
        dest.writeLong(timestamp);
        dest.writeInt(active);
        dest.writeInt(state);
    }

    private PlayerEngagement(Parcel in) {
        this.player1uid = in.readString();
        this.player2uid = in.readString();
        this.timestamp = in.readLong();
        this.active = in.readInt();
        this.state = in.readInt();
    }

    public static final Creator<PlayerEngagement> CREATOR = new Creator<PlayerEngagement>() {
        public PlayerEngagement createFromParcel(Parcel in) {
            return new PlayerEngagement(in);
        }

        public PlayerEngagement[] newArray(int size) {
            return new PlayerEngagement[size];
        }
    };
}
