package de.tr0llhoehle.disease;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Implements the records that are send to the server.
 * Created by patrick on 11/29/16.
 */

class Interaction implements Parcelable {
    Interaction(long engagement, long zombiehealth, long playerhealth) {
        this.engagement = engagement;
        this.zombiehealth = zombiehealth;
        this.playerhealth = playerhealth;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(engagement);
        dest.writeLong(zombiehealth);
        dest.writeLong(playerhealth);
    }

    private Interaction(Parcel in) {
        engagement = in.readLong();
        zombiehealth = in.readLong();
        playerhealth = in.readLong();
    }

    public boolean equals(Interaction i) {
        if (this.engagement == i.engagement) {
            return true;
        } else {
            return false;
        }
    }

        public static final Creator<Interaction> CREATOR = new Creator<Interaction>() {
        public Interaction createFromParcel(Parcel in) {
            return new Interaction(in);
        }

        public Interaction[] newArray(int size) {
            return new Interaction[size];
        }
    };

    public long engagement;
    public long zombiehealth;
    public long playerhealth;
};