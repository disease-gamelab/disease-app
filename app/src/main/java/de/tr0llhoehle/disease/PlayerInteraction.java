package de.tr0llhoehle.disease;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Implements the records that are send to the server.
 * Created by patrick on 11/29/16.
 */

class PlayerInteraction implements Parcelable {
    PlayerInteraction(String otherplayer, long state) {
        this.otherplayer = otherplayer;
        this.state = state;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(otherplayer);
        dest.writeLong(state);
    }

    private PlayerInteraction(Parcel in) {
        otherplayer = in.readString();
        state = in.readLong();
    }

        public static final Creator<PlayerInteraction> CREATOR = new Creator<PlayerInteraction>() {
        public PlayerInteraction createFromParcel(Parcel in) {
            return new PlayerInteraction(in);
        }

        public PlayerInteraction[] newArray(int size) {
            return new PlayerInteraction[size];
        }
    };

    public String otherplayer;
    public long state;
};