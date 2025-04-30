package com.technosaurus.MagicGamepad;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AutoConnect extends AppCompatActivity {
    private Socket socket;
    private TextView textView;
    private FrameLayout adContainerView;
    private AdView adView;
    private static final String TAG = "AutoConnect";
    private int port = 8765;
    private String foundIP = "";
    private ExecutorService executorService;
    private boolean back;

//    public void onBackPressed() {
//        super.onBackPressed();
//        back = true;
//        if (adView != null) {
//            adView.destroy();
//        }
//        if(socket!=null){
//            try {
//                socket.close();
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//        }
//        if(executorService!=null) {
//            executorService.shutdown();
//        }
//        Intent intent2 = new Intent(this, MainActivity.class);
//        intent2.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
//        startActivity(intent2);
//        finish();
//    }
    private void loadBanner() {
        // Create an AdView and set the ad unit ID on it.
        adView = new AdView(this);
        adView.setAdUnitId(getString(R.string.ad_auto)); // Replace with your actual Ad Unit ID
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_autoconnect);
        back=false;
        textView=findViewById(R.id.textView);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            //fileDetailsList = extras.getParcelableArrayList("file_details");
            //Log.d(TAG, "intent received");
        }

        //ActivityManager.getInstance().addActivity(this);\
        //Log.d("", Objects.requireNonNull(getLocalIpAddress()));
        if(getLocalIpAddress()!=null) {
            scanNetwork();
        }

        // Initialize the Google Mobile Ads SDK.
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

    @Override
    protected void onDestroy() {
        if (adView != null) {
            adView.destroy();
        }
        if(socket!=null){
            try {
                socket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if(executorService!=null) {
            executorService.shutdown();
        }
        super.onDestroy();
    }

    public void openRemote() {
        Intent intent = new Intent(this, remote.class);
        intent.putExtra("key", foundIP+":8765");
        startActivity(intent);
        finish();
    }

    private String getLocalIpAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    // Check if it's not a loopback address and it's an IPv4 address
                    if (!address.isLoopbackAddress() && address.getHostAddress().indexOf(':') == -1) {
                        //Log.d("",address.getHostAddress());
                        return address.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    private String getBaseIpAddress(String ipAddress) {
        // Split the IP address into octets
        String[] octets;
        if(ipAddress!=null) {
            octets = ipAddress.split("\\.");
            // Remove the last octet
            StringBuilder baseIpAddress = new StringBuilder();
            for (int i = 0; i < octets.length - 1; i++) {
                baseIpAddress.append(octets[i]);
                if (i < octets.length - 2) {
                    baseIpAddress.append(".");
                }
            }
            return baseIpAddress.toString();
        }
        else {
            return null;
        }
    }

    private void scanNetwork() {
        executorService = Executors.newFixedThreadPool(600);

        String baseIpAddress = getBaseIpAddress(getLocalIpAddress());
        //Log.d(TAG, baseIpAddress);
        if(baseIpAddress!=null) {
            ArrayList<CompletableFuture<Void>> futures = new ArrayList<>();
            for (int i = 1; i <= 255; i++) {
                final int finalI = i;
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    String host = baseIpAddress + "." + finalI;
                    //Log.d(TAG, host);
                    try {
                        socket = new Socket();
                        socket.connect(new InetSocketAddress(host, port), 400);
                        socket.close();

                        foundIP = host;
                    } catch (IOException e) {
                        // Handle failure
                    }
                }, executorService);
                futures.add(future);
            }

            CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
            allOf.thenRun(() -> {
                executorService.shutdown();
                runOnUiThread(() -> {
                    if (!foundIP.isEmpty()) {
                        //Toast.makeText(AutoConnect.this, "Found IP: " + foundIP, Toast.LENGTH_LONG).show();
                        openRemote();
                    } else {
                        //Toast.makeText(AutoConnect.this, "Server not running", Toast.LENGTH_SHORT).show();
                        if (!back) {
                            scanNetwork();
                        }
                    }
                });
            });
        }
    }
}