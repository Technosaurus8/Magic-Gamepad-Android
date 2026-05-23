package com.technosaurus.MagicGamepad.screens.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.technosaurus.MagicGamepad.util.FeedbackManager;
import com.technosaurus.MagicGamepad.util.FullscreenHelper;
import com.technosaurus.MagicGamepad.util.CustomLayoutPrefsHelper;
import com.technosaurus.MagicGamepad.R;
import com.technosaurus.MagicGamepad.input.GamepadInputHelper;
import com.technosaurus.MagicGamepad.screens.CustomLayout;
import com.technosaurus.MagicGamepad.screens.RemoteHost;
import com.zerokol.views.joystickView.JoystickView;

import java.util.Arrays;

/**
 * Fragment for the custom (user-configured) gamepad layout.
 * Replaces the setupCustomLayout() method (~375 lines) from the original remote.java.
 * Uses shared GamepadInputHelper for input wiring (eliminates duplication with GamepadFragment).
 */
public class CustomLayoutFragment extends Fragment {

    private static final String PREFERENCES_FILE = "com.technosaurus.MagicGamepad.preferences";

    private RemoteHost host;
    private FeedbackManager feedbackManager;

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
        // Handle orientation for Android 15+ (API 35+)
        if (android.os.Build.VERSION.SDK_INT > 34) {
            int orientation = getResources().getConfiguration().orientation;
            if (orientation != Configuration.ORIENTATION_LANDSCAPE) {
                return inflater.inflate(R.layout.rotate_message, container, false);
            }
        } else {
            requireActivity().setRequestedOrientation(
                    ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE);
        }
        return inflater.inflate(R.layout.custom_layout, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Don't wire inputs if showing rotate message
        if (view.findViewById(R.id.a) == null) return;

        SharedPreferences prefs = requireContext()
                .getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE);
        feedbackManager = new FeedbackManager(requireContext(), prefs);

        // Lock drawer and enter fullscreen
        host.setDrawerLocked(true);
        FullscreenHelper.setFullscreen(requireActivity());

        // Show player selection dialog
        showPlayerDialog();

        // Get the CustomLayout instance
        CustomLayout customLayout = view.findViewById(R.id.custom_layout);

        // Find views
        Button Lt = view.findViewById(R.id.lt);
        Button Lb = view.findViewById(R.id.Lb);
        Button Rb = view.findViewById(R.id.Rb);
        Button Rt = view.findViewById(R.id.rt);
        Button RS = view.findViewById(R.id.RS);
        Button LS = view.findViewById(R.id.LS);
        ImageButton a = view.findViewById(R.id.a);
        ImageButton b = view.findViewById(R.id.b);
        ImageButton x = view.findViewById(R.id.x);
        ImageButton y = view.findViewById(R.id.y);
        ImageButton dpadUp = view.findViewById(R.id.dpad_up);
        ImageButton dpadDown = view.findViewById(R.id.dpad_down);
        ImageButton dpadLeft = view.findViewById(R.id.dpad_left);
        ImageButton dpadRight = view.findViewById(R.id.dpad_right);
        ImageButton menuBtn = view.findViewById(R.id.menu);
        ImageButton viewBtn = view.findViewById(R.id.view);
        JoystickView leftJoystick = view.findViewById(R.id.left_joystick);
        JoystickView rightJoystick = view.findViewById(R.id.right_joystick);
        Button addButton = view.findViewById(R.id.add);
        TextView textView = view.findViewById(R.id.text);

        // All views that can be hidden/shown
        View[] allViews = {Lt, Lb, Rb, Rt, RS, LS, rightJoystick, leftJoystick,
                a, b, x, y, dpadUp, dpadDown, dpadLeft, dpadRight, viewBtn, menuBtn};

        // ── Phase 1: Hide everything, then show based on saved state ──
        boolean[] defaultHidden = new boolean[18];
        Arrays.fill(defaultHidden, true);

        boolean[] isHidden = CustomLayoutPrefsHelper.loadBooleanArray(prefs);
        boolean allDefault = Arrays.equals(defaultHidden, isHidden);

        ViewTreeObserver observer = customLayout.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                customLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                // Hide add button and text (not used in play mode)
                customLayout.hideView(addButton);

                // Hide all gamepad views first
                for (View v : allViews) {
                    customLayout.hideView(v);
                }
                customLayout.hideView(textView);
            }
        });

        // Phase 2: Show saved views and apply positions/sizes
        ViewTreeObserver observer2 = customLayout.getViewTreeObserver();
        observer2.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                customLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                // Show views that aren't hidden
                for (int i = 0; i < allViews.length; i++) {
                    if (!isHidden[i]) {
                        customLayout.showView(allViews[i]);
                    }
                }

                // Show helper text if user hasn't customized anything
                if (allDefault) {
                    customLayout.showView(textView);
                }

                // Load and apply saved positions and sizes
                int[][] positions = CustomLayoutPrefsHelper.loadPositions(prefs);
                int[][] sizes = CustomLayoutPrefsHelper.loadSizes(prefs);

                for (int i = 0; i < allViews.length; i++) {
                    CustomLayoutPrefsHelper.applyPosition(customLayout, allViews[i], positions, i);
                    CustomLayoutPrefsHelper.applySize(customLayout, allViews[i], sizes, i);
                }
            }
        });

        // ── Wire all gamepad inputs using shared helper ───────────
        GamepadInputHelper.State state = new GamepadInputHelper.State();
        GamepadInputHelper.wireAllInputs(view, state, feedbackManager, host, requireActivity());
    }

    @Override
    public void onResume() {
        super.onResume();
        FullscreenHelper.setFullscreen(requireActivity());
    }

    @Override
    public void onDestroyView() {
        if (feedbackManager != null) {
            feedbackManager.release();
        }
        super.onDestroyView();
    }

    private void showPlayerDialog() {
        PlayerSelectDialogFragment dialog = PlayerSelectDialogFragment.Companion.newInstance(
                player -> {
                    host.setPlayer(player);
                    FullscreenHelper.setFullscreen(requireActivity());
                    return null; // Kotlin Unit
                }
        );
        dialog.show(getChildFragmentManager(), "player_select");
    }
}
