package de.tr0llhoehle.disease;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.Context;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import java.util.ArrayList;

public class SyncService extends Service {
    public static final String NEW_REMOTE_STATE = "NEW_REMOTE_STATE";
    public static final String INTERACTION = "de.tr0llhoehle.disease.INTERACTION";
    public static final String PLAYERINTERACTION = "de.tr0llhoehle.disease.PLAYERINTERACTION";

    public static int appstate = 0;

    private static final String TAG = "SyncService";

    private ArrayList<Interaction> interaction_buffer = new ArrayList<>();
    private PlayerInteraction player_interaction_buffer = null;

    private ArrayList<Record> send_buffer = new ArrayList<>();
    private long health = -1;
    private BroadcastReceiver location_receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "broadcaster got intent");
            Log.d(TAG, intent.getAction());

            switch (intent.getAction()) {
                case LocationTracker.UPDATE_RECORD:
                    send_buffer.add((Record) intent.getParcelableExtra("record"));
                    send(context);
                    break;
                case SyncService.INTERACTION:
                    Log.d(TAG, "broadcaster got INTERACTION update");

                    Interaction i = (Interaction) intent.getParcelableExtra("interaction");
                    interaction_buffer.clear();
                    interaction_buffer.add(i);

                    //if (!interaction_buffer.contains(i)) {
                        Log.d(TAG, "broadcaster adding interaction to buffer");

                      //  interaction_buffer.add(i);
                    //}
                    health = i.playerhealth;

                    break;
                case SyncService.PLAYERINTERACTION:
                    Log.d(TAG, "broadcaster got PLAYERINTERACTION update");
                    player_interaction_buffer = intent.getParcelableExtra("playerinteraction");

            }
        }
    };

    private ArrayList<Long> acceptedEngagements = new ArrayList<>();

    @Override
    public synchronized IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");

        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(getApplicationContext());
        manager.registerReceiver(location_receiver, new IntentFilter(LocationTracker.UPDATE_RECORD));
        manager.registerReceiver(location_receiver, new IntentFilter(SyncService.INTERACTION));
        manager.registerReceiver(location_receiver, new IntentFilter(SyncService.PLAYERINTERACTION));

        startForeground(NotificationBuilder.getNotificationId(),
                NotificationBuilder.getNotification(getApplicationContext()));
    }

    @Override
    public synchronized int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Started!");
        return START_STICKY;
    }

    private synchronized void send(final Context context) {
        if (send_buffer.size() == 0) return;

        final JsonObject json = new JsonObject();
        JsonArray records = new JsonArray();
        json.add("records", records);

        Gson gson = new GsonBuilder().create();

        for (Record r : send_buffer) {
            JsonObject record  = new JsonObject();
            record.add("lon", gson.toJsonTree(r.lon));
            record.add("lat", gson.toJsonTree(r.lat));
            record.add("timestamp", gson.toJsonTree(r.timestamp));
            record.add("speed", gson.toJsonTree(r.speed));
            record.add("bearing", gson.toJsonTree(r.bearing));
            record.add("accuracy", gson.toJsonTree(r.accuracy));
            record.add("health", gson.toJsonTree(health));
            record.add("appstate", gson.toJsonTree(SyncService.appstate));
            records.add(record);
        }
        if (health == 0) {
            acceptedEngagements.clear(); // hack
        }
        json.add("accepted_engagements", gson.toJsonTree(acceptedEngagements));
        ArrayList<ArrayList<Long>> interactions = new ArrayList<>();
        for (Interaction i : interaction_buffer) {
            ArrayList<Long> inter = new ArrayList();
            inter.add(i.engagement);
            inter.add(i.zombiehealth);
            inter.add(i.playerhealth);
            interactions.add(inter);
        }
        json.add("interactions", gson.toJsonTree(interactions));

        ArrayList<String> player_interactions = new ArrayList<>(); //FIXME: this String is a hack, should be array of long....
        if (player_interaction_buffer != null) {
            Log.d(TAG, "something is in the player_interaction_buffer ");
            player_interactions.add(player_interaction_buffer.otherplayer);
            player_interactions.add(""+player_interaction_buffer.state);
        }

        json.add("player_interactions", gson.toJsonTree(player_interactions));

        Log.d(TAG, "sending json: " + json.toString());

        // FIXME only clear after they arrived
        //send_buffer.clear();
        //interaction_buffer.clear();
        //player_interaction_buffer  = null;

        SettingsManager settings = new SettingsManager(context);

        String query = settings.getServer() + "/update/v2/" + settings.getUserId();

        Log.d(TAG, query);

        Ion.with(context)
                .load(query)
                .setJsonObjectBody(json)
                .asJsonObject()
                .setCallback(new FutureCallback<JsonObject>() {
                    @Override
                    public void onCompleted(Exception e, JsonObject result) {
                        Log.d(TAG, "json sent");
                        if (e != null) {
                            e.printStackTrace();
                            return;
                        }
                        Log.d(TAG, "no catastrophic error received");

                        if (result.has("error")) {
                            Log.d(TAG, result.getAsString());
                            return;
                        }

                        Log.d(TAG, "result has no error");

                        // trying to fix this issue... FIXME only clear after they arrived
                        send_buffer.clear();
                        interaction_buffer.clear();
                        player_interaction_buffer  = null;

                        JsonArray jsonPlayers = result.getAsJsonArray("players");
                        Player[] players = new Player[jsonPlayers.size()];
                        for (int idx = 0; idx < jsonPlayers.size(); idx++) {
                            JsonObject jsonPlayer = jsonPlayers.get(idx).getAsJsonObject();
                            players[idx] = new Player(jsonPlayer.getAsJsonPrimitive("uid").getAsString(),
                                    jsonPlayer.getAsJsonPrimitive("lon").getAsDouble(),
                                    jsonPlayer.getAsJsonPrimitive("lat").getAsDouble(),
                                    jsonPlayer.getAsJsonPrimitive("timestamp").getAsLong(),
                                    jsonPlayer.getAsJsonPrimitive("health").getAsInt());
                        }

                        JsonArray jsonZombies = result.getAsJsonArray("zombies");
                        Zombie[] zombies = new Zombie[jsonZombies.size()];
                        for (int idx = 0; idx < jsonZombies.size(); idx++) {
                            JsonObject jsonZombie = jsonZombies.get(idx).getAsJsonObject();
                            zombies[idx] = new Zombie(jsonZombie.getAsJsonPrimitive("uid").getAsLong(),
                                    jsonZombie.getAsJsonPrimitive("lon").getAsDouble(),
                                    jsonZombie.getAsJsonPrimitive("lat").getAsDouble(),
                                    jsonZombie.getAsJsonPrimitive("timestamp").getAsLong(),
                                    jsonZombie.getAsJsonPrimitive("health").getAsInt(),
                                    jsonZombie.getAsJsonPrimitive("bearing").getAsDouble());
                        }

                        for (Zombie z: zombies
                             ) {
                            Log.i(TAG, z.toString());
                        }
                        JsonArray jsonBB = result.getAsJsonArray("boundingbox");
                        LatLng[] boundingbox = new LatLng[4];
                        if (jsonBB.size() == 2) {
                            JsonObject tl = jsonBB.get(0).getAsJsonObject();
                            JsonObject br = jsonBB.get(1).getAsJsonObject();

                            boundingbox[0] = new LatLng(tl.getAsJsonPrimitive("lat").getAsDouble(), tl.getAsJsonPrimitive("lon").getAsDouble());
                            boundingbox[1] = new LatLng(br.getAsJsonPrimitive("lat").getAsDouble(), tl.getAsJsonPrimitive("lon").getAsDouble());
                            boundingbox[2] = new LatLng(br.getAsJsonPrimitive("lat").getAsDouble(), br.getAsJsonPrimitive("lon").getAsDouble());
                            boundingbox[3] = new LatLng(tl.getAsJsonPrimitive("lat").getAsDouble(), br.getAsJsonPrimitive("lon").getAsDouble());
                        }

                        Log.i(TAG, "BB:"+boundingbox[0]+" "+boundingbox[1]+" "+boundingbox[2]+" "+boundingbox[3]+" ");

                        JsonArray jsonPBB = result.getAsJsonArray("player_boundingbox");
                        LatLng[] player_boundingbox = new LatLng[4];
                        if (jsonPBB.size() == 2) {
                            JsonObject tl = jsonPBB.get(0).getAsJsonObject();
                            JsonObject br = jsonPBB.get(1).getAsJsonObject();

                            player_boundingbox[0] = new LatLng(tl.getAsJsonPrimitive("lat").getAsDouble(), tl.getAsJsonPrimitive("lon").getAsDouble());
                            player_boundingbox[1] = new LatLng(br.getAsJsonPrimitive("lat").getAsDouble(), tl.getAsJsonPrimitive("lon").getAsDouble());
                            player_boundingbox[2] = new LatLng(br.getAsJsonPrimitive("lat").getAsDouble(), br.getAsJsonPrimitive("lon").getAsDouble());
                            player_boundingbox[3] = new LatLng(tl.getAsJsonPrimitive("lat").getAsDouble(), br.getAsJsonPrimitive("lon").getAsDouble());
                        }

                        Log.i(TAG, "PBB:"+player_boundingbox[0]+" "+player_boundingbox[1]+" "+player_boundingbox[2]+" "+player_boundingbox[3]+" ");

                        JsonArray jsonEngagements = result.getAsJsonArray("engagements");
                        Engagement[] engagements = new Engagement[jsonEngagements.size()];

                        acceptedEngagements.clear();
                        for (int idx = 0; idx < jsonEngagements.size(); idx++) {
                            JsonObject jsonEngagement = jsonEngagements.get(idx).getAsJsonObject();
                            engagements[idx] = new Engagement(jsonEngagement.getAsJsonPrimitive("playeruid").getAsString(),
                                    jsonEngagement.getAsJsonPrimitive("zombieuid").getAsLong(),
                                    jsonEngagement.getAsJsonPrimitive("timestamp").getAsLong(),
                                    jsonEngagement.getAsJsonPrimitive("active").getAsInt(),
                                    jsonEngagement.getAsJsonPrimitive("accepted").getAsInt());
                            acceptedEngagements.add(jsonEngagement.getAsJsonPrimitive("zombieuid").getAsLong());
                        }

                        JsonArray jsonItems = result.getAsJsonArray("items");
                        Item[] items = new Item[jsonItems.size()];
                        for (int idx = 0; idx < jsonItems.size(); idx++) {
                            JsonObject jsonItem = jsonItems.get(idx).getAsJsonObject();
                            items[idx] = new Item(jsonItem.getAsJsonPrimitive("itemuid").getAsLong(),
                                    jsonItem.getAsJsonPrimitive("owneruid").getAsLong(),
                                    jsonItem.getAsJsonPrimitive("itemtype").getAsInt(),
                                    jsonItem.getAsJsonPrimitive("timestamp").getAsLong(),
                                    jsonItem.getAsJsonPrimitive("lon").getAsDouble(),
                                    jsonItem.getAsJsonPrimitive("lat").getAsDouble());
                        }

                        JsonArray jsonPlayerItems = result.getAsJsonArray("player_items");
                        Item[] player_items = new Item[jsonPlayerItems.size()];
                        for (int idx = 0; idx < jsonPlayerItems.size(); idx++) {
                            JsonObject jsonPlayerItem = jsonPlayerItems.get(idx).getAsJsonObject();
                            player_items[idx] = new Item(jsonPlayerItem.getAsJsonPrimitive("itemuid").getAsLong(),
                                    jsonPlayerItem.getAsJsonPrimitive("owneruid").getAsLong(),
                                    jsonPlayerItem.getAsJsonPrimitive("itemtype").getAsInt(),
                                    jsonPlayerItem.getAsJsonPrimitive("timestamp").getAsLong(),
                                    jsonPlayerItem.getAsJsonPrimitive("lon").getAsDouble(),
                                    jsonPlayerItem.getAsJsonPrimitive("lat").getAsDouble());
                        }

                        JsonArray jsonPlayerEngagements = result.getAsJsonArray("player_engagements");
                        PlayerEngagement[] player_engagements = new PlayerEngagement[jsonPlayerEngagements.size()];

                        for (int idx = 0; idx < jsonPlayerEngagements.size(); idx++) {
                            JsonObject jsonPlayerEngagement = jsonPlayerEngagements.get(idx).getAsJsonObject();
                            player_engagements[idx] = new PlayerEngagement(jsonPlayerEngagement.getAsJsonPrimitive("player1uid").getAsString(),
                                    jsonPlayerEngagement.getAsJsonPrimitive("player2uid").getAsString(),
                                    jsonPlayerEngagement.getAsJsonPrimitive("timestamp").getAsLong(),
                                    jsonPlayerEngagement.getAsJsonPrimitive("active").getAsInt(),
                                    jsonPlayerEngagement.getAsJsonPrimitive("state").getAsInt());
                        }

                        JsonArray jsonPlayerInfos = result.getAsJsonArray("player_info");
                        PlayerInfo[] player_info = new PlayerInfo[jsonPlayerInfos.size()];

                        for (int idx = 0; idx < jsonPlayerInfos.size(); idx++) {
                            JsonObject jsonPlayerInfo = jsonPlayerInfos.get(idx).getAsJsonObject();
                            player_info[idx] = new PlayerInfo(jsonPlayerInfo.getAsJsonPrimitive("playeruid").getAsString(),
                                    jsonPlayerInfo.getAsJsonPrimitive("xp").getAsInt(),
                                    jsonPlayerInfo.getAsJsonPrimitive("health").getAsInt());
                            health = jsonPlayerInfo.getAsJsonPrimitive("health").getAsInt(); //FIXME: dirty hack
                        }

                        Intent intent = new Intent(SyncService.NEW_REMOTE_STATE);
                        intent.putExtra("players", players);
                        intent.putExtra("zombies", zombies);
                        intent.putExtra("boundingbox", boundingbox);
                        intent.putExtra("player_boundingbox", player_boundingbox);
                        intent.putExtra("engagements", engagements);
                        intent.putExtra("items", items);
                        intent.putExtra("player_items", player_items);
                        intent.putExtra("player_engagements", player_engagements);
                        intent.putExtra("player_info", player_info);
                        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
                        // todo update internal state to server state
                    }
                });
    }
}
