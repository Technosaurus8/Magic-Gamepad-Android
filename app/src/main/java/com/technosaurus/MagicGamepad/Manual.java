package com.technosaurus.MagicGamepad;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.technosaurus.MagicGamepad.R.id;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import com.google.android.material.snackbar.Snackbar;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Manual extends AppCompatActivity {
    // Regular expression for validating IPv4 address and port
    private static final String IP_PORT_REGEX = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}" +
            "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?):" +
            "(6553[0-5]|655[0-2][0-9]|65[0-4][0-9][0-9]|" +
            "6[0-4][0-9][0-9][0-9]|[1-5][0-9]{4}|[1-9][0-9]{0,3})$";

    // Pattern object compiled from the regular expression
    private static final Pattern IP_PORT_PATTERN = Pattern.compile(IP_PORT_REGEX);
    private FrameLayout adContainerView;
    private AdView adView;

    public void onBackPressed() {
        super.onBackPressed();
        Intent intent2 = new Intent(this, MainActivity.class);
        intent2.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent2);
        finish();
    }
    String ip = "";
    TextView textView;
    EditText inputText;
    public void openRemote() {
        Intent intent = new Intent(this, remote.class);
        intent.putExtra("key", ip);
        startActivity(intent);
        finish();
    }
    public static boolean isValidIpPort(String ipPort) {
        if (ipPort == null) {
            return false;
        }
        Matcher matcher = IP_PORT_PATTERN.matcher(ipPort);
        return matcher.matches();
    }
    private void showSnackbarAtTop(String message) {
        Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG);
        View snackbarView = snackbar.getView();
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) snackbarView.getLayoutParams();
        params.gravity = android.view.Gravity.TOP;
        snackbarView.setLayoutParams(params);
        snackbar.show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual);
        textView = (TextView) findViewById(R.id.textView);
        inputText = (EditText) findViewById(R.id.inputText);

        Button btn1 = findViewById(R.id.button3);

        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ip = inputText.getText().toString();
                if(!ip.isEmpty()){
                    if(isValidIpPort(ip)) {
                        openRemote();
                    }
                    else {
                        showSnackbarAtTop("invalid code");
                    }
                }
            }
        });

        inputText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
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


        adContainerView = findViewById(R.id.ad_container);

        if (adContainerView != null) {
            adContainerView.post(this::loadBanner);
        }
    }
    private void loadBanner() {
        // Create an AdView and set the ad unit ID on it.
        adView = new AdView(this);
        adView.setAdUnitId(getString(R.string.ad_manual)); // Replace with your actual Ad Unit ID
        adContainerView.addView(adView);

        AdSize adSize = getAdSize();
        adView.setAdSize(adSize);

        // Create an ad request.
        AdRequest adRequest = new AdRequest.Builder().build();

        // Start loading the ad.
        adView.loadAd(adRequest);
    }

    private AdSize getAdSize() {
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
    @Override
    protected void onDestroy() {
        if (adView != null) {
            adView.destroy();
        }
        super.onDestroy();
    }

}
