package com.technosaurus.MagicGamepad;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.Vibrator;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.zerokol.views.joystickView.JoystickView;
import com.zerokol.views.joystickView.JoystickView.OnJoystickMoveListener;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.Guideline;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;

import android.view.MenuItem;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Activity;
import android.widget.TextView;

public class remote extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private int currentLayout;
    private int previousAdHeightPx = 0;
    private CustomLayout CustomLayout;
    private AudioTrack audioTrack;
    private String TouchFeedback;
    private boolean menu = false;
    private static final String KEY_CURRENT_LAYOUT = "current_layout";
    private static final String PREFERENCES_FILE = "com.technosaurus.MagicGamepad.preferences";
    private static final String TOUCH_FEEDBACK_KEY = "touch_feedback_key";
    private static final int LAYOUT_GAMEPAD = 2;
    private static final int LAYOUT_TOUCHPAD = 3;
    private static final int LAYOUT_KEYBOARD =4;
    private static final int LAYOUT_CUSTOM = 1;
    private FrameLayout contentFrame;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private String player;
    private Snackbar snackbar;
    private boolean LsOn = false;
    private boolean RsOn = false;
    private int startX, startY, X, Y, ScrollStartY, ScrollY;
    int[] Lstick, Rstick;
    int[] buttonState = new int[17];
    private long curTime;
    private FrameLayout fr_kb,fr_tp;
    private AdView adView_kb,adView_tp;
    private Vibrator vibrator;
    private AlertDialog dialog;
    private WifiManager.WifiLock wifiLock;
    private PowerManager.WakeLock wakeLock;
    private int[][] Positions = new int[18][2];
    private int[][] Sizes = new int[18][2];
    boolean isBt = false;
    private SharedPreferences preferences;
    private ConnectionViewModel viewModel;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remote);
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        contentFrame = findViewById(R.id.content_frame);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        preferences = getSharedPreferences(PREFERENCES_FILE, MODE_PRIVATE);
        TouchFeedback = preferences.getString(TOUCH_FEEDBACK_KEY, "Sound");
        //drawerLayout.openDrawer(GravityCompat.END);
        Intent intent = getIntent();
        player="p1";
        String ip = intent.getStringExtra("key");
        if(ip==null){
            isBt=true;
        }
        else {
            // Initialize WifiLock
            WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            if (Build.VERSION.SDK_INT >= 29) {
                wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_LOW_LATENCY, "MyApp::WifiLock");
            } else {
                wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "MyApp::WifiLock");
            }
        }
        // Initialize WakeLock
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyApp::WakeLock");
        acquireLocks();
        viewModel = new ViewModelProvider(this).get(ConnectionViewModel.class);
        if ((isBt && !BtSocket.isConnected()) || (!isBt && viewModel.getClient() == null)) {
            viewModel.connect(isBt, ip, intent, new ConnectionViewModel.ConnectCallback() {
                @Override
                public void onConnected() {
                    runOnUiThread(()->{
                        ((ViewGroup) findViewById(R.id.progressBar).getParent()).removeView(findViewById(R.id.progressBar));
                    });
                }
            });
        }
        else{
            //it is already connected
            ((ViewGroup) findViewById(R.id.progressBar).getParent()).removeView(findViewById(R.id.progressBar));
        }
        viewModel.getDisconnectedLiveData().observe(this, disconnected -> {
            if (disconnected) {
                showDisconnectMsg();
            }
        });
        if (savedInstanceState != null) {
            currentLayout = savedInstanceState.getInt(KEY_CURRENT_LAYOUT, LAYOUT_GAMEPAD);
        } else {
            currentLayout = LAYOUT_GAMEPAD; // default layout
        }
        setLayout(currentLayout);
    }

    private void acquireLocks() {
            if (!isBt && !wifiLock.isHeld()) {
                wifiLock.acquire();
            }
            if (!wakeLock.isHeld()) {
                wakeLock.acquire();
            }

    }

    private void releaseLocks() {
            if (!isBt) {
                if (wifiLock!=null&&wifiLock.isHeld()) {
                    wifiLock.release();
                }
            }
            if (wakeLock!=null&&wakeLock.isHeld()) {
                wakeLock.release();
            }

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
            send("up");
            // Handle volume up key press
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
            send("down");
            // Handle volume down key press
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    private void showDisconnectMsg(){
        View parentLayout = findViewById(android.R.id.content); // Find the root view
        Snackbar snackbar = Snackbar.make(parentLayout, "Disconnected. Go back and reconnect", Snackbar.LENGTH_LONG);
        View view = snackbar.getView();
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) view.getLayoutParams();
        params.gravity = Gravity.TOP;
        view.setLayoutParams(params);
        snackbar.show();
    }
    private void send(String msg){
        viewModel.send(msg);
    }

    private void setLayout(int layout) {
        switch (layout) {
            case LAYOUT_GAMEPAD:
                setupGamepad();
                break;
            case LAYOUT_KEYBOARD:
                setupKeyboard();
                break;
            case LAYOUT_TOUCHPAD:
                setupTouchpadLayout();
                break;
            case LAYOUT_CUSTOM:
                setupCustomLayout();
                break;
        }
    }

    private void loadBanner(AdView adView, FrameLayout adContainerView, String adUnitId) {
        // Set the ad unit ID on the AdView.
        if(adView.getAdUnitId()==null||adView.getAdUnitId().isEmpty()) {//if something goes wrong try removing this if
            adView.setAdUnitId(adUnitId);
        }// Replace with your actual Ad Unit ID
        adContainerView.addView(adView);

        AdSize adSize = getAdSize(adContainerView);
        adView.setAdSize(adSize);

        // Create an ad request.
        AdRequest adRequest = new AdRequest.Builder().build();

        // Start loading the ad.
        adView.loadAd(adRequest);
    }

    private AdSize getAdSize(FrameLayout adContainerView) {
        // Determine the screen width (less decorations) to use for the ad width.
        Display display = getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        float density = outMetrics.density;
        float adWidthPixels = adContainerView.getWidth();

        if (adWidthPixels == 0) {
            adWidthPixels = outMetrics.widthPixels;
        }

        int adWidth = (int) (adWidthPixels / density);

        // Return the optimal AdSize for the given width.
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth);
    }
    private void destroy_ad(AdView adView){
        if (adView != null) {
            adView.destroy();
        }
    }
    private void setupOntouch(View button, String down,String up){
        button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        send(down);
                        return true;
                    case MotionEvent.ACTION_UP:
                        send(up);
                        return true;
                }
                return false;
            }
        });

    }
    private void simulateVibration(int durationMs, int sampleRate, int vibrationFrequencyHz) {
        int numSamples = (int) ((durationMs / 1000.0) * sampleRate);
        double[] sample = new double[numSamples];
        byte[] generatedSound = new byte[2 * numSamples];

        // Generate sine wave at the desired frequency
        for (int i = 0; i < numSamples; ++i) {
            sample[i] = Math.sin(2 * Math.PI * i / (sampleRate / (double) vibrationFrequencyHz));
        }

        // Convert to 16-bit PCM sound array
        int idx = 0;
        for (final double dVal : sample) {
            short val = (short) ((dVal * 32767));
            generatedSound[idx++] = (byte) (val & 0x00ff);
            generatedSound[idx++] = (byte) ((val & 0xff00) >>> 8);
        }

        // Play the generated sound
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                sampleRate, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT, generatedSound.length,
                AudioTrack.MODE_STATIC);
        audioTrack.write(generatedSound, 0, generatedSound.length);
        audioTrack.play();
    }
    private void VibrateOrSound(){
        if (TouchFeedback.equals("Vibration")) {
            if (vibrator != null) {
                vibrator.vibrate(35);
            }
        } else {
            if (audioTrack != null) {
                audioTrack.release();
            }
            simulateVibration(35, 44100, 300);
        }
    }

    private void setupKeyboard() {
        currentLayout = LAYOUT_KEYBOARD;
        exitFullscreen(this);
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        destroy_ad(adView_tp);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
        contentFrame.removeAllViews();
        View.inflate(this, R.layout.keyboard, contentFrame);
        //setContentView(R.layout.keyboard);
        ImageButton sendKey = findViewById(R.id.sendkey);
        Button backspace = findViewById(R.id.backspace);
        EditText keystroke = findViewById(R.id.Keystroke);
        Button win = findViewById(R.id.win);
        Button alt = findViewById(R.id.alt);
        Button tab = findViewById(R.id.tab);
        Button enter = findViewById(R.id.Enter);
        Button ctrl = findViewById(R.id.ctrl);
        Button delete = findViewById(R.id.delete);
        Button playpause = findViewById(R.id.play);
        Button Left = findViewById(R.id.Left);
        Button Right = findViewById(R.id.Right);
        setupOntouch(Right,"right_arrow_down","right_arrow_up");
        setupOntouch(Left, "left_arrow_down", "left_arrow_up");
        setupOntouch(delete,"delete_down","delete_up");
        setupOntouch(ctrl, "ctrl_down", "ctrl_up");
        setupOntouch(win, "win_down", "win_up");
        setupOntouch(alt, "alt_down", "alt_up");
        setupOntouch(tab, "tab_down", "tab_up");
        setupOntouch(enter, "enter_down","enter_up");
        setupOntouch(backspace,"backspace_down","backspace_up");
        playpause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                send("play");
            }
        });
        sendKey.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                send("k3y" + keystroke.getText().toString());
                keystroke.setText("");
            }
        });
        keystroke.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_NEXT) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                    return true;
                }
                return false;
            }
        });
        new Thread(
                () -> {
                    // Initialize the Google Mobile Ads SDK on a background thread.
                    MobileAds.initialize(this, initializationStatus -> {});
                })
                .start();
        adView_kb = new AdView(this);
        fr_kb = findViewById(R.id.ad_kb);
        if (fr_kb != null) {
            fr_kb.post(() -> loadBanner(adView_kb, fr_kb, getString(R.string.ad_kb)));
        }
    }

    private void setupGamepadButton(InputObserver Gamepad, View button, int buttonIndex) {
        button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        buttonState[buttonIndex] = 1;
                        Gamepad.setButtonState(buttonState);
                        VibrateOrSound();
                        return true;

                    case MotionEvent.ACTION_UP:
                        buttonState[buttonIndex] = 0;
                        Gamepad.setButtonState(buttonState);
                        return true;
                }
                return false;
            }
        });
    }
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if(hasFocus) {
            if (currentLayout == LAYOUT_GAMEPAD || currentLayout == LAYOUT_CUSTOM) {// this triggers initially but its not that important bug
                setFullscreen(this);
            }
        }
    }
    // Fullscreen method
    public void setFullscreen(Activity activity) {
        View decorView = activity.getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    // Exit fullscreen method
    public void exitFullscreen(Activity activity) {
        View decorView = activity.getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
    }
    private int[] convertToXboxAnalogRange(double angle, double power) {
        // Convert angle from degrees to radians
        double angleRadians = Math.toRadians(angle);

        // Calculate x and y coordinates scaled to fit Xbox analog stick range (-32768 to 32767)
        double xDouble = power * Math.cos(angleRadians);
        double yDouble = power * Math.sin(angleRadians);

        // Scale x and y to fit within the range -32768 to 32767
        double x = xDouble * 32767.0 / 100.0;
        double y = yDouble * 32767.0 / 100.0;

        // Clamp values to ensure they are within valid range and round to nearest integer
        x = Math.max(-32768, Math.min(32767, x));
        y = Math.max(-32768, Math.min(32767, y));

        // Convert to int and return
        return new int[]{(int) Math.round(x), (int) Math.round(y)};
    }

    private void showDialog() {
        // Create a builder for the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select a player");

        // Get the layout inflater
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_playerselect, null);
        builder.setView(dialogView);

        // Setup buttons in the dialog
        Button player1Button = dialogView.findViewById(R.id.player1_button);
        Button player2Button = dialogView.findViewById(R.id.player2_button);
        Button player3Button = dialogView.findViewById(R.id.player3_button);
        Button player4Button = dialogView.findViewById(R.id.player4_button);

        // Set click listeners for each button
        player1Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle option 1 selection
                player = "p1";

                dismissDialog();
            }
        });

        player2Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle option 2 selection
                player = "p2";
                dismissDialog();
            }
        });

        player3Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle option 3 selection
                player = "p3";
                dismissDialog();
            }
        });

        player4Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle option 4 selection
                player = "p4";
                dismissDialog();
            }
        });

        // Create and show the dialog
        dialog = builder.create();
        dialog.setCancelable(false);
        dialog.show();
    }

    private void dismissDialog() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
        setFullscreen(this);
    }

    private void setupGamepad(){
        currentLayout = LAYOUT_GAMEPAD;
        destroy_ad(adView_tp);
        destroy_ad(adView_kb);
        if (android.os.Build.VERSION.SDK_INT > 34) {
            // Device is running Android 15 or higher
            int orientation = getResources().getConfiguration().orientation;
            if (orientation != Configuration.ORIENTATION_LANDSCAPE) {
                contentFrame.removeAllViews();
                View.inflate(this, R.layout.rotate_message, contentFrame);
                return;
            }
        }
        else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE);
        }
        contentFrame.removeAllViews();
        View.inflate(this, R.layout.gamepad, contentFrame);
        menu=false;
        setFullscreen(this);
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        showDialog();
        //EdgeToEdge.enable(this);
        //setContentView(R.layout.gamepad);
        View parentLayout = findViewById(android.R.id.content);
        InputObserver Gamepad = new InputObserver();
        ImageButton a = findViewById(R.id.a);
        ImageButton b = findViewById(R.id.b);
        ImageButton x = findViewById(R.id.x);
        ImageButton y = findViewById(R.id.y);
        ImageButton dpadUp = findViewById(R.id.dpad_up);
        ImageButton dpadLeft = findViewById(R.id.dpad_left);
        ImageButton dpadRight = findViewById(R.id.dpad_right);
        ImageButton dpadDown = findViewById(R.id.dpad_down);
        Button Lt = findViewById(R.id.lt);
        Button Rt = findViewById(R.id.rt);
        Button Rb = findViewById(R.id.Rb);
        Button Lb = findViewById(R.id.Lb);
        Button LS = findViewById(R.id.LS);
        Button RS = findViewById(R.id.RS);
        ImageButton Menu = findViewById(R.id.menu);
        ImageButton view = findViewById(R.id.view);

        //SwitchCompat Player = findViewById(R.id.playerselector);
        JoystickView left_joystick = findViewById(R.id.left_joystick);
        JoystickView right_joystick = findViewById(R.id.right_joystick);
        //setupGamepadButton(Gamepad,Menu,14);
        setupGamepadButton(Gamepad,view,15);
        setupGamepadButton(Gamepad,a,0);
        setupGamepadButton(Gamepad,b,2);
        setupGamepadButton(Gamepad,x,1);
        setupGamepadButton(Gamepad,y,3);
        setupGamepadButton(Gamepad,Lb,6);
        setupGamepadButton(Gamepad,Rb,7);
        setupGamepadButton(Gamepad,dpadUp,10);
        setupGamepadButton(Gamepad,dpadDown,11);
        setupGamepadButton(Gamepad,dpadLeft,12);
        setupGamepadButton(Gamepad,dpadRight,13);

        Menu.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        buttonState[14] = 1;
                        Gamepad.setButtonState(buttonState);
                        VibrateOrSound();
                        return false;

                    case MotionEvent.ACTION_UP:
                        buttonState[14] = 0;
                        Gamepad.setButtonState(buttonState);
                        return false;
                }
                return false;
            }
        });
        Menu.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                // Unlock the DrawerLayout on long click
                if (menu) {
                    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
                    menu = false;
                    snackbar = Snackbar.make(parentLayout, "Menu Locked", Snackbar.LENGTH_SHORT);
                    snackbar.show();
                    Menu.clearColorFilter();
                } else {
                    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
                    menu = true;
                    snackbar = Snackbar.make(parentLayout, "Menu Unlocked", Snackbar.LENGTH_LONG);
                    snackbar.show();
                    Menu.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.menu_tint));
                }

                //Log.d("Menu Long Click", "Menu button was long clicked."); // Add a log message here
                // Return true to indicate that the long click event is consumed
                return true;
            }
        });
        LS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (LsOn) {
                    buttonState[8]=0;
                    Gamepad.setButtonState(buttonState);
                    LsOn = false;
                    LS.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.default_button_color));
                } else {
                    buttonState[8]=1;
                    Gamepad.setButtonState(buttonState);
                    LsOn = true;
                    LS.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.clicked_button_color));
                }
                VibrateOrSound();
            }
        });
        RS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (RsOn) {
                    buttonState[9]=0;
                    Gamepad.setButtonState(buttonState);
                    RsOn = false;
                    RS.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.default_button_color));
                } else {
                    buttonState[9]=1;
                    Gamepad.setButtonState(buttonState);
                    RsOn = true;
                    RS.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.clicked_button_color));
                }
                VibrateOrSound();
            }
        });

        Lt.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        buttonState[4] = 255;
                        Gamepad.setButtonState(buttonState);
                        VibrateOrSound();
                        return true;

                    case MotionEvent.ACTION_UP:
                        buttonState[4]=0;
                        Gamepad.setButtonState(buttonState);
                        return true;
                }
                return false;
            }
        });
        Rt.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        buttonState[5] = 255;
                        Gamepad.setButtonState(buttonState);
                        VibrateOrSound();
                        return true;

                    case MotionEvent.ACTION_UP:
                        buttonState[5] = 0;
                        Gamepad.setButtonState(buttonState);
                        return true;
                }
                return false;
            }
        });


        right_joystick.setOnJoystickMoveListener(new OnJoystickMoveListener() {
            @Override
            public void onValueChanged(int angle, int power, int direction) {
                //Log.d("","angle:"+angle+" power:"+power);
                Rstick=convertToXboxAnalogRange(angle,power);
                Gamepad.setRstick(Rstick);
            }

        });
        right_joystick.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction()==MotionEvent.ACTION_DOWN) {
                    VibrateOrSound();
                }
                return false;
            }
        });


        left_joystick.setOnJoystickMoveListener(new OnJoystickMoveListener() {
            @Override
            public void onValueChanged(int angle, int power, int direction) {
                //Log.d("","angle:"+angle+" power:"+power);
                Lstick=convertToXboxAnalogRange(angle,power);
                Gamepad.setLstick(Lstick);
            }

        });
        left_joystick.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction()==MotionEvent.ACTION_DOWN) {
                    VibrateOrSound();
                }
                return false;
            }
        });

        Gamepad.setOnInputChangedListener(new InputObserver.OnInputChangedListener() {
            @Override
            public void onInputChanged(int[] Lstick, int[] Rstick, int[] buttons) {
                //Log.d("Lstick: "+Lstick[0]+", "+Lstick[1]+" | Rstick: "+Rstick[0]+", "+Rstick[1]+" | Buttons: "+ Arrays.toString(buttons));
                send(player+"Lstick: "+Lstick[0]+", "+Lstick[1]+" | Rstick: "+Rstick[0]+", "+Rstick[1]+" | Buttons: "+ Arrays.toString(buttons));
            }
        });
    }
    public boolean[] loadBooleanArray(String key) {
        boolean[] def = {true,true,true,true,true,true,true,true,true,true,true,true,true,true,true,true,true,true};
        String savedString = preferences.getString(key, null);
        if (savedString!=null) {
            boolean[] array = new boolean[18];
            String[] parts = savedString.split(",");
            for (int i = 0; i < parts.length; i++) {
                array[i] = parts[i].equals("1");
            }
            return array;
        }
        else {
            return def;
        }
    }

    private void Load_position(String key, View view, int buttonNumber) {
        String positionsString = preferences.getString(key, "");
        if (!positionsString.isEmpty()) {
            String[] positionsArray = positionsString.split(";");
            for (int i = 0; i < positionsArray.length; i++) {
                String[] coords = positionsArray[i].split(",");
                Positions[i][0] = Integer.parseInt(coords[0]);
                Positions[i][1] = Integer.parseInt(coords[1]);
            }
        }
        CustomLayout.moveViewTo(view, Positions[buttonNumber][0], Positions[buttonNumber][1]);
    }
    private void Load_size(String key, View view, int buttonNumber) {
        String sizeString = preferences.getString(key, "");
        if (!sizeString.isEmpty()) {
            String[] sizesArray = sizeString.split(";");
            for (int i = 0; i < sizesArray.length; i++) {
                String[] dimensions = sizesArray[i].split(",");
                Sizes[i][0] = Integer.parseInt(dimensions[0]);
                Sizes[i][1] = Integer.parseInt(dimensions[1]);
            }
        }
        if(Sizes[buttonNumber][0]!=0) {
            CustomLayout.setViewSize(view, Sizes[buttonNumber][0], Sizes[buttonNumber][1]);
        }
    }
    private void setupCustomLayout(){
        currentLayout = LAYOUT_CUSTOM;
        destroy_ad(adView_tp);
        destroy_ad(adView_kb);
        if (android.os.Build.VERSION.SDK_INT > 34) {
            // Device is running Android 15 or higher
            int orientation = getResources().getConfiguration().orientation;
            if (orientation != Configuration.ORIENTATION_LANDSCAPE) {
                contentFrame.removeAllViews();
                View.inflate(this, R.layout.rotate_message, contentFrame);
                return;
            }
        }
        else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE);
        }
        contentFrame.removeAllViews();
        View.inflate(this, R.layout.custom_layout, contentFrame);
        CustomLayout = findViewById(R.id.custom_layout);
        menu=false;
        setFullscreen(this);
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        showDialog();
        //EdgeToEdge.enable(this);
        //setContentView(R.layout.gamepad);
        View parentLayout = findViewById(android.R.id.content);
        InputObserver Gamepad = new InputObserver();
        boolean[] isHidden = {true,true,true,true,true,true,true,true,true,true,true,true,true,true,true,true,true,true};
        ImageButton a = findViewById(R.id.a);
        ImageButton b = findViewById(R.id.b);
        ImageButton x = findViewById(R.id.x);
        ImageButton y = findViewById(R.id.y);
        ImageButton dpadUp = findViewById(R.id.dpad_up);
        ImageButton dpadLeft = findViewById(R.id.dpad_left);
        ImageButton dpadRight = findViewById(R.id.dpad_right);
        ImageButton dpadDown = findViewById(R.id.dpad_down);
        Button Lt = findViewById(R.id.lt);
        Button Rt = findViewById(R.id.rt);
        Button Rb = findViewById(R.id.Rb);
        Button Lb = findViewById(R.id.Lb);
        Button LS = findViewById(R.id.LS);
        Button RS = findViewById(R.id.RS);
        ImageButton Menu = findViewById(R.id.menu);
        ImageButton view = findViewById(R.id.view);
        JoystickView left_joystick = findViewById(R.id.left_joystick);
        JoystickView right_joystick = findViewById(R.id.right_joystick);
        Button add_button = findViewById(R.id.add);
        TextView textView = findViewById(R.id.text);

        //load hidden state
        ViewTreeObserver observer = CustomLayout.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                // Remove the listener to avoid multiple calls
                CustomLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                CustomLayout.hideView(add_button);
                CustomLayout.hideView(a);
                CustomLayout.hideView(b);
                CustomLayout.hideView(x);
                CustomLayout.hideView(y);
                CustomLayout.hideView(dpadUp);
                CustomLayout.hideView(dpadLeft);
                CustomLayout.hideView(dpadRight);
                CustomLayout.hideView(dpadDown);
                CustomLayout.hideView(Lt);
                CustomLayout.hideView(Rt);
                CustomLayout.hideView(Rb);
                CustomLayout.hideView(Lb);
                CustomLayout.hideView(LS);
                CustomLayout.hideView(RS);
                CustomLayout.hideView(right_joystick);
                CustomLayout.hideView(left_joystick);
                CustomLayout.hideView(Menu);
                CustomLayout.hideView(view);
                CustomLayout.hideView(textView);
                // You may need to call hideView again here if it relies on layout height
            }
        });
        boolean[] temp = isHidden;
        isHidden = loadBooleanArray("isHidden");
        ViewTreeObserver observer2 = CustomLayout.getViewTreeObserver();
        boolean[] finalIsHidden = isHidden;
        observer2.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                CustomLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                if (!finalIsHidden[0]) {
                    CustomLayout.showView(Lt);
                }
                if (!finalIsHidden[1]) {
                    CustomLayout.showView(Lb);
                }
                if (!finalIsHidden[2]) {
                    CustomLayout.showView(Rb);
                }
                if (!finalIsHidden[3]) {
                    CustomLayout.showView(Rt);
                }
                if (!finalIsHidden[4]) {
                    CustomLayout.showView(RS);
                }
                if (!finalIsHidden[5]) {
                    CustomLayout.showView(LS);
                }
                if (!finalIsHidden[6]) {
                    CustomLayout.showView(right_joystick);
                }
                if (!finalIsHidden[7]) {
                    CustomLayout.showView(left_joystick);
                }
                if (!finalIsHidden[8]) {
                    CustomLayout.showView(a);
                }
                if (!finalIsHidden[9]) {
                    CustomLayout.showView(b);
                }
                if (!finalIsHidden[10]) {
                    CustomLayout.showView(x);
                }
                if (!finalIsHidden[11]) {
                    CustomLayout.showView(y);
                }
                if (!finalIsHidden[12]) {
                    CustomLayout.showView(dpadUp);
                }
                if (!finalIsHidden[13]) {
                    CustomLayout.showView(dpadDown);
                }
                if (!finalIsHidden[14]) {
                    CustomLayout.showView(dpadLeft);
                }
                if (!finalIsHidden[15]) {
                    CustomLayout.showView(dpadRight);
                }
                if (!finalIsHidden[16]) {
                    CustomLayout.showView(view);
                }
                if (!finalIsHidden[17]) {
                    CustomLayout.showView(Menu);
                }
                if(Arrays.equals(temp, finalIsHidden)){
                    CustomLayout.showView(textView);
                }
                // Load positions and sizes
                Load_position("positions", Lt, 0);
                Load_size("sizes", Lt, 0);

                Load_position("positions", Lb, 1);
                Load_size("sizes", Lb, 1);

                Load_position("positions", Rb, 2);
                Load_size("sizes", Rb, 2);

                Load_position("positions", Rt, 3);
                Load_size("sizes", Rt, 3);

                Load_position("positions", RS, 4);
                Load_size("sizes", RS, 4);

                Load_position("positions", LS, 5);
                Load_size("sizes", LS, 5);

                Load_position("positions", right_joystick, 6);
                Load_size("sizes", right_joystick, 6);

                Load_position("positions", left_joystick, 7);
                Load_size("sizes", left_joystick, 7);

                Load_position("positions", a, 8);
                Load_size("sizes", a, 8);

                Load_position("positions", b, 9);
                Load_size("sizes", b, 9);

                Load_position("positions", x, 10);
                Load_size("sizes", x, 10);

                Load_position("positions", y, 11);
                Load_size("sizes", y, 11);

                Load_position("positions", dpadUp, 12);
                Load_size("sizes", dpadUp, 12);

                Load_position("positions", dpadDown, 13);
                Load_size("sizes", dpadDown, 13);

                Load_position("positions", dpadLeft, 14);
                Load_size("sizes", dpadLeft, 14);

                Load_position("positions", dpadRight, 15);
                Load_size("sizes", dpadRight, 15);

                Load_position("positions", view, 16);
                Load_size("sizes", view, 16);

                Load_position("positions", Menu, 17);
                Load_size("sizes", Menu, 17);
            }
        });


        //setupGamepadButton(Gamepad,Menu,14);
        setupGamepadButton(Gamepad,view,15);
        setupGamepadButton(Gamepad,a,0);
        setupGamepadButton(Gamepad,b,2);
        setupGamepadButton(Gamepad,x,1);
        setupGamepadButton(Gamepad,y,3);
        setupGamepadButton(Gamepad,Lb,6);
        setupGamepadButton(Gamepad,Rb,7);
        setupGamepadButton(Gamepad,dpadUp,10);
        setupGamepadButton(Gamepad,dpadDown,11);
        setupGamepadButton(Gamepad,dpadLeft,12);
        setupGamepadButton(Gamepad,dpadRight,13);

        Menu.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        buttonState[14] = 1;
                        Gamepad.setButtonState(buttonState);
                        VibrateOrSound();
                        return false;

                    case MotionEvent.ACTION_UP:
                        buttonState[14] = 0;
                        Gamepad.setButtonState(buttonState);
                        return false;
                }
                return false;
            }
        });
        Menu.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                // Unlock the DrawerLayout on long click
                if (menu) {
                    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
                    menu = false;
                    snackbar = Snackbar.make(parentLayout, "Menu Locked", Snackbar.LENGTH_SHORT);
                    snackbar.show();
                    Menu.clearColorFilter();
                } else {
                    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
                    menu = true;
                    snackbar = Snackbar.make(parentLayout, "Menu Unlocked", Snackbar.LENGTH_LONG);
                    snackbar.show();
                    Menu.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.menu_tint));
                }

                //Log.d("Menu Long Click", "Menu button was long clicked."); // Add a log message here
                // Return true to indicate that the long click event is consumed
                return true;
            }
        });
        LS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (LsOn) {
                    buttonState[8]=0;
                    Gamepad.setButtonState(buttonState);
                    LsOn = false;
                    LS.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.default_button_color));
                } else {
                    buttonState[8]=1;
                    Gamepad.setButtonState(buttonState);
                    LsOn = true;
                    LS.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.clicked_button_color));
                }
                VibrateOrSound();
            }
        });
        RS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (RsOn) {
                    buttonState[9]=0;
                    Gamepad.setButtonState(buttonState);
                    RsOn = false;
                    RS.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.default_button_color));
                } else {
                    buttonState[9]=1;
                    Gamepad.setButtonState(buttonState);
                    RsOn = true;
                    RS.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.clicked_button_color));
                }
                VibrateOrSound();
            }
        });

        Lt.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        buttonState[4] = 255;
                        Gamepad.setButtonState(buttonState);
                        VibrateOrSound();
                        return true;

                    case MotionEvent.ACTION_UP:
                        buttonState[4]=0;
                        Gamepad.setButtonState(buttonState);
                        return true;
                }
                return false;
            }
        });

        Rt.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        buttonState[5] = 255;
                        Gamepad.setButtonState(buttonState);
                        VibrateOrSound();
                        return true;

                    case MotionEvent.ACTION_UP:
                        buttonState[5] = 0;
                        Gamepad.setButtonState(buttonState);
                        return true;
                }
                return false;
            }
        });


        right_joystick.setOnJoystickMoveListener(new OnJoystickMoveListener() {
            @Override
            public void onValueChanged(int angle, int power, int direction) {
                //Log.d("","angle:"+angle+" power:"+power);
                Rstick=convertToXboxAnalogRange(angle,power);
                Gamepad.setRstick(Rstick);
            }

        });
        right_joystick.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction()==MotionEvent.ACTION_DOWN) {
                    VibrateOrSound();
                }
                return false;
            }
        });


        left_joystick.setOnJoystickMoveListener(new OnJoystickMoveListener() {
            @Override
            public void onValueChanged(int angle, int power, int direction) {
                //Log.d("","angle:"+angle+" power:"+power);
                Lstick=convertToXboxAnalogRange(angle,power);
                Gamepad.setLstick(Lstick);
            }

        });
        left_joystick.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction()==MotionEvent.ACTION_DOWN) {
                    VibrateOrSound();
                }
                return false;
            }
        });

        Gamepad.setOnInputChangedListener(new InputObserver.OnInputChangedListener() {
            @Override
            public void onInputChanged(int[] Lstick, int[] Rstick, int[] buttons) {
                //Log.d("Lstick: "+Lstick[0]+", "+Lstick[1]+" | Rstick: "+Rstick[0]+", "+Rstick[1]+" | Buttons: "+ Arrays.toString(buttons));
                send(player+"Lstick: "+Lstick[0]+", "+Lstick[1]+" | Rstick: "+Rstick[0]+", "+Rstick[1]+" | Buttons: "+ Arrays.toString(buttons));
            }
        });
    }

    private void setupTouchpadLayout() {
        currentLayout = LAYOUT_TOUCHPAD;
        exitFullscreen(this);
        previousAdHeightPx=0;
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        destroy_ad(adView_kb);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
        contentFrame.removeAllViews();
        View.inflate(this, R.layout.touchpad, contentFrame);
        //setContentView(R.layout.touchpad);
        Button btn5 = findViewById(R.id.lmb);
        Button btn4 = findViewById(R.id.rmb);
        Button btn3 = findViewById(R.id.mmb);
        View touchpad = findViewById(R.id.touchpad);
        View hscroll = findViewById(R.id.hscrollbar);
        btn3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                send("mmb");
            }
        });
        hscroll.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        ScrollStartY = (int) event.getY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        ScrollY = (int) event.getY();
                        send(("v" + "," + Integer.toString(ScrollY - ScrollStartY)));
                        //Log.d("", ("v" + "," + Integer.toString(ScrollY - ScrollStartY)));
                        ScrollStartY = ScrollY;
                        break;
                }
                return false;
            }
        });



        touchpad.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = (int) event.getX();
                        startY = (int) event.getY();
                        curTime = System.currentTimeMillis();
                        break;

                    case MotionEvent.ACTION_MOVE:
                        X = (int) event.getX();
                        Y = (int) event.getY();
                        send((Integer.toString(X - startX) + "," + Integer.toString(Y - startY)));
                        //Log.d("", (Integer.toString(X - startX) + "," + Integer.toString(Y - startY)));
                        startX = X;
                        startY = Y;
                        break;

                    case MotionEvent.ACTION_UP:
                        long duration = System.currentTimeMillis() - curTime;
                        if (duration < 200) {
                            //Log.d("Click", Long.toString(duration));
                            try {
                                Thread.sleep(50);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                            send("lmb");
                        }
                        break;
                }
                return true;
            }
        });

        btn5.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    // Action when button is pressed
                    send("mousedown");
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    // Action when button is released
                    send("mouseup");
                }
                return false;
            }
        });

        btn4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                send("rmb");
            }
        });

        new Thread(
                () -> {
                    // Initialize the Google Mobile Ads SDK on a background thread.
                    MobileAds.initialize(this, initializationStatus -> {});
                })
                .start();
        adView_tp = new AdView(this);
        fr_tp = findViewById(R.id.ad_tp);
        if (fr_tp != null) {
            fr_tp.post(() -> loadBanner(adView_tp, fr_tp,getString(R.string.ad_tp)));
        }
        Guideline bottom = findViewById(R.id.guideline33);
        Guideline top = findViewById(R.id.guideline27);
        adView_tp.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                fr_tp.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        // Get the height of the ad view in pixels
                        int newAdHeightPx = fr_tp.getHeight();

                        // Calculate the difference between the old and new ad heights
                        int heightDifference = newAdHeightPx - previousAdHeightPx;
                        //Log.d("heightdiff", String.valueOf(heightDifference));

                        // Adjust bottom guideline
                        ConstraintLayout.LayoutParams bottomGuidelineParams = (ConstraintLayout.LayoutParams) bottom.getLayoutParams();
                        //Log.d("bottom_before", String.valueOf(bottomGuidelineParams.guideEnd));
                        bottomGuidelineParams.guideEnd += heightDifference;
                        bottom.setLayoutParams(bottomGuidelineParams);
                        //Log.d("bottom_after", String.valueOf(bottomGuidelineParams.guideEnd));

                        // Adjust top guideline
                        ConstraintLayout.LayoutParams topGuidelineParams = (ConstraintLayout.LayoutParams) top.getLayoutParams();
                        //Log.d("top_before", String.valueOf(topGuidelineParams.guideEnd));
                        topGuidelineParams.guideEnd += heightDifference;
                        top.setLayoutParams(topGuidelineParams);
                        //Log.d("top_after", String.valueOf(topGuidelineParams.guideEnd));

                        // Update previous ad height
                        previousAdHeightPx = newAdHeightPx;

                        // Ensure this listener is removed to avoid multiple calls
                        fr_tp.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                });
            }
        });
    }

    private void setupOnClick(Button button, final String msg)
    {
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                send(msg);
            }
        });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_CURRENT_LAYOUT, currentLayout);
    }

    @Override
    protected void onDestroy() {
        destroy_ad(adView_kb);
        destroy_ad(adView_tp);
        releaseLocks();
        if(dialog!=null && dialog.isShowing()){
            dialog.dismiss();
        }
        super.onDestroy();
    }
    @Override
    protected void onResume() {
        super.onResume();
        if(currentLayout==LAYOUT_GAMEPAD||currentLayout==LAYOUT_CUSTOM) {
            setFullscreen(this);
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if(id==R.id.navigation_gamepad){
            drawerLayout.closeDrawer(GravityCompat.END);
            setupGamepad();
        } else if (id==R.id.navigation_keyboard) {
            setupKeyboard();
        } else if (id==R.id.navigation_touchpad) {
            setupTouchpadLayout();
        } else if(id==R.id.navigation_custom){
            drawerLayout.closeDrawer(GravityCompat.END);
            setupCustomLayout();
        }
        return false;
    }
}
