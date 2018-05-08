package de.tr0llhoehle.disease;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.Settings;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.RelativeLayout;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

/**
 * Captures the complete state of the game;
 * Created by patrick on 11/21/16.
 */

class GameModel extends BroadcastReceiver {
    private static final String TAG = "GameModel";

    private Player current_player = new Player("not connected", 0, 0, 0, -1);
    private Player[] other_players;
    private Zombie[] zombies;
    private Item[] items;
    private ArrayList<Item> player_items = new ArrayList<>();
    private LatLng[] boundingbox;
    private LatLng[] player_boundingbox;
    private ArrayList<Engagement> engagements = new ArrayList<>();
    private ArrayList<PlayerEngagement> player_engagements = new ArrayList<>();
    public long last_player_attack = 0;
    public long last_player_defend = 0;
    public long last_player_potion = 0;

    public long last_zombie_attack = 0;
    public long zombie_attack_time = 3000;
    public int zombie_attack_state = 0;

    public boolean engaged = false;
    public boolean player_engaged = false;
    public boolean player_engagement_ended = false;
    public String engaged_with_player = "";

    public boolean foreground = true;

    public boolean interactive_mode = false;

    public boolean portrait = true;

    public Item newest_item = null;

    private boolean first_sync = true;

    public boolean new_data = false;

    public ArrayList<Potion> potions = new ArrayList<>();

    private void updatePlayer(Record record) {
        assert record != null;
        if (record.timestamp > current_player.last_timestamp) {
            current_player.lon = record.lon;
            current_player.lat = record.lat;
            current_player.last_timestamp = record.timestamp;
        }
    }

    private void updateOtherPlayers(Player[] other) {
        Log.d(TAG, "received players: ");

        other_players = other;
    }

    private void updatePlayerItems(Item[] pitems) {
        for (Item item: pitems
             ) {
            if(!player_items.contains(item)) {
                player_items.add(item);
                if (!first_sync) {
                    newest_item = item;
                }
            }
        }
    }

    private void updateZombies(Zombie[] zombies) { this.zombies = zombies; }
    private void updateItems(Item[] items) { this.items = items; }
    private void updateBoundingbox(LatLng[] boundingbox) { this.boundingbox = boundingbox; }
    private void updatePlayerBoundingbox(LatLng[] player_boundingbox) { this.player_boundingbox = player_boundingbox; }

    public ArrayList<PlayerEngagement> getPlayerEngagements() {
        return player_engagements;
    }

    private void updateEngagements(Engagement[] engagements) {
        if (!engaged) {
            this.engagements.clear();
            for (Engagement e : engagements

                    ) {
                if (last_zombie_attack == 0) {
                    last_zombie_attack = System.currentTimeMillis();
                }
                this.engagements.add(e);
            }
        }
    }

    private void updatePlayerInfo(PlayerInfo[] player_info) {
        if(player_info != null && player_info.length == 1) {
            Log.d(TAG, "received player info");
            current_player.health = player_info[0].health;
        }
    }

    private void updatePlayerEngagements(PlayerEngagement[] pe) {
        if(this.player_engagements.size() == 0) {
            this.player_engagements.clear();
            for (PlayerEngagement p : pe) {
                Log.d(TAG, "received player_engagements info: " + p.toString());
                this.player_engagements.add(p);
            }
        } else {
            if (pe.length == 2) {
                if (!pe[0].player2uid.equals(engaged_with_player)) {
                    engaged_with_player = pe[0].player2uid;
                    // post notification
                    NotificationManagerCompat notificationManager = NotificationBuilder.manager;
                    if (notificationManager != null) {
                        Intent intentAttack = new Intent(NotificationBuilder.context,ActionReceiver.class);
                        intentAttack.putExtra("action","attack");

                        Intent intentBefriend = new Intent(NotificationBuilder.context,ActionReceiver.class);
                        intentBefriend.putExtra("action","befriend");

                        Intent intentOpenApp= new Intent(NotificationBuilder.context,ActionReceiver.class);
                        intentOpenApp.putExtra("action","openApp");

                        Intent intentRun = new Intent(NotificationBuilder.context,ActionReceiver.class);
                        intentRun.putExtra("action","run");

                        PendingIntent pendingIntentAttack = PendingIntent.getBroadcast(NotificationBuilder.context, 1, intentAttack, PendingIntent.FLAG_UPDATE_CURRENT);
                        PendingIntent pendingIntentBefriend = PendingIntent.getBroadcast(NotificationBuilder.context, 2, intentBefriend, PendingIntent.FLAG_UPDATE_CURRENT);
                        PendingIntent pendingIntentOpenApp = PendingIntent.getBroadcast(NotificationBuilder.context, 3, intentOpenApp, PendingIntent.FLAG_UPDATE_CURRENT);
                        PendingIntent pendingIntentRun = PendingIntent.getBroadcast(NotificationBuilder.context, 4, intentRun, PendingIntent.FLAG_UPDATE_CURRENT);

                        Notification notification = new NotificationCompat.Builder(NotificationBuilder.context)
                                .setContentTitle("Disease")
                                .setContentText("Player in reach!")
                                .setSmallIcon(R.mipmap.ic_launcher)
                                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                                .setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
                                .setVibrate(new long[] { 100, 50, 100, 20, 50, 10, 30,300, 200 })
                                .setLights(Color.RED, 1000, 1000)
                                .addAction(R.drawable.common_google_signin_btn_icon_dark, "ATTACK", pendingIntentAttack)
                                .addAction(R.drawable.common_google_signin_btn_icon_dark, "BEFRIEND", pendingIntentBefriend)
                                .addAction(R.drawable.common_google_signin_btn_icon_dark, "RUN", pendingIntentRun)
                                .setContentIntent(pendingIntentOpenApp)
                                .build();
                        notificationManager.notify(NotificationBuilder.getNotificationId(), notification);
                    }
                }
                this.player_engaged = true;
                for (int i = 0; i < pe.length; i++) {
                    PlayerEngagement p = this.player_engagements.get(i);
                    p.active = pe[i].active;
                    p.player1uid = pe[i].player1uid; //TODO good idea?
                    p.player2uid = pe[i].player2uid; //TODO good idea?
                    p.state = pe[i].state;
                    p.timestamp = pe[i].timestamp; // TODO / FIXME - maybe use this as a feature to decide if we want to update or replace?
                }
                for(PlayerEngagement p: this.player_engagements) {
                    Log.d(TAG, "$$$new player_engagements info: " + p.toString());
                }
            }
        }

        if( pe.length == 1) {
            this.player_engagements.clear();
            for (PlayerEngagement p : pe) {
                Log.d(TAG, "received single player_engagements info: " + p.toString());
                this.player_engagements.add(p);
            }
        }

        if (pe.length == 0) {
            Log.d(TAG, "resetting player_engagements");
            this.player_engagements.clear();
            this.player_engaged = false;
            NotificationManagerCompat notificationManager = NotificationBuilder.manager;
            if (notificationManager != null) {
                notificationManager.notify(NotificationBuilder.getNotificationId(), NotificationBuilder.notification);
            }
        }
    }


    public ArrayList<Item> getPlayerItems() {
        return player_items;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        switch (intent.getAction()) {
            case LocationTracker.UPDATE_RECORD:
                updatePlayer((Record) intent.getParcelableExtra("record"));
                break;
            case SyncService.NEW_REMOTE_STATE:
                this.new_data = true;

                Parcelable[] parcelables = intent.getParcelableArrayExtra("players");
                Player[] players = new Player[parcelables.length];
                for (int i = 0; i < parcelables.length; ++i)
                    players[i] = (Player) parcelables[i];
                updateOtherPlayers(players);

                Parcelable[] parcelables_z = intent.getParcelableArrayExtra("zombies");
                Zombie[] zs = new Zombie[parcelables_z.length];
                for (int i = 0; i < parcelables_z.length; ++i)
                    zs[i] = (Zombie) parcelables_z[i];
                updateZombies(zs);

                Parcelable[] parcelables_bb = intent.getParcelableArrayExtra("boundingbox");
                LatLng[] bb = new LatLng[parcelables_bb.length];
                for (int i = 0; i < parcelables_bb.length; ++i)
                    bb[i] = (LatLng) parcelables_bb[i];
                updateBoundingbox(bb);

                Parcelable[] parcelables_pbb = intent.getParcelableArrayExtra("player_boundingbox");
                LatLng[] pbb = new LatLng[parcelables_pbb.length];
                for (int i = 0; i < parcelables_pbb.length; ++i)
                    pbb[i] = (LatLng) parcelables_pbb[i];
                updatePlayerBoundingbox(pbb);

                Parcelable[] parcelables_e = intent.getParcelableArrayExtra("engagements");
                Engagement[] e = new Engagement[parcelables_e.length];
                for (int i = 0; i < parcelables_e.length; ++i)
                    e[i] = (Engagement) parcelables_e[i];
                updateEngagements(e);

                Parcelable[] parcelables_i = intent.getParcelableArrayExtra("items");
                Item[] zitems = new Item[parcelables_i.length];
                for (int i = 0; i < parcelables_i.length; ++i)
                    zitems[i] = (Item) parcelables_i[i];
                updateItems(zitems);

                Parcelable[] parcelables_pi = intent.getParcelableArrayExtra("player_items");
                Item[] pitems = new Item[parcelables_pi.length];
                for (int i = 0; i < parcelables_pi.length; ++i)
                    pitems[i] = (Item) parcelables_pi[i];
                updatePlayerItems(pitems);

                Parcelable[] parcelables_pe = intent.getParcelableArrayExtra("player_engagements");
                PlayerEngagement[] pe = new PlayerEngagement[parcelables_pe.length];
                for (int i = 0; i < parcelables_pe.length; ++i) {
                    pe[i] = (PlayerEngagement) parcelables_pe[i];
                }
                updatePlayerEngagements(pe);

                Parcelable[] parcelables_player_info = intent.getParcelableArrayExtra("player_info");
                PlayerInfo[] pinfo = new PlayerInfo[parcelables_player_info.length];
                for (int i = 0; i < parcelables_player_info.length; ++i) {
                    pinfo[i] = (PlayerInfo) parcelables_player_info[i];
                }
                updatePlayerInfo(pinfo);

                first_sync = false;


                break;
            case NotificationBuilder.NOTIFICATION_OPEN_APP:
                Log.d(TAG, "received NOTIFICATION_OPEN APP -> activity start");
                Intent i = new Intent(NotificationBuilder.context, MainActivity.class);
                i.setAction(Intent.ACTION_MAIN);
                i.addCategory(Intent.CATEGORY_LAUNCHER);
                NotificationBuilder.context.startActivity(i);
                break;
            case NotificationBuilder.NOTIFICATION_RUN:
                Log.d(TAG, "received NOTIFICATION_RUN -> silent reply");

                if (other_players.length >= 1) {
                    Log.d(TAG, "sending PLAYERINTERACTION intent reply");

                    PlayerInteraction playerinteraction = new PlayerInteraction(other_players[0].uid, 1);
                    // set the local state so we do proper handling in the ui
                    if(this.player_engagements.size() == 2) {
                        //this.player_engagements.get(0).state = 1; // FIXME if i'm wrong but i think it's better to handle this completely in the ui code
                    }
                    Intent update = new Intent(SyncService.PLAYERINTERACTION);
                    update.putExtra("playerinteraction", playerinteraction);
                    LocalBroadcastManager.getInstance(context).sendBroadcast(update);
                }
                break;
            case NotificationBuilder.NOTIFICATION_ATTACK:
                Log.d(TAG, "received NOTIFICATION_ATTACK -> silent reply");
                if (other_players.length >= 1) {
                    Log.d(TAG, "sending PLAYERINTERACTION intent reply");

                    PlayerInteraction playerinteraction = new PlayerInteraction(other_players[0].uid, 2);
                    // set the local state so we do proper handling in the ui
                    if(this.player_engagements.size() == 2) {
                        this.player_engagements.get(0).state = 2;
                    }
                    Intent update = new Intent(SyncService.PLAYERINTERACTION);
                    update.putExtra("playerinteraction", playerinteraction);
                    LocalBroadcastManager.getInstance(context).sendBroadcast(update);
                }
                break;
            case NotificationBuilder.NOTIFICATION_BEFRIEND:
                Log.d(TAG, "received NOTIFICATION_BEFRIEND -> silent reply");
                if (other_players.length >= 1) {
                    Log.d(TAG, "sending PLAYERINTERACTION intent reply");

                    PlayerInteraction playerinteraction = new PlayerInteraction(other_players[0].uid, 3);
                    // set the local state so we do proper handling in the ui
                    if(this.player_engagements.size() == 2) {
                        this.player_engagements.get(0).state = 3;
                    }
                    Intent update = new Intent(SyncService.PLAYERINTERACTION);
                    update.putExtra("playerinteraction", playerinteraction);
                    LocalBroadcastManager.getInstance(context).sendBroadcast(update);
                }
                break;

        }
    }

    public Player getPlayer() { return current_player; }
    public Player[] getOther() { return other_players; }
    public Zombie[] getZombies() { return zombies; }
    public LatLng[] getBoundingbox() { return boundingbox; }
    public LatLng[] getPlayerBoundingbox() { return player_boundingbox; }
    public ArrayList<Engagement> getEngagements() { return  engagements; }
    public Item[] getItems() { return items; }

    public Zombie getZombie(long uid) {
        for (Zombie z: zombies
             ) {
            if (z.uid == uid) {
                return z;
            }
        }
        return null;
    }

    public void updateZombieHealth(long uid, int health) {
        for (int i = 0; i < zombies.length; i++) {
            if(zombies[i].uid == uid) {
                zombies[i].health = health;
            }
        }
    }

}
