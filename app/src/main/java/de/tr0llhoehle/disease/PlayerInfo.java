package de.tr0llhoehle.disease;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Implements the records that are send to the server.
 * Created by patrick on 11/29/16.
 */

class PlayerInfo implements Parcelable {
    PlayerInfo(String playeruid, int xp, int health) {
        this.playeruid = playeruid;
        this.xp = xp;
        this.health = health;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(playeruid);
        dest.writeInt(xp);
        dest.writeInt(health);
    }

    private PlayerInfo(Parcel in) {
        playeruid = in.readString();
        xp = in.readInt();
        health = in.readInt();
    }

        public static final Creator<PlayerInfo> CREATOR = new Creator<PlayerInfo>() {
        public PlayerInfo createFromParcel(Parcel in) {
            return new PlayerInfo(in);
        }

        public PlayerInfo[] newArray(int size) {
            return new PlayerInfo[size];
        }
    };

    public String playeruid;
    public int xp;
    public int health;
};