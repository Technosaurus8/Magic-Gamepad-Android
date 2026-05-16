package com.technosaurus.MagicGamepad.screens.fragments;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.technosaurus.MagicGamepad.util.FullscreenHelper;
import com.technosaurus.MagicGamepad.R;
import com.technosaurus.MagicGamepad.screens.RemoteHost;

/**
 * Fragment for the keyboard layout.
 * Replaces the setupKeyboard() method (~65 lines) from the original remote.java.
 */
public class KeyboardFragment extends Fragment {

    private RemoteHost host;
    private AdView adView;

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
        return inflater.inflate(R.layout.keyboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Exit fullscreen and unlock drawer for keyboard mode
        FullscreenHelper.exitFullscreen(requireActivity());
        host.setDrawerLocked(false);
        requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);

        //Add System Paddings
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
            );
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        // ── Wire keyboard controls ──────────────────────────────
        ImageButton sendKey = view.findViewById(R.id.sendkey);
        Button backspace = view.findViewById(R.id.backspace);
        EditText keystroke = view.findViewById(R.id.Keystroke);
        Button win = view.findViewById(R.id.win);
        Button alt = view.findViewById(R.id.alt);
        Button tab = view.findViewById(R.id.tab);
        Button enter = view.findViewById(R.id.Enter);
        Button ctrl = view.findViewById(R.id.ctrl);
        Button delete = view.findViewById(R.id.delete);
        Button playpause = view.findViewById(R.id.play);
        Button left = view.findViewById(R.id.Left);
        Button right = view.findViewById(R.id.Right);

        // Hold-to-send buttons (send down on press, up on release)
        setupHoldButton(right, "right_arrow_down", "right_arrow_up");
        setupHoldButton(left, "left_arrow_down", "left_arrow_up");
        setupHoldButton(delete, "delete_down", "delete_up");
        setupHoldButton(ctrl, "ctrl_down", "ctrl_up");
        setupHoldButton(win, "win_down", "win_up");
        setupHoldButton(alt, "alt_down", "alt_up");
        setupHoldButton(tab, "tab_down", "tab_up");
        setupHoldButton(enter, "enter_down", "enter_up");
        setupHoldButton(backspace, "backspace_down", "backspace_up");

        playpause.setOnClickListener(v -> host.send("play"));

        sendKey.setOnClickListener(v -> {
            host.send("k3y" + keystroke.getText().toString());
            keystroke.setText("");
        });

        keystroke.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                InputMethodManager imm = (InputMethodManager)
                        requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                View focus = requireActivity().getCurrentFocus();
                if (imm != null && focus != null) {
                    imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
                }
                return true;
            }
            return false;
        });

        // ── Banner ad ───────────────────────────────────────────
        adView = new AdView(requireContext());
        FrameLayout adContainer = view.findViewById(R.id.ad_kb);
        if (adContainer != null) {
            adContainer.post(() -> {
                if (isAdded()) loadBanner(adContainer);
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

    private void setupHoldButton(View button, String downMsg, String upMsg) {
        button.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    host.send(downMsg);
                    return true;
                case android.view.MotionEvent.ACTION_UP:
                    host.send(upMsg);
                    return true;
            }
            return false;
        });
    }

    private void loadBanner(FrameLayout container) {
        if (adView.getAdUnitId() == null || adView.getAdUnitId().isEmpty()) {
            adView.setAdUnitId(getString(R.string.ad_kb));
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

        adView.loadAd(new AdRequest.Builder().build());
    }
}
