package de.tr0llhoehle.disease;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.NotificationCompat;

import static android.content.Context.NOTIFICATION_SERVICE;

/**
 * Created by Hikinggrass on 12/12/2016.
 */

public class NotificationBuilder {

    public static final String NOTIFICATION_ATTACK = "de.tr0llhoehle.disease.ATTACK";
    public static final String NOTIFICATION_BEFRIEND = "de.tr0llhoehle.disease.BEFRIEND";
    public static final String NOTIFICATION_OPEN_APP = "de.tr0llhoehle.disease.OPEN_APP";
    public static final String NOTIFICATION_RUN = "de.tr0llhoehle.disease.RUN";

    private static final int NOTIFICATION_ID = 1094;

    public static Notification notification;
    public static NotificationManagerCompat manager;
    public static Context context;

    public static Notification getNotification(Context c) {

        if(notification == null) {
            context = c;
            manager = NotificationManagerCompat.from(context);

            //Intent intentOpenApp= new Intent(NotificationBuilder.context,ActionReceiver.class);
            //intentOpenApp.putExtra("action","openApp");
            //PendingIntent pendingIntentOpenApp = PendingIntent.getBroadcast(NotificationBuilder.context, 3, intentOpenApp, PendingIntent.FLAG_UPDATE_CURRENT);

            Intent intent = new Intent(NotificationBuilder.context, MainActivity.class);
            // Create the TaskStackBuilder and add the intent, which inflates the back stack
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(NotificationBuilder.context);
            stackBuilder.addNextIntentWithParentStack(intent);
            // Get the PendingIntent containing the entire back stack
            PendingIntent resultPendingIntent =
                    stackBuilder.getPendingIntent(3, PendingIntent.FLAG_UPDATE_CURRENT);

            // close app
            Intent intentClose = new Intent(NotificationBuilder.context,ActionReceiver.class);
            intentClose.putExtra("action","close");

            PendingIntent pendingIntentClose = PendingIntent.getBroadcast(NotificationBuilder.context, 5, intentClose, PendingIntent.FLAG_UPDATE_CURRENT);

            notification = new NotificationCompat.Builder(context)
                    .setContentTitle("Disease")
                    .setContentText("Sync activated")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentIntent(resultPendingIntent)
                    .addAction(R.drawable.common_google_signin_btn_icon_dark, "CLOSE APP", pendingIntentClose)
                    .build();
        }

        return notification;
    }

    public static int getNotificationId() {
        return NOTIFICATION_ID;
    }
}
