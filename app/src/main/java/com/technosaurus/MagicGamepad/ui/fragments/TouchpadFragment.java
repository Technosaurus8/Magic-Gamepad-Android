package com.technosaurus.MagicGamepad.ui.fragments;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.Guideline;
import androidx.fragment.app.Fragment;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.technosaurus.MagicGamepad.util.FullscreenHelper;
import com.technosaurus.MagicGamepad.R;
import com.technosaurus.MagicGamepad.ui.RemoteHost;

/**
 * Fragment for the touchpad layout.
 * Replaces the setupTouchpadLayout() method (~150 lines) from the original remote.java.
 * Fixes Thread.sleep(50) on UI thread by using Handler.postDelayed() instead.
 */
public class TouchpadFragment extends Fragment {

    private RemoteHost host;
    private AdView adView;
    private int previousAdHeightPx = 0;

    // Touch tracking
    private int startX, startY, X, Y, ScrollStartY, ScrollY;
    private long curTime;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof RemoteHost) {
            host = (RemoteHost) context;
        } else {
            throw new RuntimeException(context + " must implement RemoteHost");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.touchpad, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Exit fullscreen and unlock drawer for touchpad mode
        FullscreenHelper.exitFullscreen(requireActivity());
        host.setDrawerLocked(false);
        requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);

        // ── Wire touchpad controls ──────────────────────────────
        Button lmb = view.findViewById(R.id.lmb);
        Button rmb = view.findViewById(R.id.rmb);
        Button mmb = view.findViewById(R.id.mmb);
        View touchpad = view.findViewById(R.id.touchpad);
        View hscroll = view.findViewById(R.id.hscrollbar);

        mmb.setOnClickListener(v -> host.send("mmb"));

        hscroll.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    ScrollStartY = (int) event.getY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    ScrollY = (int) event.getY();
                    host.send("v" + "," + (ScrollY - ScrollStartY));
                    ScrollStartY = ScrollY;
                    break;
            }
            return false;
        });

        touchpad.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    startX = (int) event.getX();
                    startY = (int) event.getY();
                    curTime = System.currentTimeMillis();
                    break;

                case MotionEvent.ACTION_MOVE:
                    X = (int) event.getX();
                    Y = (int) event.getY();
                    host.send((X - startX) + "," + (Y - startY));
                    startX = X;
                    startY = Y;
                    break;

                case MotionEvent.ACTION_UP:
                    long duration = System.currentTimeMillis() - curTime;
                    if (duration < 200) {
                        // FIX: Use Handler.postDelayed() instead of Thread.sleep(50)
                        // The old code blocked the UI thread with Thread.sleep(50)
                        new Handler(Looper.getMainLooper()).postDelayed(
                                () -> host.send("lmb"), 50);
                    }
                    break;
            }
            return true;
        });

        lmb.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                host.send("mousedown");
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                host.send("mouseup");
            }
            return false;
        });

        rmb.setOnClickListener(v -> host.send("rmb"));

        // ── Banner ad ───────────────────────────────────────────
        adView = new AdView(requireContext());
        FrameLayout adContainer = view.findViewById(R.id.ad_tp);
        if (adContainer != null) {
            adContainer.post(() -> {
                if (isAdded()) loadBanner(view, adContainer);
            });
        }
    }

    @Override
    public void onPause() {
        if (adView != null) adView.pause();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adView != null) adView.resume();
    }

    @Override
    public void onDestroyView() {
        if (adView != null) {
            adView.destroy();
            adView = null;
        }
        super.onDestroyView();
    }

    // ── Private helpers ─────────────────────────────────────────────

    private void loadBanner(View rootView, FrameLayout container) {
        if (adView.getAdUnitId() == null || adView.getAdUnitId().isEmpty()) {
            adView.setAdUnitId(getString(R.string.ad_tp));
        }
        container.addView(adView);

        float density = getResources().getDisplayMetrics().density;
        float adWidthPixels = container.getWidth();
        if (adWidthPixels == 0) {
            adWidthPixels = getResources().getDisplayMetrics().widthPixels;
        }
        int adWidth = (int) (adWidthPixels / density);
        adView.setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
                requireActivity(), adWidth));

        Guideline bottom = rootView.findViewById(R.id.guideline33);
        Guideline top = rootView.findViewById(R.id.guideline27);

        adView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                container.getViewTreeObserver().addOnGlobalLayoutListener(
                        new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        int newAdHeightPx = container.getHeight();
                        int heightDifference = newAdHeightPx - previousAdHeightPx;

                        ConstraintLayout.LayoutParams bottomParams =
                                (ConstraintLayout.LayoutParams) bottom.getLayoutParams();
                        bottomParams.guideEnd += heightDifference;
                        bottom.setLayoutParams(bottomParams);

                        ConstraintLayout.LayoutParams topParams =
                                (ConstraintLayout.LayoutParams) top.getLayoutParams();
                        topParams.guideEnd += heightDifference;
                        top.setLayoutParams(topParams);

                        previousAdHeightPx = newAdHeightPx;

                        container.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                });
            }
        });

        adView.loadAd(new AdRequest.Builder().build());
    }
}
