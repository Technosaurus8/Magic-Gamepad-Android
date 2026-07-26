package com.technosaurus.MagicGamepad.util;

import android.content.SharedPreferences;

/**
 * Shared layout IDs and default-layout preference for RemoteActivity.
 */
public final class RemoteLayoutPrefs {

    public static final String PREFERENCES_FILE = "com.technosaurus.MagicGamepad.preferences";
    public static final String KEY_DEFAULT_LAYOUT = "default_remote_layout";

    public static final int LAYOUT_CUSTOM = 1;
    public static final int LAYOUT_GAMEPAD = 2;
    public static final int LAYOUT_TOUCHPAD = 3;
    public static final int LAYOUT_KEYBOARD = 4;
    public static final int LAYOUT_DRIVING = 5;

    private RemoteLayoutPrefs() {}

    public static boolean isValidLayout(int layoutId) {
        return layoutId == LAYOUT_CUSTOM
                || layoutId == LAYOUT_GAMEPAD
                || layoutId == LAYOUT_TOUCHPAD
                || layoutId == LAYOUT_KEYBOARD
                || layoutId == LAYOUT_DRIVING;
    }

    public static int getDefaultLayout(SharedPreferences prefs) {
        int layout = prefs.getInt(KEY_DEFAULT_LAYOUT, LAYOUT_GAMEPAD);
        return isValidLayout(layout) ? layout : LAYOUT_GAMEPAD;
    }

    public static void saveDefaultLayout(SharedPreferences prefs, int layoutId) {
        if (!isValidLayout(layoutId)) {
            return;
        }
        prefs.edit().putInt(KEY_DEFAULT_LAYOUT, layoutId).apply();
    }

    public static String layoutIdToLabel(int layoutId) {
        return switch (layoutId) {
            case LAYOUT_CUSTOM -> "Custom Layout";
            case LAYOUT_TOUCHPAD -> "Touchpad";
            case LAYOUT_KEYBOARD -> "Keyboard";
            case LAYOUT_DRIVING -> "Driving";
            case LAYOUT_GAMEPAD -> "Gamepad";
            default -> "Gamepad";
        };
    }
}
