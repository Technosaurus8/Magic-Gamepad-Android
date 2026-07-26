package com.technosaurus.MagicGamepad.screens.fragments;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.technosaurus.MagicGamepad.input.InputObserver;
import com.technosaurus.MagicGamepad.util.CustomLayoutProfile;
import com.technosaurus.MagicGamepad.util.CustomLayoutStore;
import com.technosaurus.MagicGamepad.util.FeedbackManager;
import com.technosaurus.MagicGamepad.util.FullscreenHelper;
import com.technosaurus.MagicGamepad.util.CustomLayoutPrefsHelper;
import com.technosaurus.MagicGamepad.R;
import com.technosaurus.MagicGamepad.input.GamepadInputHelper;
import com.technosaurus.MagicGamepad.screens.CustomLayout;
import com.technosaurus.MagicGamepad.screens.RemoteHost;
import com.technosaurus.MagicGamepad.views.JoystickView;
import com.technosaurus.MagicGamepad.views.SteeringWheelView;
import com.technosaurus.MagicGamepad.views.TriggerSliderView;

import java.util.Arrays;
import java.util.List;

/**
 * Fragment for the custom (user-configured) gamepad layout.
 * Replaces the setupCustomLayout() method (~375 lines) from the original remote.java.
 * Uses shared GamepadInputHelper for input wiring (eliminates duplication with GamepadFragment).
 */
public class CustomLayoutFragment extends Fragment {

    private static final String PREFERENCES_FILE = "com.technosaurus.MagicGamepad.preferences";
    private static final String KEY_LAYOUT_ID = "saved_layout_id";

    private InputObserver gamepad;
    private RemoteHost host;
    private FeedbackManager feedbackManager;
    private boolean isBtHid;
    private String layoutId;
    private CustomLayout customLayout;
    private View[] allViews;
    private Button addButton;
    private TextView textView;
    private boolean layoutApplied;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof RemoteHost) {
            host = (RemoteHost) context;
        } else {
            throw new RuntimeException(context + " must implement RemoteHost");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            isBtHid = args.getBoolean("isBtHid", false);
        }
        if (savedInstanceState != null) {
            layoutId = savedInstanceState.getString(KEY_LAYOUT_ID, null);
            Log.d("Layout Id Loaded: ", layoutId!=null?layoutId:"null");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Handle orientation for Android 15+ (API 35+)
        if (Build.VERSION.SDK_INT > 34) {
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
        //the below if block will execute when the user selects this layout from the drawer. and won't execute on rotation.
        if (savedInstanceState == null) {
            // Lock drawer on first creation (selecting the layout from drawer) ; activity restores lock state after rotation
            host.setDrawerLocked(true);
        }
        // Don't wire inputs if showing rotate message
        // even if 'a' is hidden it will be present in the custom_layout.xml file so it will not return null
        // but in rotate_message.xml 'a' is not present so it will return null
        if (view.findViewById(R.id.a) == null) {
            // no need to show it in portrait mode.
            dismissPresetDialogIfPresent();
            // do not dismissPlayerDialogIfPresent();
            // because it will if user selects player in gamepad/custom layout then switch back to custom layout
            // and change orientation to portrait then landscape the dialog won't show because savedInstance won't be null and skips the dialog.
            return;
        }

        feedbackManager = new FeedbackManager(requireContext(),
                requireContext().getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE));
        //Enter fullscreen
        FullscreenHelper.setFullscreen(requireActivity());

        customLayout = view.findViewById(R.id.custom_layout);

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
        SteeringWheelView steeringWheel = view.findViewById(R.id.steering_wheel);
        TriggerSliderView triggerSliderLeft = view.findViewById(R.id.l_slider);
        TriggerSliderView triggerSliderRight = view.findViewById(R.id.r_slider);
        addButton = view.findViewById(R.id.add);
        textView = view.findViewById(R.id.text);

        allViews = new View[]{Lt, Lb, Rb, Rt, RS, LS, rightJoystick, leftJoystick,
                a, b, x, y, dpadUp, dpadDown, dpadLeft, dpadRight, viewBtn, menuBtn,
                steeringWheel, triggerSliderLeft, triggerSliderRight};

        // Wire all gamepad inputs using shared helper (no more duplication)
        GamepadInputHelper.State state = new GamepadInputHelper.State();
        gamepad = GamepadInputHelper.wireAllInputs(view, state, feedbackManager, host, requireActivity());

        List<CustomLayoutProfile> profiles = CustomLayoutStore.getProfiles(requireContext());
        if (layoutId != null) {
            applyLayoutProfile();
            maybeShowPlayerDialog(savedInstanceState);
        } else if (profiles.size() == 1) {
            layoutId = profiles.get(0).getId();
            applyLayoutProfile();
            maybeShowPlayerDialog(savedInstanceState);
        } else {
            showEmptyLayout();
            maybeShowPlayerDialog(savedInstanceState);
            maybeShowPresetDialog(savedInstanceState);
        }
    }

    private void maybeShowPlayerDialog(@Nullable Bundle savedInstanceState) {
        // Only show dialog on first creation if player is not selected.
        // savedInstanceState == null is added because if user selects player in gamepad layout then
        // switch to custom layout then the dialog won't show
        if (!isBtHid && (savedInstanceState == null || host.getPlayer().isEmpty())) {
            host.setPlayer("");
            showPlayerDialog();
        } else {
            dismissPlayerDialogIfPresent();
        }
    }
    private void maybeShowPresetDialog(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState == null || layoutId == null || layoutId.isEmpty()) {
            Log.d("Layout Id: ", layoutId != null ? layoutId : "null");
            showPresetDialog(null);
        }
        else {
            dismissPresetDialogIfPresent();
        }
    }
    private void showEmptyLayout() {
        runWhenLayoutReady(() -> {
            customLayout.hideView(addButton);
            for (View v : allViews) {
                customLayout.hideView(v);
            }
            customLayout.hideView(textView);
            layoutApplied = true;
        });
    }

    private void runWhenLayoutReady(Runnable action) {
        if (customLayout == null || allViews == null) return;

        if (layoutApplied && customLayout.parentWidth > 0 && customLayout.parentHeight > 0) {
            action.run();
            return;
        }

        ViewTreeObserver observer = customLayout.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (customLayout.parentWidth == 0 || customLayout.parentHeight == 0) {
                    return;
                }
                customLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                action.run();
            }
        });
    }

    /**
     * Reload button visibility, positions, and sizes for the current layout preset.
     * Called on first show and when the user switches presets while playing.
     */
    public void applyLayoutProfile() {
        if (customLayout == null || allViews == null || layoutId == null) return;

        CustomLayoutProfile profile = CustomLayoutStore.loadProfile(requireContext(), layoutId);
        if (profile == null) return;

        boolean[] defaultHidden = new boolean[21];
        Arrays.fill(defaultHidden, true);
        boolean[] isHidden = profile.getHiddenStates();
        boolean allDefault = Arrays.equals(defaultHidden, isHidden);
        int[][] positions = profile.getPositions();
        int[][] sizes = profile.getSizes();

        Runnable applyVisibility = () -> {
            customLayout.hideView(addButton);
            for (View v : allViews) {
                customLayout.hideView(v);
            }
            customLayout.hideView(textView);
            for (int i = 0; i < allViews.length; i++) {
                if (!isHidden[i]) {
                    customLayout.showView(allViews[i]);
                }
            }
            if (allDefault) {
                customLayout.showView(textView);
                host.setDrawerLocked(false);
            } else {
                host.setDrawerLocked(true);
            }
        };

        if (layoutApplied) {
            for (int i = 0; i < allViews.length; i++) {
                CustomLayoutPrefsHelper.applyLayout(customLayout, allViews[i], positions, sizes, i);
            }
            applyVisibility.run();
            return;
        }

        ViewTreeObserver observer = customLayout.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (customLayout.parentWidth == 0 || customLayout.parentHeight == 0) {
                    return;
                }
                customLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                for (int i = 0; i < allViews.length; i++) {
                    CustomLayoutPrefsHelper.applyLayout(customLayout, allViews[i], positions, sizes, i);
                }
                applyVisibility.run();
                layoutApplied = true;
            }
        });
    }

    public void switchToProfile(String profileId) {
        if (profileId == null || profileId.equals(layoutId)) return;
        CustomLayoutProfile profile = CustomLayoutStore.loadProfile(requireContext(), profileId);
        if (profile == null) return;
        layoutId = profileId;
        applyLayoutProfile();
    }

    private void showPresetDialog(@Nullable Runnable onSelected) {
        if (getChildFragmentManager().findFragmentByTag(CustomLayoutPresetDialogFragment.TAG) != null) {
            return;
        }
        CustomLayoutPresetDialogFragment.Companion.newInstance(
                layoutId != null ? layoutId : "",
                profileId -> {
                    switchToProfile(profileId);
                    return null;
                },
                onSelected
        ).show(getChildFragmentManager(), CustomLayoutPresetDialogFragment.TAG);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (layoutId != null) {
            Log.d("Layout Id Saved: ", layoutId);
            outState.putString(KEY_LAYOUT_ID, layoutId);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        FullscreenHelper.setFullscreen(requireActivity());
    }

    // on stop is also used because the gamepad is getting null when
    // another layout is opened from the drawer. and the controls won't be reset
    @Override
    public void onStop() {
        if(gamepad!=null) {
            gamepad.resetAll();
        }
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        if (feedbackManager != null) {
            feedbackManager.release();
        }
        if(gamepad!=null) {
            gamepad.resetAll();
        }
        layoutApplied = false;
        customLayout = null;
        allViews = null;
        super.onDestroyView();
    }

    private void showPlayerDialog() {
        if (getChildFragmentManager().findFragmentByTag(PlayerSelectDialogFragment.TAG) != null) {
            return;
        }
        PlayerSelectDialogFragment.Companion.newInstance()
                .show(getChildFragmentManager(), PlayerSelectDialogFragment.TAG);
    }

    private void dismissPlayerDialogIfPresent() {
        Fragment existing = getChildFragmentManager()
                .findFragmentByTag(PlayerSelectDialogFragment.TAG);
        if (existing instanceof DialogFragment) {
            ((DialogFragment) existing).dismissAllowingStateLoss();
        }
    }
    private void dismissPresetDialogIfPresent() {
        Fragment existing = getChildFragmentManager()
                .findFragmentByTag(CustomLayoutPresetDialogFragment.TAG);
        if (existing instanceof DialogFragment) {
            ((DialogFragment) existing).dismissAllowingStateLoss();
        }
    }
}
