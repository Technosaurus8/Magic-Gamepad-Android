package com.technosaurus.MagicGamepad.util;
import android.view.View;
import com.technosaurus.MagicGamepad.screens.CustomLayout;

/**
 * Shared utility for loading/saving custom layout positions, sizes, and boolean arrays
 * from SharedPreferences. Eliminates duplicate code in remote.java and customize_layout.java.
 */
public final class CustomLayoutPrefsHelper {

    private CustomLayoutPrefsHelper() {}

    // ── Positions ──────────────────────────────────────────────────────

    public static void applyPosition(CustomLayout layout, View view, int[][] positions, int index) {
        layout.setViewPosition(view, positions[index][0], positions[index][1]);
    }

    /** Apply saved size then position so centering uses the correct dimensions. */
    public static void applyLayout(CustomLayout layout, View view, int[][] positions, int[][] sizes, int index) {
        applySize(layout, view, sizes, index);
        applyPosition(layout, view, positions, index);
    }

    public static void applySize(CustomLayout layout, View view, int[][] sizes, int index) {
        if (sizes[index][0] != 0) {
            layout.setViewSize(view, sizes[index][0], sizes[index][1]);
        }
    }
}
