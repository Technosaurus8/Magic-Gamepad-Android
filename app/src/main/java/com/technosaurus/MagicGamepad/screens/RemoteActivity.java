package com.technosaurus.MagicGamepad.screens;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.ads.MobileAds;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.technosaurus.MagicGamepad.connection.BtSocket;
import com.technosaurus.MagicGamepad.connection.ConnectionViewModel;
import com.technosaurus.MagicGamepad.screens.fragments.CustomLayoutFragment;
import com.technosaurus.MagicGamepad.screens.fragments.DrawerAwareFragment;
import com.technosaurus.MagicGamepad.util.FullscreenHelper;
import com.technosaurus.MagicGamepad.util.RemoteLayoutPrefs;
import com.technosaurus.MagicGamepad.screens.fragments.GamepadFragment;
import com.technosaurus.MagicGamepad.screens.fragments.KeyboardFragment;
import com.technosaurus.MagicGamepad.R;
import com.technosaurus.MagicGamepad.screens.fragments.TouchpadFragment;

import android.view.MenuItem;

/**
 * Main controller Activity — now a thin shell that hosts layout Fragments.
 * Reduced from ~1380 lines to ~200 lines.
 *
 * All layout-specific logic (gamepad, keyboard, touchpad, custom) has been moved
 * to their respective Fragment classes. Shared utilities (fullscreen, feedback,
 * input wiring, preferences) are extracted into helper classes.
 */
public class RemoteActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, RemoteHost {

    private static final String KEY_CURRENT_LAYOUT = "current_layout";
    private static final String KEY_DRAWER_LOCKED = "drawer_locked";
    private static final String SELECTED_PLAYER = "selected_player";
    private int currentLayout;
    private String player = "";
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    /** Authoritative lock state; DrawerLayout lock mode can desync on config change. */
    private boolean drawerLocked;// using separate variable for tracking drawer state because
    //drawerLayout.getDrawerLockMode(GravityCompat.END) is not reliable and giving unpredictable output.
    private ConnectionViewModel viewModel;
    private WifiManager.WifiLock wifiLock;
    private PowerManager.WakeLock wakeLock;
    private boolean isBt = false;
    // ── Lifecycle ────────────────────────────────────────────────────
    private void notifyDrawerState(boolean open) {
        Fragment current = getSupportFragmentManager()
                .findFragmentById(R.id.content_frame);
        if (current instanceof DrawerAwareFragment) {
            ((DrawerAwareFragment) current).onDrawerStateChanged(open);
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this,SystemBarStyle.dark(Color.TRANSPARENT),SystemBarStyle.dark(Color.TRANSPARENT));
        setContentView(R.layout.activity_remote);

        drawerLayout = findViewById(R.id.drawer_layout);
        if (savedInstanceState != null) {
            drawerLocked = savedInstanceState.getBoolean(KEY_DRAWER_LOCKED, false);
        }

        drawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {
                notifyDrawerState(slideOffset > 0);
            }

            @Override
            public void onDrawerOpened(@NonNull View drawerView) {
                notifyDrawerState(true);
            }

            @Override
            public void onDrawerClosed(@NonNull View drawerView) {
                notifyDrawerState(false);
            }

            @Override
            public void onDrawerStateChanged(int newState) {}
        });

        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        applyDrawerLockMode();

        // Determine connection type
        Intent intent = getIntent();
        String ip = intent.getStringExtra("selected_device_ip");
        if (ip == null) {
            isBt = true;
        } else {
            WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            if (Build.VERSION.SDK_INT >= 29) {
                wifiLock = wifiManager.createWifiLock(
                        WifiManager.WIFI_MODE_FULL_LOW_LATENCY, "MyApp::WifiLock");
            } else {
                wifiLock = wifiManager.createWifiLock(
                        WifiManager.WIFI_MODE_FULL_HIGH_PERF, "MyApp::WifiLock");
            }
        }

        // WakeLock with timeout to prevent battery drain on leak
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyApp::WakeLock");
        acquireLocks();

        //restore player on screen rotate.
        if (savedInstanceState != null) {
            player = savedInstanceState.getString(SELECTED_PLAYER, "");
        }

        viewModel = new ViewModelProvider(this).get(ConnectionViewModel.class);

        if ((isBt && !BtSocket.isConnected()) || (!isBt && viewModel.getClient() == null)) {
            viewModel.connect(isBt, ip, intent, new ConnectionViewModel.ConnectCallback() {
                @Override
                public void onConnected() {
                    if (isFinishing() || isDestroyed()) return;// for preventing crash when back button is pressed while connecting.
                    runOnUiThread(() -> {
                        removeProgressBar();
                        currentLayout = resolveInitialLayout(savedInstanceState);
                        // Prevents fragment from being added again on rotation
                        /*
                        if (savedInstanceState == null || host.getPlayer().isEmpty()) {
                            showPlayerDialog();
                        }
                        */
                        // without this guard the above if block in gamepad fragment and custom layout fragment
                        // will fail to work because when calling setLayout the fragment level saved instance will be null on rotation
                        // fragments will be automatically restored by the fragment manager.
                        if (savedInstanceState == null) {
                            setLayout(currentLayout);
                        }
                    });
                }
            });
        } else {
            removeProgressBar();
            currentLayout = resolveInitialLayout(savedInstanceState);
            if (savedInstanceState == null) {
                setLayout(currentLayout);
            }
        }

        viewModel.getDisconnectedLiveData().observe(this, disconnected -> {
            if (!isFinishing() && !isDestroyed() && disconnected) {
                showDisconnectMsg();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentLayout == RemoteLayoutPrefs.LAYOUT_GAMEPAD
                || currentLayout == RemoteLayoutPrefs.LAYOUT_CUSTOM) {
            FullscreenHelper.setFullscreen(this);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && (currentLayout == RemoteLayoutPrefs.LAYOUT_GAMEPAD
                || currentLayout == RemoteLayoutPrefs.LAYOUT_CUSTOM)) {
            FullscreenHelper.setFullscreen(this);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_CURRENT_LAYOUT, currentLayout);
        outState.putString(SELECTED_PLAYER, player);
        outState.putBoolean(KEY_DRAWER_LOCKED, drawerLocked);
    }

    @Override
    protected void onDestroy() {
        releaseLocks();
        super.onDestroy();
    }

    // ── RemoteHost interface ────────────────────────────────────────

    @Override
    public void send(String msg) {
        try {
            viewModel.send(msg);
        } catch (Exception e) {
            Log.w("Remote", "Send failed", e);
        }
    }

    @Override
    public void setDrawerLocked(boolean locked) {
        drawerLocked = locked;
        applyDrawerLockMode();
    }

    @Override
    public boolean isDrawerLocked() {
        return drawerLocked;
    }

    private void applyDrawerLockMode() {
        if (drawerLayout == null || navigationView == null) {
            return;
        }
        int mode = drawerLocked ? DrawerLayout.LOCK_MODE_LOCKED_CLOSED
                : DrawerLayout.LOCK_MODE_UNLOCKED;
        drawerLayout.setDrawerLockMode(mode, navigationView);
    }

    @Override
    public void closeDrawer() {
        drawerLayout.closeDrawer(GravityCompat.END);
    }

    @Override
    public boolean isDrawerOpen() {
        return drawerLayout != null
                && drawerLayout.isDrawerOpen(GravityCompat.END);
    }

    @Override
    public String getPlayer() {
        return player;
    }

    @Override
    public void setPlayer(String player) {
        this.player = player;
    }

    // ── Navigation ──────────────────────────────────────────────────

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.navigation_gamepad) {
            closeDrawer();
            setLayout(RemoteLayoutPrefs.LAYOUT_GAMEPAD);
        } else if (id == R.id.navigation_keyboard) {
            setLayout(RemoteLayoutPrefs.LAYOUT_KEYBOARD);
        } else if (id == R.id.navigation_touchpad) {
            setLayout(RemoteLayoutPrefs.LAYOUT_TOUCHPAD);
        } else if (id == R.id.navigation_custom) {
            closeDrawer();
            setLayout(RemoteLayoutPrefs.LAYOUT_CUSTOM);
        }
        return false;
    }

    // ── Volume keys ─────────────────────────────────────────────────

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
            send("up");
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
            send("down");
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    // ── Private helpers ─────────────────────────────────────────────

    private int resolveInitialLayout(Bundle savedInstanceState) {
        int defaultLayout = RemoteLayoutPrefs.getDefaultLayout(
                getSharedPreferences(RemoteLayoutPrefs.PREFERENCES_FILE, MODE_PRIVATE));
        if (savedInstanceState != null) {
            return savedInstanceState.getInt(KEY_CURRENT_LAYOUT, defaultLayout);
        }
        return defaultLayout;
    }

    /**
     * Switch to a layout Fragment with a fade animation.
     */
    private void setLayout(int layout) {
        currentLayout = layout;
        Fragment fragment;
        switch (layout) {
            case RemoteLayoutPrefs.LAYOUT_GAMEPAD:
                fragment = new GamepadFragment();
                break;
            case RemoteLayoutPrefs.LAYOUT_KEYBOARD:
                fragment = new KeyboardFragment();
                break;
            case RemoteLayoutPrefs.LAYOUT_TOUCHPAD:
                fragment = new TouchpadFragment();
                break;
            case RemoteLayoutPrefs.LAYOUT_CUSTOM:
                fragment = new CustomLayoutFragment();
                break;
            default:
                return;
        }
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.content_frame, fragment)
                .commit();
    }

    /**
     * Fade out and remove the connection progress bar.
     */
    private void removeProgressBar() {
        View progressBar = findViewById(R.id.progressBar);
        if (progressBar != null) {
            progressBar.animate().alpha(0f).setDuration(300).withEndAction(() -> {
                ViewGroup parent = (ViewGroup) progressBar.getParent();
                if (parent != null) parent.removeView(progressBar);
            }).start();
        }
    }

    private void showDisconnectMsg() {
        View parentLayout = findViewById(android.R.id.content);
        Snackbar snackbar = Snackbar.make(parentLayout,
                "Disconnected. Go back and reconnect", Snackbar.LENGTH_LONG);
        View snackView = snackbar.getView();
        FrameLayout.LayoutParams params =
                (FrameLayout.LayoutParams) snackView.getLayoutParams();
        params.gravity = Gravity.TOP;
        snackView.setLayoutParams(params);
        snackbar.show();
    }

    private void acquireLocks() {
        if (!isBt && wifiLock != null && !wifiLock.isHeld()) {
            wifiLock.acquire();
        }
        if (wakeLock != null && !wakeLock.isHeld()) {
            wakeLock.acquire(10 * 60 * 1000L); // 10 min timeout (prevents battery drain)
        }
    }

    private void releaseLocks() {
        if (!isBt && wifiLock != null && wifiLock.isHeld()) {
            wifiLock.release();
        }
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }
}
