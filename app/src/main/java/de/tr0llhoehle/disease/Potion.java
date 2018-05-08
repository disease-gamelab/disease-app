package de.tr0llhoehle.disease;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by kai on 02/05/18.
 */

public class Potion implements Parcelable {

    @Override
    public String toString() {
        return "Potion{" +
                "healing='" + healing + '\'' +
                '}';
    }

    public long healing;

    Potion(long healing) {
        this.healing = healing;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(healing);
    }

    private Potion(Parcel in) {
        this.healing = in.readLong();
    }

    public static final Creator<Potion> CREATOR = new Creator<Potion>() {
        public Potion createFromParcel(Parcel in) {
            return new Potion(in);
        }

        public Potion[] newArray(int size) {
            return new Potion[size];
        }
    };
}
