package com.technosaurus.MagicGamepad.util;

import android.content.SharedPreferences;
import android.view.View;

import com.technosaurus.MagicGamepad.ui.CustomLayout;

import java.util.Arrays;

/**
 * Shared utility for loading/saving custom layout positions, sizes, and boolean arrays
 * from SharedPreferences. Eliminates duplicate code in remote.java and customize_layout.java.
 */
public final class LayoutPrefsHelper {

    public static final String KEY_POSITIONS = "positions";
    public static final String KEY_SIZES = "sizes";
    public static final String KEY_IS_HIDDEN = "isHidden";
    private static final int ELEMENT_COUNT = 18;

    private LayoutPrefsHelper() {}

    // ── Positions ──────────────────────────────────────────────────────

    public static int[][] loadPositions(SharedPreferences prefs) {
        return loadIntPairs(prefs, KEY_POSITIONS);
    }

    public static void savePositions(SharedPreferences.Editor editor, int[][] positions) {
        saveIntPairs(editor, KEY_POSITIONS, positions);
    }

    public static void applyPosition(CustomLayout layout, View view, int[][] positions, int index) {
        layout.moveViewTo(view, positions[index][0], positions[index][1]);
    }

    // ── Sizes ──────────────────────────────────────────────────────────

    public static int[][] loadSizes(SharedPreferences prefs) {
        return loadIntPairs(prefs, KEY_SIZES);
    }

    public static void saveSizes(SharedPreferences.Editor editor, int[][] sizes) {
        saveIntPairs(editor, KEY_SIZES, sizes);
    }

    public static void applySize(CustomLayout layout, View view, int[][] sizes, int index) {
        if (sizes[index][0] != 0) {
            layout.setViewSize(view, sizes[index][0], sizes[index][1]);
        }
    }

    // ── Boolean array (hidden state) ──────────────────────────────────

    public static boolean[] loadBooleanArray(SharedPreferences prefs) {
        boolean[] def = new boolean[ELEMENT_COUNT];
        Arrays.fill(def, true);
        String savedString = prefs.getString(KEY_IS_HIDDEN, null);
        if (savedString != null) {
            boolean[] array = new boolean[ELEMENT_COUNT];
            String[] parts = savedString.split(",");
            for (int i = 0; i < parts.length && i < ELEMENT_COUNT; i++) {
                array[i] = parts[i].equals("1");
            }
            return array;
        }
        return def;
    }

    public static void saveBooleanArray(SharedPreferences.Editor editor, boolean[] array) {
        StringBuilder sb = new StringBuilder();
        for (boolean b : array) {
            sb.append(b ? '1' : '0').append(',');
        }
        if (sb.length() > 0) sb.setLength(sb.length() - 1);
        editor.putString(KEY_IS_HIDDEN, sb.toString());
        editor.apply();
    }

    // ── Private helpers ───────────────────────────────────────────────

    private static int[][] loadIntPairs(SharedPreferences prefs, String key) {
        int[][] data = new int[ELEMENT_COUNT][2];
        String raw = prefs.getString(key, "");
        if (!raw.isEmpty()) {
            String[] entries = raw.split(";");
            for (int i = 0; i < entries.length && i < ELEMENT_COUNT; i++) {
                String[] parts = entries[i].split(",");
                if (parts.length >= 2) {
                    data[i][0] = Integer.parseInt(parts[0]);
                    data[i][1] = Integer.parseInt(parts[1]);
                }
            }
        }
        return data;
    }

    private static void saveIntPairs(SharedPreferences.Editor editor, String key, int[][] data) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
            sb.append(data[i][0]).append(",").append(data[i][1]);
            if (i < data.length - 1) sb.append(";");
        }
        editor.putString(key, sb.toString());
        editor.apply();
    }
}
