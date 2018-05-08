package de.tr0llhoehle.disease;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.Fragment;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;

import java.util.ArrayList;
import java.util.Random;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback, SurfaceHolder.Callback {
    static final String TAG = "MainActivity";
    private static final int player_to_zombie_damage = 40;
    private static final int zombie_to_player_damage = 20;
    private final float[] accelerometerReading = new float[3];
    private final float[] magnetometerReading = new float[3];
    private final float[] rotationMatrix = new float[9];
    private final float[] orientationAngles = new float[3];
    GameModel model = new GameModel();
    Handler handler = new Handler();
    SettingsManager settings;
    private GoogleMap mMap;
    private Marker player_marker;
    private ArrayList<Marker> zombie_markers;
    private ArrayList<Marker> other_player_markers;
    private ArrayList<Marker> items_markers;
    private Polygon boundingbox;
    private Polygon player_boundingbox;
    private LocalBroadcastManager manager;
    private LatLng lastLatLng = null;
    private long last_updated = 0;
    private int last_health = 0;
    private int last_zombie_health = 0;
    private boolean inventory_open = false;
    private String uid;

    private boolean zombie_engagement_running = false;

    private ObjectAnimator progressAnimatorLeft;
    private ObjectAnimator progressAnimatorCenter;
    private ObjectAnimator progressAnimatorRight;

    private CompoundButton.OnCheckedChangeListener switch_portrait_listener = null;
    private CompoundButton.OnCheckedChangeListener switch_landscape_listener = null;

    private UIState currentUIState = UIState.INIT;

    private TutorialState currentTutorialState = TutorialState.NONE;

    //TODO: the following is an attempt to cache as many resource loads as possible
    private Bitmap health_bg;
    private Bitmap health_bar_human;
    private Bitmap health_bar_zombie;

    private Button attack_button;
    private Button attack_button_landscape;
    private Button defend_button;
    private Button defend_button_landscape;
    private Button healing_button;
    private Button healing_button_landscape;
    private Button attack_person;
    private Button attack_person_landscape;
    private Button befriend_person;
    private Button befriend_person_landscape;
    private Button run_away;
    private Button run_away_landscape;

    private ImageView health_bar_portrait;
    private ImageView health_bar_landscape;
    private ImageView zombie_health_bar_portrait;
    private ImageView zombie_health_bar_landscape;

    private TextView health_txt_portrait;
    private TextView health_txt_landscape;

    private TextView food_text_portrait;
    private TextView drink_text_portrait;
    private TextView food_text_landscape;
    private TextView drink_text_landscape;

    private ImageView engagement_portrait;
    private ImageView engagement_landscape;
    private View map;

    private Drawable zombie_vertical;
    private Drawable zombie_vertical_readying;
    private Drawable zombie_vertical_hit;
    private Drawable zombie_vertical_hit_by_player;
    private Drawable zombie_vertical_defended;
    private Drawable zombie_vertical_defeated;
    private Drawable zombie_vertical_killed;
    private Drawable person_vertical;
    private Drawable person_vertical_you_ran_away;
    private Drawable person_vertical_they_ran_away;
    private Drawable person_vertical_waiting;
    private Drawable person_vertical_both_attack;
    private Drawable person_vertical_both_befriend;
    private Drawable person_vertical_won_fight;
    private Drawable person_vertical_lost_fight;
    private Drawable person_vertical_got_robbed;
    private Drawable person_vertical_successful_robbing;
    private Drawable beans_vertical;
    private Drawable beans_horizontal;
    private Drawable whiskey_vertical;
    private Drawable whiskey_horizontal;


    private Runnable map_screen = new Runnable() {
        @Override
        public void run() {
            model.engaged = false;
            setUIState(UIState.MAP);
        }
    };
    //TODO: THIS IS THE MAIN GAMELOOP
    Runnable refreshDebugDisplay = new Runnable() {
        @Override
        public void run() {
            try {
                if(settings.getTutorialMode()) {
                    return;
                }
                long current_time = System.currentTimeMillis();

                Player player = model.getPlayer();
                Player[] other = model.getOther();
                Zombie[] zombies = model.getZombies();
                Item[] items = model.getItems();

                updateHealth(player.health);
                updatePotions(model.potions.size());

                ArrayList<Engagement> engagements = model.getEngagements();

                ArrayList<PlayerEngagement> player_engagements = model.getPlayerEngagements();

                // TODO: FIXME: when is model.engaged reset?

                // TODO: size must be 2 because we get both sides of the "story" here
                // FIXME: was model.foreground really needed here?
                if(!model.engaged && model.player_engaged && player_engagements.size() == 2 && player.health > 0) {
                    // we know that there is an engagement. now we just have to check the state of both players engagement objects:
                    PlayerEngagement e0 = player_engagements.get(0);
                    PlayerEngagement e1 = player_engagements.get(1);
                    foregroundPlayerInteraction(e0, e1);
                }

                if(player_engagements.size() == 1) {
                    Log.d(TAG, "#### GOT ONLY HALF A ENGAGEMENT BACK ####");
                    Log.d(TAG, player_engagements.toString());
                    PlayerEngagement e0 = player_engagements.get(0);
                    Log.d(TAG, "uid: " + uid + "  euid: " + e0.player1uid);

                    if(uid.equals(e0.player1uid) && e0.state == 100) { // we ran away
                        setUIState(UIState.PERSON_YOU_RAN_AWAY);

                        handler.postDelayed(map_screen, 2000);
                        model.player_engaged = false;
                        player_engagements.clear();
                    } else if(uid.equals(e0.player1uid) && e0.state == 101) { // they ran away
                        setUIState(UIState.PERSON_THEY_RAN_AWAY);

                        handler.postDelayed(map_screen, 2000);
                        model.player_engaged = false;
                        player_engagements.clear();
                    }
                }

                if(player_engagements.size() == 1 || player_engagements.size() == 2) {
                    PlayerEngagement e0 = player_engagements.get(0);
                    if(e0.state == 200) { // won fight
                        setUIState(UIState.PERSON_WON_FIGHT);

                        handler.postDelayed(map_screen, 2000);
                        model.player_engaged = false;
                        player_engagements.clear();

                    } else if(e0.state == 201) { // lost fight
                        setUIState(UIState.PERSON_LOST_FIGHT);

                        handler.postDelayed(map_screen, 2000);
                        model.player_engaged = false;
                        player_engagements.clear();
                    } else if(e0.state == 42) { // both befriended
                        setUIState(UIState.PERSON_BOTH_BEFRIEND);

                        handler.postDelayed(map_screen, 2000);
                        model.player_engaged = false;
                        player_engagements.clear();
                    } else if(e0.state == 300) { // robbing success
                        setUIState(UIState.PERSON_ROBBING_SUCCESS);

                        handler.postDelayed(map_screen, 2000);
                        model.player_engaged = false;
                        player_engagements.clear();
                    } else if(e0.state == 301) { // robbed
                        setUIState(UIState.PERSON_GOT_ROBBED);

                        handler.postDelayed(map_screen, 2000);
                        model.player_engaged = false;
                        player_engagements.clear();
                    }
                }

                if(!zombie_engagement_running && !model.player_engaged && player_engagements.size() == 0) {
                    setUIState(UIState.MAP); //FIXME?, this will create problems with item pickup
                }

                // BACKGROUND ZOMBIE KILLING - needs some tweaking TODO: use (zombie_engagement_running || !model.player_engaged)?
                if (!model.player_engaged && (model.foreground == false || !model.interactive_mode) && engagements.size() == 1 && engagements.get(0).active == 1 && player.health > 0) {
                    backgroundZombieKilling();
                }

                // FOREGROUND ZOMBIE KILLING (displaying of ui)
                if ((zombie_engagement_running || !model.player_engaged) && model.interactive_mode && model.foreground && engagements.size() == 1 && engagements.get(0).active == 1 && player.health > 0) {
                    foregroundZombieKilling(engagements.get(0), zombies, current_time);
                }

                // UI for player death
                if (!model.player_engaged && model.engaged && model.getPlayer().health == 0) {
                    setUIState(UIState.KILLED);
                    model.engaged = false;
                    zombie_engagement_running = false;
                    //Handler handler = new Handler();
                    handler.postDelayed(map_screen, 1000);
                }

                // update maps display
                if (model.foreground && mMap != null) { // && !model.engaged) {
                    updateMap(zombies, player, other, items);
                }

                // show the latest item that was picked up
                if(!model.player_engaged && model.newest_item != null) {
                    if (model.newest_item.itemtype == 0) {
                        setUIState(UIState.PICKED_UP_FOOD);
                        //Handler handler = new Handler();
                        handler.postDelayed(map_screen, 1000);
                    } else if (model.newest_item.itemtype == 1) {
                        setUIState(UIState.PICKED_UP_DRINK);
                        //Handler handler = new Handler();
                        handler.postDelayed(map_screen, 1000);
                    }
                    model.newest_item = null;
                }
            } finally {
                handler.postDelayed(refreshDebugDisplay, 250);
            }
        }
    };

    private void disableElement(UIElement element) {
        switch (element) {
            case ATTACK_BUTTON:
                attack_button.setEnabled(false);
                break;
            case DEFEND_BUTTON:
                defend_button.setEnabled(false);
                break;
            case HEALING_BUTTON:
                healing_button.setEnabled(false);
                break;
            case ATTACK_PERSON:
                attack_person.setEnabled(false);
                break;
            case BEFRIEND_PERSON:
                befriend_person.setEnabled(false);
                break;
            case RUN_AWAY:
                run_away.setEnabled(false);
                break;
        }
    }

    private void enableElement(UIElement element) {
        switch (element) {
            case ATTACK_BUTTON:
                attack_button.setEnabled(true);
                break;
            case DEFEND_BUTTON:
                defend_button.setEnabled(true);
                break;
            case HEALING_BUTTON:
                healing_button.setEnabled(true);
                break;
            case ATTACK_PERSON:
                attack_person.setEnabled(true);
                break;
            case BEFRIEND_PERSON:
                befriend_person.setEnabled(true);
                break;
            case RUN_AWAY:
                run_away.setEnabled(true);
                break;
        }
    }

    private void hideElement(UIElement element) {
        // TODO: does hiding need setEnabled=false?
        switch (element) {
            case ATTACK_BUTTON:
                attack_button.setVisibility(View.GONE);
                attack_button_landscape.setVisibility(View.GONE);
                break;
            case DEFEND_BUTTON:
                defend_button.setVisibility(View.GONE);
                defend_button_landscape.setVisibility(View.GONE);
                break;
            case HEALING_BUTTON:
                healing_button.setVisibility(View.GONE);
                healing_button_landscape.setVisibility(View.GONE);
                break;
            case ATTACK_PERSON:
                attack_person.setVisibility(View.GONE);
                attack_person_landscape.setVisibility(View.GONE);
                break;
            case BEFRIEND_PERSON:
                befriend_person.setVisibility(View.GONE);
                befriend_person_landscape.setVisibility(View.GONE);
                break;
            case RUN_AWAY:
                run_away.setVisibility(View.GONE);
                run_away_landscape.setVisibility(View.GONE);
                break;
            case INVENTORY:
                // portrait
                ImageView inventory = findViewById(R.id.inventory_portrait);
                inventory.setVisibility(View.INVISIBLE);
                TextView food = findViewById(R.id.food_amount_portrait);
                food.setVisibility(View.INVISIBLE);
                TextView drink = findViewById(R.id.drink_amount_portrait);
                drink.setVisibility(View.INVISIBLE);
                // landscape
                ImageView inventory_landscape = findViewById(R.id.inventory_landscape);
                inventory_landscape.setVisibility(View.INVISIBLE);
                TextView food_landscape = findViewById(R.id.food_amount_landscape);
                food_landscape.setVisibility(View.INVISIBLE);
                TextView drink_landscape = findViewById(R.id.drink_amount_landscape);
                drink_landscape.setVisibility(View.INVISIBLE);
                break;
            case ZOMBIE_HEALTH_BAR:
                zombie_health_bar_portrait.setVisibility(View.INVISIBLE);
                zombie_health_bar_landscape.setVisibility(View.INVISIBLE);
                break;
        }
    }

    private void showElement(UIElement element) {
        switch (element) {
            case ATTACK_BUTTON:
                attack_button.setVisibility(View.VISIBLE);
                attack_button_landscape.setVisibility(View.VISIBLE);
                break;
            case DEFEND_BUTTON:
                defend_button.setVisibility(View.VISIBLE);
                defend_button_landscape.setVisibility(View.VISIBLE);
                break;
            case HEALING_BUTTON:
                healing_button.setVisibility(View.VISIBLE);
                healing_button_landscape.setVisibility(View.VISIBLE);
                break;
            case ATTACK_PERSON:
                attack_person.setVisibility(View.VISIBLE);
                attack_person_landscape.setVisibility(View.VISIBLE);
                break;
            case BEFRIEND_PERSON:
                befriend_person.setVisibility(View.VISIBLE);
                befriend_person_landscape.setVisibility(View.VISIBLE);
                break;
            case RUN_AWAY:
                run_away.setVisibility(View.VISIBLE);
                run_away_landscape.setVisibility(View.VISIBLE);
                break;
            case INVENTORY:
                // portrait
                ImageView inventory = findViewById(R.id.inventory_portrait);
                inventory.setVisibility(View.VISIBLE);
                TextView food = findViewById(R.id.food_amount_portrait);
                food.setVisibility(View.VISIBLE);
                TextView drink = findViewById(R.id.drink_amount_portrait);
                drink.setVisibility(View.VISIBLE);
                // landscape
                ImageView inventory_landscape = findViewById(R.id.inventory_landscape);
                inventory_landscape.setVisibility(View.VISIBLE);
                TextView food_landscape = findViewById(R.id.food_amount_landscape);
                food_landscape.setVisibility(View.VISIBLE);
                TextView drink_landscape = findViewById(R.id.drink_amount_landscape);
                drink_landscape.setVisibility(View.VISIBLE);
                break;
            case ZOMBIE_HEALTH_BAR:
                zombie_health_bar_portrait.setVisibility(View.VISIBLE);
                zombie_health_bar_landscape.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void idleMode() {
        hideElement(UIElement.ATTACK_BUTTON);
        hideElement(UIElement.DEFEND_BUTTON);
        hideElement(UIElement.HEALING_BUTTON);
        hideElement(UIElement.ATTACK_PERSON);
        hideElement(UIElement.BEFRIEND_PERSON);
        hideElement(UIElement.RUN_AWAY);
        hideElement(UIElement.ZOMBIE_HEALTH_BAR);
    }

    private void zombieEngagementMode() {
        showElement(UIElement.ATTACK_BUTTON);
        showElement(UIElement.DEFEND_BUTTON);
        showElement(UIElement.HEALING_BUTTON);
        showElement(UIElement.ZOMBIE_HEALTH_BAR);
    }

    private void personEngagementMode() {
        showElement(UIElement.ATTACK_PERSON);
        showElement(UIElement.BEFRIEND_PERSON);
        showElement(UIElement.RUN_AWAY);
    }

    public void openInventory(View view) {
        if(!inventory_open) {
            updateInventory(model.getPlayerItems());

            showElement(UIElement.INVENTORY);
        } else {
            hideElement(UIElement.INVENTORY);
        }
        inventory_open = !inventory_open;
    }

    public void attackZombie(View view) {
        if (!model.engaged) {
            return;
        }
        Log.d(TAG, "ATTACK PRESSED");
        for (Engagement e : model.getEngagements()
                ) {
            Zombie z = model.getZombie(e.zombieuid);
            //e.active = 0;
            e.displayed = false;
            Log.d(TAG, "hit engagement codepath");

            if (z != null) {
                model.last_player_attack = System.currentTimeMillis();
                // disable attack button
                disableElement(UIElement.ATTACK_BUTTON);
                progressAnimatorLeft.start();
                setUIState(UIState.ZOMBIE_HIT_BY_PLAYER);

                //attack_button.setText("Attack Zombie (cooldown)");

                z.health -= player_to_zombie_damage;
                if (z.health <= 0) {
                    z.health = 0;
                    e.active = 0;
                    model.last_zombie_attack = 0;
                    // FIXME: this might not be what we want sometimes?
                    /*setUIState(UIState.MAP);*/

                    model.engaged = false;
                    zombie_engagement_running = false;
                    setUIState(UIState.ZOMBIE_DEFEATED);
                    //Handler handler = new Handler();
                    handler.postDelayed(map_screen, 1000);
                }
                updateZombieHealth(z.health);
                Log.d(TAG, "sending INTERACTION update");

                model.updateZombieHealth(e.zombieuid, z.health);
                Interaction interaction = new Interaction(e.zombieuid, z.health, model.getPlayer().health);
                Intent update = new Intent(SyncService.INTERACTION);
                update.putExtra("interaction", interaction);
                this.manager.sendBroadcast(update);
            }
        }

    }

    public void defendZombie(View view) {
        if (!model.engaged) {
            return;
        }
        model.last_player_defend = System.currentTimeMillis();
        disableElement(UIElement.DEFEND_BUTTON);
        progressAnimatorCenter.start();

        // FIXME: this should prevent us from receiving damage
        if (model.zombie_attack_state == 1) {
            model.zombie_attack_state = 0;
            model.last_zombie_attack = System.currentTimeMillis();
            setUIState(UIState.ZOMBIE_DEFENDED);

            resetZombieAttack();
        }
    }

    public void closeApp(View view) {
        stopService(new Intent(this, LocationTracker.class));
        stopService(new Intent(this, SyncService.class));
        stopService(new Intent(this, NotificationBuilder.class));

        System.exit(0);
    }

    public void tutorial(View view) {
        this.settings.enableTutorialMode();
        tutorialMode();
    }
/*
    public void restockPotions(MenuItem item) {
        if (model.potions.size() == 0) {
            for (int i = 0; i < 5; i++) {
                model.potions.add(new Potion(20));
            }
        }
    }

    public void showHelp(MenuItem item) {
        Context context = getApplicationContext();
        CharSequence text = "Showing help...";
        int duration = Toast.LENGTH_SHORT;

        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
    }

    public void menu(View view) {
        PopupMenu popup = new PopupMenu(this, view);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.game_menu, popup.getMenu());
        popup.show();
    }

    public void shelter(View view) {

    }*/

    public void heal(View view) {
        if (!model.engaged) {
            return;
        }
        if (model.potions.size() > 0) {
            Potion p = model.potions.get(0);
            model.getPlayer().health += p.healing;
            model.potions.remove(0);
        }
        model.last_player_potion = System.currentTimeMillis();
        disableElement(UIElement.HEALING_BUTTON);
        progressAnimatorRight.start();
    }

    public void runAway(View view) {
        Intent intentSend = new Intent(NotificationBuilder.NOTIFICATION_RUN);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intentSend);
        model.player_engaged = false;
        setUIState(UIState.PERSON_YOU_RAN_AWAY);
        //Handler handler = new Handler();
        handler.postDelayed(map_screen, 2000);
    }

    public void attackPerson(View view) {
        Intent intentSend = new Intent(NotificationBuilder.NOTIFICATION_ATTACK);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intentSend);
    }

    public void befriendPerson(View view) {
        Intent intentSend = new Intent(NotificationBuilder.NOTIFICATION_BEFRIEND);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intentSend);
    }

    private Bitmap scaleHealthbar(int health, int foreground) {
        Bitmap health_bar;
        if (foreground == R.drawable.health_bar_zombie) {
            health_bar = this.health_bar_zombie;
        } else {
            health_bar = this.health_bar_human;
        }
        if (health_bg != null && health_bar != null) {

            Bitmap health_display = Bitmap.createBitmap(health_bg.getWidth(), health_bg.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(health_display);
            int width = health_bg.getWidth() * health / 100;
            if (health > 0) {
                if (health > 100) {
                    width = health_bg.getWidth();
                }
                health_bar = Bitmap.createBitmap(health_bar, 0, 0, width, health_bg.getHeight());
                canvas.drawBitmap(health_bar, 0, 0, null);
            }
            canvas.drawBitmap(health_bg, 0, 0, null);
            return health_display;
        }
        return null;
    }

    private void updateZombieHealth(int health) {
        if (health != last_zombie_health) {
            last_zombie_health = health;

            Bitmap health_display = scaleHealthbar(health, R.drawable.health_bar_zombie);
            if (health_display != null) {
                zombie_health_bar_portrait.setImageBitmap(health_display);
                zombie_health_bar_landscape.setImageBitmap(health_display);
            }
        }
    }

    private void updateHealth(int health) {
        if (health != last_health) {
            last_health = health;

            if (health == -1) {
                // still in init phase
                health_txt_portrait.setText("Health: n/a");
                health_txt_landscape.setText("Health: n/a");
            } else {
                health_txt_portrait.setText("Health: " + health);
                health_txt_landscape.setText("Health: " + health);
            }

            Bitmap health_display = scaleHealthbar(health, R.drawable.health_bar);
            if (health_display != null) {
                health_bar_portrait.setImageBitmap(health_display);
                health_bar_landscape.setImageBitmap(health_display);
            }
        }
    }

    private void updateInventory(ArrayList<Item> player_items) {
        int food = 0;
        int drink = 0;
        for (Item item: player_items
             ) {
            if(item.itemtype == 0) {
                food += 1;
            }
            if(item.itemtype == 1) {
                drink += 1;
            }
        }

        food_text_portrait.setText(""+food);
        drink_text_portrait.setText(""+drink);
        food_text_landscape.setText(""+food);
        drink_text_landscape.setText(""+drink);
    }

    private void resetProgressBars() {
        progressAnimatorLeft.cancel();
        progressAnimatorCenter.cancel();
        progressAnimatorRight.cancel();

        ProgressBar progressBarLeft = findViewById(R.id.progressBarLeftPortrait);
        progressBarLeft.setProgress(0);
        ProgressBar progressBarCenter = findViewById(R.id.progressBarCenterPortrait);
        progressBarCenter.setProgress(0);
        ProgressBar progressBarRight = findViewById(R.id.progressBarRightPortrait);
        progressBarRight.setProgress(0);
    }


    private long last_update = 0;
    private long delay_time = 1000;

    private void setUIState(UIState state) {
        if (state == currentUIState) {
            return;
        }

        long current_time = System.currentTimeMillis();
        if(last_update+delay_time >= current_time) {
            Log.d(TAG, "SET UI STATE: delay time not yet reached. no change!");
            return;
        }

        currentUIState = state;
        last_update = current_time;

        Log.d(TAG, "SET UI STATE: " + state.toString());

        // FIXME:

        Notification notification;
        if (state != UIState.MAP) {
            map.setVisibility(View.GONE);
        }
        switch (state) {
            case MAP:
                map.setVisibility(View.VISIBLE);
                engagement_portrait.setVisibility(View.GONE);
                engagement_landscape.setVisibility(View.INVISIBLE);
                idleMode();
                break;
            case ZOMBIE:
                engagement_portrait.setImageDrawable(zombie_vertical);
                engagement_portrait.setVisibility(View.VISIBLE);
                engagement_landscape.setImageDrawable(zombie_vertical);
                engagement_landscape.setVisibility(View.VISIBLE);
                zombieEngagementMode();
                break;
            case ZOMBIE_READYING:
                engagement_portrait.setImageDrawable(zombie_vertical_readying);
                engagement_portrait.setVisibility(View.VISIBLE);
                engagement_landscape.setImageDrawable(zombie_vertical_readying);
                engagement_landscape.setVisibility(View.VISIBLE);
                break;
            case ZOMBIE_HIT:
                engagement_portrait.setImageDrawable(zombie_vertical_hit);
                engagement_portrait.setVisibility(View.VISIBLE);
                engagement_landscape.setImageDrawable(zombie_vertical_hit);
                engagement_landscape.setVisibility(View.VISIBLE);
                break;
            case ZOMBIE_HIT_BY_PLAYER:
                engagement_portrait.setImageDrawable(zombie_vertical_hit_by_player);
                engagement_portrait.setVisibility(View.VISIBLE);
                engagement_landscape.setImageDrawable(zombie_vertical_hit_by_player);
                engagement_landscape.setVisibility(View.VISIBLE);
                break;
            case ZOMBIE_DEFENDED:
                engagement_portrait.setImageDrawable(zombie_vertical_defended);
                engagement_portrait.setVisibility(View.VISIBLE);
                engagement_landscape.setImageDrawable(zombie_vertical_defended);
                engagement_landscape.setVisibility(View.VISIBLE);
                break;
            case ZOMBIE_DEFEATED:
                engagement_portrait.setImageDrawable(zombie_vertical_defeated);
                engagement_portrait.setVisibility(View.VISIBLE);
                engagement_landscape.setImageDrawable(zombie_vertical_defeated);
                engagement_landscape.setVisibility(View.VISIBLE);
                resetProgressBars();
                break;
            case KILLED:
                engagement_portrait.setImageDrawable(zombie_vertical_killed);
                engagement_portrait.setVisibility(View.VISIBLE);
                engagement_landscape.setImageDrawable(zombie_vertical_killed);
                engagement_landscape.setVisibility(View.VISIBLE);
                resetProgressBars();
                break;
            case PERSON:
                engagement_portrait.setImageDrawable(person_vertical);
                engagement_portrait.setVisibility(View.VISIBLE);
                engagement_landscape.setImageDrawable(person_vertical);
                engagement_landscape.setVisibility(View.VISIBLE);
                personEngagementMode();
                break;
            case PERSON_YOU_RAN_AWAY:
                engagement_portrait.setImageDrawable(person_vertical_you_ran_away);
                engagement_portrait.setVisibility(View.VISIBLE);
                engagement_landscape.setImageDrawable(person_vertical_you_ran_away);
                engagement_landscape.setVisibility(View.VISIBLE);
                break;
            case PERSON_THEY_RAN_AWAY:
                engagement_portrait.setImageDrawable(person_vertical_they_ran_away);
                engagement_portrait.setVisibility(View.VISIBLE);
                engagement_landscape.setImageDrawable(person_vertical_they_ran_away);
                engagement_landscape.setVisibility(View.VISIBLE);
                notification = new NotificationCompat.Builder(getApplicationContext())
                        .setContentTitle("Disease")
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentText("The other player ran away.")
                        .build();
                NotificationBuilder.manager.notify(NotificationBuilder.getNotificationId(), notification);
                break;
            case PERSON_WAITING:
                engagement_portrait.setImageDrawable(person_vertical_waiting);
                engagement_portrait.setVisibility(View.VISIBLE);
                engagement_landscape.setImageDrawable(person_vertical_waiting);
                engagement_landscape.setVisibility(View.VISIBLE);
                break;
            case PERSON_BOTH_ATTACK:
                engagement_portrait.setImageDrawable(person_vertical_both_attack);
                engagement_portrait.setVisibility(View.VISIBLE);
                engagement_landscape.setImageDrawable(person_vertical_both_attack);
                engagement_landscape.setVisibility(View.VISIBLE);
                break;
            case PERSON_BOTH_BEFRIEND:
                engagement_portrait.setImageDrawable(person_vertical_both_befriend);
                engagement_portrait.setVisibility(View.VISIBLE);
                engagement_landscape.setImageDrawable(person_vertical_both_befriend);
                engagement_landscape.setVisibility(View.VISIBLE);
                notification = new NotificationCompat.Builder(getApplicationContext())
                        .setContentTitle("Disease")
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentText("You made another friend.")
                        .build();
                NotificationBuilder.manager.notify(NotificationBuilder.getNotificationId(), notification);
                break;
            case PERSON_WON_FIGHT:
                engagement_portrait.setImageDrawable(person_vertical_won_fight);
                engagement_portrait.setVisibility(View.VISIBLE);
                engagement_landscape.setImageDrawable(person_vertical_won_fight);
                engagement_landscape.setVisibility(View.VISIBLE);
                notification = new NotificationCompat.Builder(getApplicationContext())
                        .setContentTitle("Disease")
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentText("You won the fight.")
                        .build();
                NotificationBuilder.manager.notify(NotificationBuilder.getNotificationId(), notification);
                break;
            case PERSON_LOST_FIGHT:
                engagement_portrait.setImageDrawable(person_vertical_lost_fight);
                engagement_portrait.setVisibility(View.VISIBLE);
                engagement_landscape.setImageDrawable(person_vertical_lost_fight);
                engagement_landscape.setVisibility(View.VISIBLE);
                notification = new NotificationCompat.Builder(getApplicationContext())
                        .setContentTitle("Disease")
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentText("You lost the fight.")
                        .build();
                NotificationBuilder.manager.notify(NotificationBuilder.getNotificationId(), notification);
                break;
            case PERSON_GOT_ROBBED:
                engagement_portrait.setImageDrawable(person_vertical_got_robbed);
                engagement_portrait.setVisibility(View.VISIBLE);
                engagement_landscape.setImageDrawable(person_vertical_got_robbed);
                engagement_landscape.setVisibility(View.VISIBLE);
                notification = new NotificationCompat.Builder(getApplicationContext())
                        .setContentTitle("Disease")
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentText("You got robbed.")
                        .build();
                NotificationBuilder.manager.notify(NotificationBuilder.getNotificationId(), notification);
                break;
            case PERSON_ROBBING_SUCCESS:
                engagement_portrait.setImageDrawable(person_vertical_successful_robbing);
                engagement_portrait.setVisibility(View.VISIBLE);
                engagement_landscape.setImageDrawable(person_vertical_successful_robbing);
                engagement_landscape.setVisibility(View.VISIBLE);
                notification = new NotificationCompat.Builder(getApplicationContext())
                        .setContentTitle("Disease")
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentText("You robbed the other player successfully.")
                        .build();
                NotificationBuilder.manager.notify(NotificationBuilder.getNotificationId(), notification);
                break;
            case PICKED_UP_FOOD:
                engagement_portrait.setImageDrawable(beans_vertical);
                engagement_portrait.setVisibility(View.VISIBLE);
                engagement_landscape.setImageDrawable(beans_horizontal);
                engagement_landscape.setVisibility(View.VISIBLE);
                break;
            case PICKED_UP_DRINK:
                engagement_portrait.setImageDrawable(whiskey_vertical);
                engagement_portrait.setVisibility(View.VISIBLE);
                engagement_landscape.setImageDrawable(whiskey_horizontal);
                engagement_landscape.setVisibility(View.VISIBLE);
                break;
        }

    }

    private void updatePotions(int potions) {
        //TextView potions_portrait = findViewById(R.id.potions_portrait);
        //potions_portrait.setText("Potions: " + potions);
    }

    private void updateMapCamera(LatLng pos) {
        lastLatLng = pos;
        updateMapCamera();
    }

    private void updateMapCamera() {
        if (lastLatLng != null) {
            float latitude_factor = 0.003f;
            if (!model.portrait) {
                latitude_factor = 0;
            }
            LatLng n = new LatLng(lastLatLng.latitude - latitude_factor, lastLatLng.longitude);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(n, 14));
        }
    }

    public Bitmap tintDrawable(int drawable, int color) {
        Resources resources = getResources();
        Bitmap icon = BitmapFactory.decodeResource(resources, drawable).copy(Bitmap.Config.ARGB_8888, true);

        Paint paint = new Paint();
        ColorFilter filter = new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN);
        paint.setColorFilter(filter);

        Canvas canvas = new Canvas(icon);
        canvas.drawBitmap(icon, 0, 0, paint);

        return icon;
    }

    private void backgroundZombieKilling() {
        // background zombie killing - way to fast right now but gotta start somewhere...
        Log.d(TAG, "BACKGROUND ENGAGEMENT");
        for (Engagement e : model.getEngagements()
                ) {
            Zombie z = model.getZombie(e.zombieuid);
            //e.active = 0;
            e.displayed = false;

            if (z != null) {
                z.health -= player_to_zombie_damage;
                if (z.health <= 0) {
                    z.health = 0;
                    e.active = 0;
                    model.last_zombie_attack = 0;
                    model.engaged = false;
                }
                updateZombieHealth(z.health);
                Log.d(TAG, "sending INTERACTION update");

                model.updateZombieHealth(e.zombieuid, z.health);
                Interaction interaction = new Interaction(e.zombieuid, z.health, model.getPlayer().health);
                Intent update = new Intent(SyncService.INTERACTION);
                update.putExtra("interaction", interaction);
                manager.sendBroadcast(update);
            }
        }
    }

    private void foregroundPlayerInteraction(PlayerEngagement e0, PlayerEngagement e1) {
        if(e0.active == 1 && e1.active == 1) {
            // check the responses
            // this means we have not decided yet
            if(e1.state == 1) {
                setUIState(UIState.PERSON_THEY_RAN_AWAY);
                Log.d(TAG, "the other player ran away!!! -.-");
                //Handler handler = new Handler();
                handler.postDelayed(map_screen, 1000);
                model.player_engaged = false;
            } else if (e0.state == 0) {
                setUIState(UIState.PERSON);
                // present ze buttons
            } else {

                // remove our buttons, because our choice is made
                // now check the other side...
                if (e0.state != 1 && e1.state == 0) {
                    // this means we have decided but they have not yet. display some sort of waiting graphic
                    setUIState(UIState.PERSON_WAITING);
                    // TODO, how is this properly handled?
                } else if (e0.state == 2 && e1.state == 2) {
                    // we both attack
                    // this means we need to display some sort of graphic until we get the result from the server
                    setUIState(UIState.PERSON_BOTH_ATTACK);
                } else if (e0.state == 3 && e1.state == 3) {
                    // we both befriend
                    // good for both of use, display some graphic
                    setUIState(UIState.PERSON_BOTH_BEFRIEND);
                } else if (e0.state == 2 && e1.state == 3) {
                    // we attack, they befriend -> they made a mistake, they lose something and we gain a little
                    setUIState(UIState.PERSON_ROBBING_SUCCESS);
                } else if (e0.state == 3 && e1.state == 2) {
                    // they attack, we befriend -> we made a mistake, we lose something and they gain a little
                    setUIState(UIState.PERSON_GOT_ROBBED);
                }
            }

        }
    }

    private void foregroundZombieKilling(Engagement engagement, Zombie[] zombies, long current_time) {
        model.engaged = true;
        zombie_engagement_running = true; //FIXME: this could potentially lead to a data race
        // find zombie that is involved in our engagement
        Zombie involved_zombie = null;
        for (Zombie z : zombies
                ) {
            if (z.uid == engagement.zombieuid) {
                involved_zombie = z;
            }
        }
        engagement.displayed = true;

        if (involved_zombie != null) {
            if(involved_zombie.health == 0) {
                zombie_engagement_running = false;
            }
            updateZombieHealth(involved_zombie.health);
        }
        long diff_zombie_attack = current_time - model.last_zombie_attack;

        if (diff_zombie_attack >= model.zombie_attack_time) {

            Log.d(TAG, "WOOWOWOWOWOWOWOWOW");

            switch (model.zombie_attack_state) {
                case 0:
                    // flash the yellow thingy
                    setUIState(UIState.ZOMBIE_READYING);
                    //readying.setVisibility(View.VISIBLE);
                    model.zombie_attack_state = 1;
                    break;
                case 1:
                    //
                    if (diff_zombie_attack >= model.zombie_attack_time + 2000) {
                        setUIState(UIState.ZOMBIE_HIT);
                        //readying.setVisibility(View.GONE);
                        // attack
                        model.getPlayer().health -= zombie_to_player_damage;
                        updateHealth(model.getPlayer().health);

                        Interaction interaction = new Interaction(involved_zombie.uid, involved_zombie.health, model.getPlayer().health);
                        Intent update = new Intent(SyncService.INTERACTION);
                        update.putExtra("interaction", interaction);
                        this.manager.sendBroadcast(update);

                        resetZombieAttack();
                    }
                    break;
            }
        } else {
            setUIState(UIState.ZOMBIE);
        }
/*      // TODO: remove all of this
        // set UI button states (cooldown)
        long diff_attack = current_time - model.last_player_attack;
        long diff_defend = current_time - model.last_player_defend;
        long diff_potion = current_time - model.last_player_potion;
        if (diff_attack >= 3 * 1000) {
            // enable attack button
            //enableElement(UIElement.ATTACK_BUTTON);
            //attack_button.setText("Attack Zombie");
        } else {
            //attack_button.setText("Attack Zombie (cooldown: " + diff_attack / 1000.0 + ")");
        }

        if (diff_defend >= 3 * 1000) {
            // enable defend button
            enableElement(UIElement.DEFEND_BUTTON);
            //defend_button.setText("Defend");
        } else {
            //defend_button.setText("Defend (cooldown: " + diff_defend / 1000.0 + ")");
        }

        if (diff_potion >= 3 * 1000) {
            // enable healing button
            enableElement(UIElement.HEALING_BUTTON);
            //potion_button.setText("Use Potion ("+model.potions.size()+")");
        } else {
            //potion_button.setText("Use Potion (cooldown: " + diff_potion / 1000.0 + ")");
        }
        */
    }

    private void resetZombieAttack() {
        long current_time = System.currentTimeMillis();

        Random r = new Random();
        int min = 2000;
        int max = 4000;
        int random_attack_time = r.nextInt((max - min) + 1) + min;
        model.zombie_attack_time = random_attack_time;
        model.last_zombie_attack = current_time;

        model.zombie_attack_state = 0;
    }

    private void updateMap(Zombie[] zombies, Player player, Player[] other, Item[] items) {
        if (!model.new_data) {
            return;
        } else {
            model.new_data = false;
        }
        // draw player boundingbox
        LatLng[] pbb = model.getPlayerBoundingbox();
        if (pbb != null) {
            if (player_boundingbox != null) {
                ArrayList<LatLng> latLngs = new ArrayList<>();
                latLngs.add(pbb[0]);
                latLngs.add(pbb[1]);
                latLngs.add(pbb[2]);
                latLngs.add(pbb[3]);
                latLngs.add(pbb[0]);

                player_boundingbox.setPoints(latLngs);
            } else {

                PolygonOptions rectOptions = new PolygonOptions()
                        .add(pbb[0], pbb[1], pbb[2], pbb[3], pbb[0]).fillColor(0x7700ff00).strokeWidth(0.1f);

                // Get back the mutable Polygon
                player_boundingbox = mMap.addPolygon(rectOptions);
            }
        }
        // FIXME: display the zombie trail?
        for (Marker m : zombie_markers
                ) {
            m.remove();
        }

        // TODO: do not display player icon or something instead - hidden for now
        LatLng home = new LatLng(player.lat, player.lon);

        // TODO: display other players
        if (zombies != null) {
            for (Zombie zombie : zombies
                    ) {
                LatLng z = new LatLng(zombie.lat, zombie.lon);

                MarkerOptions marker;
                if (zombie.health == 0) {
                    // TODO
                    marker = new MarkerOptions().position(z).title("Zombie #" + zombie.uid + " position").icon(BitmapDescriptorFactory.fromBitmap(tintDrawable(R.drawable.ic_directions_walk_black_24dp, 0xFFFF0000)));
                } else {
                    marker = new MarkerOptions().position(z).title("Zombie #" + zombie.uid + " position").icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_directions_walk_black_24dp));
                }

                zombie_markers.add(mMap.addMarker(marker));
            }
        }

        for (Marker m : other_player_markers
                ) {
            m.remove();
        }

        if (other != null) {
            for (Player other_player : other
                    ) {
                LatLng z = new LatLng(other_player.lat, other_player.lon);

                /*MarkerOptions marker;
                if (other_player.health == 0) {
                    marker = new MarkerOptions().position(z).title("Player #" + player.uid + " position").icon(BitmapDescriptorFactory.fromBitmap(tintDrawable(R.drawable.ic_person_black_24dp, 0xFFFF0000)));
                } else {
                    marker = new MarkerOptions().position(z).title("Player #" + player.uid + " position").icon(BitmapDescriptorFactory.fromBitmap(tintDrawable(R.drawable.ic_person_black_24dp, 0xFFFFFF00)));
                }*/

                //other_player_markers.add(mMap.addMarker(marker));
            }
        }

        for (Marker m : items_markers
                ) {
            m.remove();
        }

        if (items != null) {
            for (Item item : items
                    ) {
                LatLng z = new LatLng(item.lat, item.lon);

                MarkerOptions marker;
                if (item.owneruid == -1) { //TODO: update item pickup somewhere else... and check for our userid
                    marker = new MarkerOptions().position(z).title("Item #" + item.itemuid + " position").icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_card_giftcard_black_24dp));
                    items_markers.add(mMap.addMarker(marker));
                }

            }
        }

        updateMapCamera(home);
    }

    private void tutorialMode() {
        ImageView tut = findViewById(R.id.tutorial_portrait);

        if (settings.getTutorialMode()) {
            tut.setVisibility(View.VISIBLE);
            tut.setImageResource(R.drawable.tutorial_overview_inventory_portrait);

            currentTutorialState = TutorialState.INVENTORY_SCREEN;

            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        }

        tut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ImageView tut = findViewById(R.id.tutorial_portrait);

                switch (currentTutorialState) {
                    case NONE:
                        tut.setOnClickListener(null);
                        tut.setVisibility(View.GONE);
                        settings.disableTutorialMode();
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
                        break;
                    case INVENTORY_BUTTON:
                        tut.setImageResource(R.drawable.tutorial_overview_inventory_portrait);
                        currentTutorialState = TutorialState.INVENTORY_SCREEN;
                        break;
                    case INVENTORY_SCREEN:
                        updateInventory(model.getPlayerItems());

                        showElement(UIElement.INVENTORY);
                        tut.setImageResource(R.drawable.tutorial_overview_inventory_portrait);
                        currentTutorialState = TutorialState.INTERACTIVE_BUTTON;
                        break;
                    case INTERACTIVE_BUTTON:
                        hideElement(UIElement.INVENTORY);
                        tut.setImageResource(R.drawable.tutorial_overview_interactive_portrait);
                        currentTutorialState = TutorialState.ZOMBIE_SCREEN;
                        break;
                    case ZOMBIE_SCREEN:
                        setUIState(UIState.ZOMBIE);
                        tut.setImageResource(R.drawable.tutorial_overview_zombie_engagement_portrait);
                        currentTutorialState = TutorialState.PERSON_SCREEN;
                        break;
                    case PERSON_SCREEN:
                        idleMode();
                        setUIState(UIState.PERSON);
                        tut.setImageResource(R.drawable.tutorial_overview_player_engagement_portrait);
                        currentTutorialState = TutorialState.NONE;
                        break;
                }
                if(settings.getTutorialMode()) {

                }
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        // TODO ###### PRELOAD EVERYTHING POSSIBLE #######
        // preload image resources:
        this.health_bg = BitmapFactory.decodeResource(getResources(), R.drawable.health_bg);
        this.health_bar_human = BitmapFactory.decodeResource(getResources(), R.drawable.health_bar);
        this.health_bar_zombie = BitmapFactory.decodeResource(getResources(), R.drawable.health_bar_zombie);

        // ui elements
        this.attack_button = findViewById(R.id.attack_portrait);
        this.attack_button_landscape = findViewById(R.id.attack_landscape);
        this.defend_button = findViewById(R.id.defend_portrait);
        this.defend_button_landscape = findViewById(R.id.defend_landscape);
        this.healing_button = findViewById(R.id.heal_portrait);
        this.healing_button_landscape = findViewById(R.id.heal_landscape);
        this.attack_person = findViewById(R.id.attack_person_portrait);
        this.attack_person_landscape = findViewById(R.id.attack_person_landscape);
        this.befriend_person = findViewById(R.id.befriend_portrait);
        this.befriend_person_landscape = findViewById(R.id.befriend_landscape);
        this.run_away = findViewById(R.id.run_away_portrait);
        this.run_away_landscape = findViewById(R.id.run_away_landscape);


        this.health_bar_portrait = findViewById(R.id.health_portrait_image);
        this.health_bar_landscape = findViewById(R.id.health_landscape_image);
        this.zombie_health_bar_portrait = findViewById(R.id.zombie_health_portrait_image);
        this.zombie_health_bar_landscape = findViewById(R.id.zombie_health_landscape_image);

        this.health_txt_portrait = findViewById(R.id.health_portrait);
        this.health_txt_landscape = findViewById(R.id.health_landscape);
        this.food_text_portrait = findViewById(R.id.food_amount_portrait);
        this.drink_text_portrait = findViewById(R.id.drink_amount_portrait);
        this.food_text_landscape = findViewById(R.id.food_amount_landscape);
        this.drink_text_landscape = findViewById(R.id.drink_amount_landscape);

        this.engagement_portrait = findViewById(R.id.engagement_portrait);
        this.engagement_landscape = findViewById(R.id.engagement_landscape);
        this.map = findViewById(R.id.map);

        Resources res = getResources();
        this.zombie_vertical = res.getDrawable(R.drawable.zombie_vertical);
        this.zombie_vertical_readying = res.getDrawable(R.drawable.zombie_vertical_readying);
        this.zombie_vertical_hit = res.getDrawable(R.drawable.zombie_vertical_hit);
        this.zombie_vertical_hit_by_player = res.getDrawable(R.drawable.zombie_vertical_hit_by_player);
        this.zombie_vertical_defended = res.getDrawable(R.drawable.zombie_vertical_defended);
        this.zombie_vertical_defeated = res.getDrawable(R.drawable.zombie_vertical_defeated);
        this.zombie_vertical_killed = res.getDrawable(R.drawable.zombie_vertical_killed);
        this.person_vertical = res.getDrawable(R.drawable.person_vertical);
        this.person_vertical_you_ran_away = res.getDrawable(R.drawable.person_vertical_you_ran_away);
        this.person_vertical_they_ran_away = res.getDrawable(R.drawable.person_vertical_they_ran_away);
        this.person_vertical_waiting = res.getDrawable(R.drawable.person_vertical_waiting);
        this.person_vertical_both_attack = res.getDrawable(R.drawable.person_vertical_both_attack);
        this.person_vertical_both_befriend = res.getDrawable(R.drawable.person_vertical_both_befriend);
        this.person_vertical_won_fight = res.getDrawable(R.drawable.person_vertical_won_fight);
        this.person_vertical_lost_fight = res.getDrawable(R.drawable.person_vertical_lost_fight);
        this.person_vertical_got_robbed = res.getDrawable(R.drawable.person_vertical_got_robbed);
        this.person_vertical_successful_robbing = res.getDrawable(R.drawable.person_vertical_successful_robbing);
        this.beans_vertical = res.getDrawable(R.drawable.beans_vertical);
        this.beans_horizontal = res.getDrawable(R.drawable.beans_horizontal);
        this.whiskey_vertical = res.getDrawable(R.drawable.whiskey_vertical);
        this.whiskey_horizontal = res.getDrawable(R.drawable.whiskey_horizontal);



        // TODO: set idle mode after preload
        idleMode();


        progressAnimatorLeft = ObjectAnimator.ofInt(findViewById(R.id.progressBarLeftPortrait), "progress", 100,0);
        progressAnimatorLeft.setDuration(3000);

        progressAnimatorLeft .addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                enableElement(UIElement.ATTACK_BUTTON);
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });

        progressAnimatorCenter = ObjectAnimator.ofInt(findViewById(R.id.progressBarCenterPortrait), "progress", 100,0);
        progressAnimatorCenter.setDuration(3000);

        progressAnimatorCenter .addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                enableElement(UIElement.DEFEND_BUTTON);
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });

        progressAnimatorRight = ObjectAnimator.ofInt(findViewById(R.id.progressBarRightPortrait), "progress", 100,0);
        progressAnimatorRight.setDuration(3000);

        progressAnimatorRight .addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                enableElement(UIElement.HEALING_BUTTON);
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });

        final Switch interactive_portrait = findViewById(R.id.interactive_portrait);
        final Switch interactive_landscape = findViewById(R.id.interactive_landscape);

        switch_portrait_listener = new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    model.interactive_mode = true;
                } else {
                    model.interactive_mode = false;
                }
                interactive_landscape.setOnCheckedChangeListener(null);
                interactive_landscape.setChecked(isChecked);
                interactive_landscape.setOnCheckedChangeListener(switch_landscape_listener);
            }
        };

        switch_landscape_listener = new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    model.interactive_mode = true;
                } else {
                    model.interactive_mode = false;
                }
                interactive_portrait.setOnCheckedChangeListener(null);
                interactive_portrait.setChecked(isChecked);
                interactive_portrait.setOnCheckedChangeListener(switch_portrait_listener);
            }
        };

        interactive_portrait.setOnCheckedChangeListener(switch_portrait_listener);
        interactive_landscape.setOnCheckedChangeListener(switch_landscape_listener);


        startService(new Intent(this, LocationTracker.class));
        this.manager = LocalBroadcastManager.getInstance(getApplicationContext());
        this.manager.registerReceiver(model, new IntentFilter(LocationTracker.UPDATE_RECORD));
        this.manager.registerReceiver(model, new IntentFilter(SyncService.NEW_REMOTE_STATE));
        this.manager.registerReceiver(model, new IntentFilter(SyncService.INTERACTION));
        this.manager.registerReceiver(model, new IntentFilter(NotificationBuilder.NOTIFICATION_ATTACK));
        this.manager.registerReceiver(model, new IntentFilter(NotificationBuilder.NOTIFICATION_BEFRIEND));
        this.manager.registerReceiver(model, new IntentFilter(NotificationBuilder.NOTIFICATION_OPEN_APP));
        this.manager.registerReceiver(model, new IntentFilter(NotificationBuilder.NOTIFICATION_RUN));
        settings = new SettingsManager(getApplicationContext());
        this.uid = settings.getUserId();

        //this.settings.enableTutorialMode();
        tutorialMode();

        zombie_markers = new ArrayList<>();
        other_player_markers = new ArrayList<>();
        items_markers = new ArrayList<>();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        refreshDebugDisplay.run();
    }

    @Override
    public void onResume() {
        super.onResume();

        model.foreground = true;
        SyncService.appstate = 0;
    }

    @Override
    public void onPause() {
        super.onPause();

        model.foreground = false;
        SyncService.appstate = 1;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        handler.removeCallbacks(refreshDebugDisplay);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        try {
            // Customise the styling of the base map using a JSON object defined
            // in a raw resource file.
            boolean success = googleMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                            this, R.raw.mapstyle));
        } catch (Resources.NotFoundException e) {
        }


        // move camera to karlsruhe position
        LatLng home = new LatLng(49.0081641, 8.4225059);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(home, 14));
        mMap.getUiSettings().setAllGesturesEnabled(false);
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                return true;
            }
        });
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Checks the orientation of the screen
        //TextView steps = findViewById(R.id.stepcounter);

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            //Toast.makeText(this, "landscape", Toast.LENGTH_SHORT).show();
            //steps.setText("landscape");
            findViewById(R.id.portrait).setVisibility(View.GONE);
            findViewById(R.id.landscape).setVisibility(View.VISIBLE);
            model.portrait = false;
            updateMapCamera();
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            //Toast.makeText(this, "portrait", Toast.LENGTH_SHORT).show();
            //steps.setText("portrait");
            findViewById(R.id.portrait).setVisibility(View.VISIBLE);
            findViewById(R.id.landscape).setVisibility(View.GONE);
            model.portrait = true;
            updateMapCamera();
        }
    }
}
