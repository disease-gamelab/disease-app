package de.tr0llhoehle.disease;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

/**
 * Created by kai on 26.03.18.
 */

public class ActionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        String action=intent.getStringExtra("action");

        //Toast.makeText(context,"received: "+action,Toast.LENGTH_SHORT).show();

        Intent it = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        context.sendBroadcast(it);

        if (action.equals("attack")) {
            Intent intentSend = new Intent(NotificationBuilder.NOTIFICATION_ATTACK);

            LocalBroadcastManager.getInstance(context).sendBroadcast(intentSend);
        } else if (action.equals("befriend")) {
            Intent intentSend = new Intent(NotificationBuilder.NOTIFICATION_BEFRIEND);

            LocalBroadcastManager.getInstance(context).sendBroadcast(intentSend);
        } else if (action.equals("openApp")) {
            Intent intentSend = new Intent(NotificationBuilder.NOTIFICATION_OPEN_APP);

            LocalBroadcastManager.getInstance(context).sendBroadcast(intentSend);
        } else if (action.equals("run")) {
            Intent intentSend = new Intent(NotificationBuilder.NOTIFICATION_RUN);

            LocalBroadcastManager.getInstance(context).sendBroadcast(intentSend);
        } else if (action.equals("close")) {
            context.stopService(new Intent(context, LocationTracker.class));
            context.stopService(new Intent(context, SyncService.class));
            context.stopService(new Intent(context, NotificationBuilder.class));

            System.exit(0);
        }

/*
        String action=intent.getStringExtra("action");
        if(action.equals("action1")){
            performAction1();
        }
        else if(action.equals("action2")){
            performAction2();

        }
        //This is used to close the notification tray
        Intent it = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        context.sendBroadcast(it);*/
    }

    public void performAction1(){

    }

    public void performAction2(){

    }

}
