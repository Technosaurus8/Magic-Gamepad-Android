package com.technosaurus.MagicGamepad.screens.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.technosaurus.MagicGamepad.util.FeedbackManager;
import com.technosaurus.MagicGamepad.util.FullscreenHelper;
import com.technosaurus.MagicGamepad.R;
import com.technosaurus.MagicGamepad.input.GamepadInputHelper;
import com.technosaurus.MagicGamepad.screens.RemoteHost;

/**
 * Fragment for the standard gamepad layout.
 * Replaces the setupGamepad() method (~220 lines) from the original remote.java.
 */
public class GamepadFragment extends Fragment {

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
        return inflater.inflate(R.layout.gamepad, container, false);
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


        // Only show dialog on first creation if player is not selected.
        // savedInstanceState == null is added because if user selects player in gamepad layout then
        // switch to custom layout then the dialog won't show
        if (savedInstanceState == null || host.getPlayer().isEmpty()) {
            showPlayerDialog();
        }

        // Wire all gamepad inputs using shared helper (no more duplication)
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
