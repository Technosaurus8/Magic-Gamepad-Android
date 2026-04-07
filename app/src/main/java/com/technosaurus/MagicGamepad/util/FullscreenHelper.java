package com.technosaurus.MagicGamepad.util;

import android.app.Activity;

import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

/**
 * Modern fullscreen utility using WindowInsetsControllerCompat.
 * Replaces deprecated SYSTEM_UI_FLAG_* constants (deprecated since API 30).
 */
public final class FullscreenHelper {

    private FullscreenHelper() {}

    public static void setFullscreen(Activity activity) {
        if (activity == null || activity.getWindow() == null) return;
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(activity.getWindow(),
                        activity.getWindow().getDecorView());
        controller.hide(WindowInsetsCompat.Type.systemBars());
        controller.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
    }

    public static void exitFullscreen(Activity activity) {
        if (activity == null || activity.getWindow() == null) return;
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(activity.getWindow(),
                        activity.getWindow().getDecorView());
        controller.show(WindowInsetsCompat.Type.systemBars());
    }
}
