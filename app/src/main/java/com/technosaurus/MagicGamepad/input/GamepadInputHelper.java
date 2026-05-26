package com.technosaurus.MagicGamepad.input;

import android.app.Activity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;
import com.technosaurus.MagicGamepad.util.FeedbackManager;
import com.technosaurus.MagicGamepad.R;
import com.technosaurus.MagicGamepad.screens.RemoteHost;
import com.zerokol.views.joystickView.JoystickView;

import java.util.Arrays;

/**
 * Shared gamepad input wiring logic used by both GamepadFragment and CustomLayoutFragment.
 * Eliminates ~380 lines of duplicated code from setupGamepad() and setupCustomLayout().
 */
public final class GamepadInputHelper {

    private GamepadInputHelper() {}

    /**
     * Mutable state shared across all gamepad input listeners within a single layout session.
     */
    public static class State {
        public final int[] buttonState = new int[ButtonIndex.BTN_COUNT];
        public int[] Lstick = new int[2];
        public int[] Rstick = new int[2];
        public boolean LsOn = false;
        public boolean RsOn = false;
    }

    /**
     * Converts joystick angle/power to Xbox analog stick range (-32768 to 32767).
     */
    public static int[] convertToXboxAnalogRange(double angle, double power) {
        double angleRadians = Math.toRadians(angle);
        double x = power * Math.cos(angleRadians) * 32767.0 / 100.0;
        double y = power * Math.sin(angleRadians) * 32767.0 / 100.0;
        x = Math.max(-32768, Math.min(32767, x));
        y = Math.max(-32768, Math.min(32767, y));
        return new int[]{(int) Math.round(x), (int) Math.round(y)};
    }

    /**
     * Wire all standard gamepad inputs on a root view.
     * Works with both gamepad.xml and custom_layout.xml since they share the same view IDs.
     *
     * @return the InputObserver that was wired up
     */
    public static InputObserver wireAllInputs(
            View root,
            State state,
            FeedbackManager feedback,
            RemoteHost host,
            Activity activity) {

        InputObserver gamepad = new InputObserver();
        // ── Find views ─────────────────────────────────────────────
        ImageButton a = root.findViewById(R.id.a);
        ImageButton b = root.findViewById(R.id.b);
        ImageButton x = root.findViewById(R.id.x);
        ImageButton y = root.findViewById(R.id.y);
        ImageButton dpadUp = root.findViewById(R.id.dpad_up);
        ImageButton dpadDown = root.findViewById(R.id.dpad_down);
        ImageButton dpadLeft = root.findViewById(R.id.dpad_left);
        ImageButton dpadRight = root.findViewById(R.id.dpad_right);
        Button Lt = root.findViewById(R.id.lt);
        Button Rt = root.findViewById(R.id.rt);
        Button Rb = root.findViewById(R.id.Rb);
        Button Lb = root.findViewById(R.id.Lb);
        Button LS = root.findViewById(R.id.LS);
        Button RS = root.findViewById(R.id.RS);
        ImageButton menuBtn = root.findViewById(R.id.menu);
        ImageButton viewBtn = root.findViewById(R.id.view);
        JoystickView leftJoystick = root.findViewById(R.id.left_joystick);
        JoystickView rightJoystick = root.findViewById(R.id.right_joystick);

        // ── Standard buttons (press = 1, release = 0) ─────────────
        setupStandardButton(a, gamepad, state, ButtonIndex.BTN_A, feedback);
        setupStandardButton(b, gamepad, state, ButtonIndex.BTN_B, feedback);
        setupStandardButton(x, gamepad, state, ButtonIndex.BTN_X, feedback);
        setupStandardButton(y, gamepad, state, ButtonIndex.BTN_Y, feedback);
        setupStandardButton(Lb, gamepad, state, ButtonIndex.BTN_LB, feedback);
        setupStandardButton(Rb, gamepad, state, ButtonIndex.BTN_RB, feedback);
        setupStandardButton(dpadUp, gamepad, state, ButtonIndex.BTN_DPAD_UP, feedback);
        setupStandardButton(dpadDown, gamepad, state, ButtonIndex.BTN_DPAD_DOWN, feedback);
        setupStandardButton(dpadLeft, gamepad, state, ButtonIndex.BTN_DPAD_LEFT, feedback);
        setupStandardButton(dpadRight, gamepad, state, ButtonIndex.BTN_DPAD_RIGHT, feedback);
        setupStandardButton(viewBtn, gamepad, state, ButtonIndex.BTN_VIEW, feedback);

        // ── Triggers (press = 255, release = 0) ───────────────────
        setupTrigger(Lt, gamepad, state, ButtonIndex.BTN_LT, feedback);
        setupTrigger(Rt, gamepad, state, ButtonIndex.BTN_RT, feedback);

        // ── Stick Click Toggles ───────────────────────────────────
        setupStickToggle(LS, gamepad, state, ButtonIndex.BTN_LS, feedback, activity, true);
        setupStickToggle(RS, gamepad, state, ButtonIndex.BTN_RS, feedback, activity, false);

        // ── Menu button (touch + long-press to unlock/lock drawer) ─
        setupMenuButton(menuBtn, gamepad, state, feedback, host, root);

        // ── Joysticks ─────────────────────────────────────────────
        leftJoystick.setOnJoystickMoveListener((angle, power, direction) -> {
            state.Lstick = convertToXboxAnalogRange(angle, power);
            gamepad.setLstick(state.Lstick);
        });
        leftJoystick.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) feedback.performFeedback();
            return false;
        });

        rightJoystick.setOnJoystickMoveListener((angle, power, direction) -> {
            state.Rstick = convertToXboxAnalogRange(angle, power);
            gamepad.setRstick(state.Rstick);
        });
        rightJoystick.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) feedback.performFeedback();
            return false;
        });

        // ── Input change listener ─────────────────────────────────
        gamepad.setOnInputChangedListener((Lstick, Rstick, buttons) -> {
            String player = host.getPlayer();
            host.send(player + "Lstick: " + Lstick[0] + ", " + Lstick[1]
                    + " | Rstick: " + Rstick[0] + ", " + Rstick[1]
                    + " | Buttons: " + Arrays.toString(buttons));
        });
        return gamepad;
    }

    // ── Private wiring helpers ────────────────────────────────────────

    private static void setupStandardButton(View button, InputObserver gamepad,
            State state, int buttonIndex, FeedbackManager feedback) {
        button.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    state.buttonState[buttonIndex] = 1;
                    gamepad.setButtonState(state.buttonState);
                    feedback.performFeedback();
                    return true;
                case MotionEvent.ACTION_UP:
                    state.buttonState[buttonIndex] = 0;
                    gamepad.setButtonState(state.buttonState);
                    return true;
            }
            return false;
        });
    }

    private static void setupTrigger(View trigger, InputObserver gamepad,
            State state, int buttonIndex, FeedbackManager feedback) {
        trigger.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    state.buttonState[buttonIndex] = 255;
                    gamepad.setButtonState(state.buttonState);
                    feedback.performFeedback();
                    return true;
                case MotionEvent.ACTION_UP:
                    state.buttonState[buttonIndex] = 0;
                    gamepad.setButtonState(state.buttonState);
                    return true;
            }
            return false;
        });
    }

    private static void setupStickToggle(Button button, InputObserver gamepad,
            State state, int buttonIndex, FeedbackManager feedback,
            Activity activity, boolean isLeft) {
        button.setOnClickListener(v -> {
            boolean isOn = isLeft ? state.LsOn : state.RsOn;
            if (isOn) {
                state.buttonState[buttonIndex] = 0;
                gamepad.setButtonState(state.buttonState);
                button.setBackgroundColor(
                        ContextCompat.getColor(activity, R.color.default_button_color));
            } else {
                state.buttonState[buttonIndex] = 1;
                gamepad.setButtonState(state.buttonState);
                button.setBackgroundColor(
                        ContextCompat.getColor(activity, R.color.clicked_button_color));
            }
            if (isLeft) state.LsOn = !isOn;
            else state.RsOn = !isOn;
            feedback.performFeedback();
        });
    }

    private static void setupMenuButton(ImageButton menuBtn, InputObserver gamepad,
                                        State state, FeedbackManager feedback, RemoteHost host, View root) {
        if (host.isDrawerLocked()) {
            menuBtn.clearColorFilter();
        } else {
            menuBtn.setColorFilter(
                    ContextCompat.getColor(root.getContext(), R.color.menu_tint));
        }
        // Touch: press/release
        menuBtn.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    state.buttonState[ButtonIndex.BTN_MENU] = 1;
                    gamepad.setButtonState(state.buttonState);
                    feedback.performFeedback();
                    return false; // allow long-click to fire
                case MotionEvent.ACTION_UP:
                    state.buttonState[ButtonIndex.BTN_MENU] = 0;
                    gamepad.setButtonState(state.buttonState);
                    return false;
            }
            return false;
        });

        // Long-press: toggle drawer lock
        menuBtn.setOnLongClickListener(v -> {
            View parentLayout = root.getRootView().findViewById(android.R.id.content);
            if (!host.isDrawerLocked()) {
                host.setDrawerLocked(true);
                Snackbar.make(parentLayout, "Menu Locked", Snackbar.LENGTH_SHORT).show();
                menuBtn.clearColorFilter();
            } else {
                host.setDrawerLocked(false);
                Snackbar.make(parentLayout, "Menu Unlocked", Snackbar.LENGTH_LONG).show();
                menuBtn.setColorFilter(
                        ContextCompat.getColor(root.getContext(), R.color.menu_tint));
            }
            return true;
        });
    }
}
