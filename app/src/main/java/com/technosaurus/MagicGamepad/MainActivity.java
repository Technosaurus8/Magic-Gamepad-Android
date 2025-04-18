package com.technosaurus.MagicGamepad;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.DisplayMetrics;
import android.view.Display;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

public class MainActivity extends AppCompatActivity {
    private FrameLayout adContainerView;
    private AdView adView;

    public void OpenActivity(Class<?> activityClass) {
        Intent intent = new Intent(this, activityClass);
        startActivity(intent);
    }

    public void Help() {
        // The URL you want to open
        String url = "https://technosaurus8.github.io/MagicGamepad/";
        // Create an intent to view the URL
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize the Google Mobile Ads SDK.
        MobileAds.initialize(this, initializationStatus -> {});


        adContainerView = findViewById(R.id.ad_container);

        if (adContainerView != null) {
            adContainerView.post(this::loadBanner);
        }

        ImageButton btn3 = findViewById(R.id.imageButton);
        ImageButton btn4 = findViewById(R.id.settings);
        Button btn1 = findViewById(R.id.button_auto_wifi);
        Button btn2 = findViewById(R.id.button_manual_wifi);
        Button btn5 =findViewById(R.id.button_bluetooth_select);

        btn3.setOnClickListener(v -> Help());
        btn1.setOnClickListener(v -> OpenActivity(AutoConnect.class));
        btn2.setOnClickListener(v -> OpenActivity(Manual.class));
        btn4.setOnClickListener(v -> OpenActivity(settings.class));
        btn5.setOnClickListener(v -> OpenActivity(BtSelect.class));
    }

    private void loadBanner() {
        // Create an AdView and set the ad unit ID on it.
        adView = new AdView(this);
        adView.setAdUnitId(getString(R.string.ad_main));
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
