package com.technosaurus.MagicGamepad.input;

/**
 * Constants for gamepad button indices used in the buttonState array.
 * Replaces magic numbers throughout the codebase.
 */
public final class ButtonIndex {
    public static final int BTN_A = 0;
    public static final int BTN_X = 1;
    public static final int BTN_B = 2;
    public static final int BTN_Y = 3;
    public static final int BTN_LT = 4;
    public static final int BTN_RT = 5;
    public static final int BTN_LB = 6;
    public static final int BTN_RB = 7;
    public static final int BTN_LS = 8;
    public static final int BTN_RS = 9;
    public static final int BTN_DPAD_UP = 10;
    public static final int BTN_DPAD_DOWN = 11;
    public static final int BTN_DPAD_LEFT = 12;
    public static final int BTN_DPAD_RIGHT = 13;
    public static final int BTN_MENU = 14;
    public static final int BTN_VIEW = 15;
    public static final int BTN_COUNT = 17; // legacy array size

    private ButtonIndex() {} // prevent instantiation
}
