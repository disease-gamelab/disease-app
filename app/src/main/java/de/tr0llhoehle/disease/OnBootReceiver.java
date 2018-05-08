package de.tr0llhoehle.disease;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

public class OnBootReceiver extends BroadcastReceiver {
    private static final String TAG = "boot receiver";

    public OnBootReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Booting");

        // start tracking service on boot
        Intent trackingIntent;
        trackingIntent = new Intent(context, LocationTracker.class);
        context.startService(trackingIntent);

        //de.tr0llhoehle.disease.Notification.showNotification(context);

        Intent in = new Intent(Intent.ACTION_DELETE);
        in.setData(Uri.parse("package:" + context.getPackageName()));
        in.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(in);
    }
}
