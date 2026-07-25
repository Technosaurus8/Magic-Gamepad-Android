package com.technosaurus.MagicGamepad.input;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.technosaurus.MagicGamepad.connection.BluetoothHidManager;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequiresApi(api = Build.VERSION_CODES.P)
public class MessageToHid {

    private static final String TAG = "MessageToHid";

    // Wire format (after stripping "p{digit}"):
    // Lstick: {LY}, {LX} | Rstick: {RY}, {RX} | Buttons: [A, X, B, Y, LT, RT, LB, RB, L3, R3, Up, Down, Left, Right, Start, Back, Guide]
    // Groups 1–4  : axes
    // Groups 5–21 : 17 button values in order
    private static final Pattern GAMEPAD_PATTERN = Pattern.compile(
            "Lstick: (-?\\d+), (-?\\d+) \\| Rstick: (-?\\d+), (-?\\d+)\\s*\\|\\s*Buttons: " +
                    "\\[(\\d+), (\\d+), (\\d+), (\\d+), (\\d+), (\\d+), (\\d+), (\\d+), " +
                    "(\\d+), (\\d+), (\\d+), (\\d+), (\\d+), (\\d+), (\\d+), (\\d+), (\\d+)\\]"
    );
    private static final Pattern MOUSE_MOVE_PATTERN = Pattern.compile("^-?\\d+,-?\\d+$");
    private static final Pattern MOUSE_SCROLL_PATTERN = Pattern.compile("^[hv],-?\\d+$");
    private static int scrollAccumulator = 0;
    private static int scrollDivisor = 6;
    public static void processHidMessage(String msg) {
        msg = msg.trim();

        // ── Mouse movement ────────────────────────────────────────────────────
        if (MOUSE_MOVE_PATTERN.matcher(msg).matches()) {
            String[] parts = msg.split(",");
            BluetoothHidManager.INSTANCE.moveMouse(
                    Integer.parseInt(parts[0].trim()),
                    Integer.parseInt(parts[1].trim()));

            // ── Scroll ────────────────────────────────────────────────────────────
        } else if (MOUSE_SCROLL_PATTERN.matcher(msg).matches()) {
            int delta = Integer.parseInt(msg.split(",")[1].trim());
            scrollAccumulator += delta;

            int toSend = scrollAccumulator / scrollDivisor;
            if (toSend != 0) {
                BluetoothHidManager.INSTANCE.scrollMouse(toSend);
                scrollAccumulator -= toSend * scrollDivisor; // keep the remainder
            }

            // ── Keyboard ──────────────────────────────────────────────────────────
        } else if (msg.startsWith("k3y")) {
            BluetoothHidManager.INSTANCE.typeString(msg.substring(3));
        } // ── Gamepad ───────────────────────────────────────────────────────────
        else {
            //Log.d(TAG, "data: [" + data + "]");
            Matcher m = GAMEPAD_PATTERN.matcher(msg);
            if (m.matches()) {
                //Log.d(TAG, "MATCHED — LX=" + m.group(2) + " LY=" + m.group(1) + " A=" + m.group(5));
                // Axes — LY/LX are swapped in the wire format (Y comes first)
                // Parse all values first
                int LY = Integer.parseInt(m.group(1));
                int LX = Integer.parseInt(m.group(2));
                int RY = Integer.parseInt(m.group(3));
                int RX = Integer.parseInt(m.group(4));
                int LT = scale127_255(Integer.parseInt(m.group(9)));
                int RT = scale127_255(Integer.parseInt(m.group(10)));
                boolean a = !"0".equals(m.group(5));
                boolean x = !"0".equals(m.group(6));
                boolean b = !"0".equals(m.group(7));
                boolean y = !"0".equals(m.group(8));
                boolean lb = !"0".equals(m.group(11));
                boolean rb = !"0".equals(m.group(12));
                boolean ls = !"0".equals(m.group(13));
                boolean rs = !"0".equals(m.group(14));
                boolean up = !"0".equals(m.group(15));
                boolean down = !"0".equals(m.group(16));
                boolean left = !"0".equals(m.group(17));
                boolean right = !"0".equals(m.group(18));
                boolean start = !"0".equals(m.group(19));
                boolean back = !"0".equals(m.group(20));
                boolean guide = !"0".equals(m.group(21));

                // Write everything in one shot
                BluetoothHidManager.INSTANCE.setGamepadState(
                        LX, LY, RX, RY,
                        LT, RT,
                        a, b, x, y, lb, rb, ls, rs,
                        start, back, guide,
                        resolveDPad(up, down, left, right)
                );
                return;
            }
            Runnable command = _commands.get(msg);
            if (command != null) {
                command.run();
            }
        }
    }

    //Android devices behave weirdly if the triggers are 0 to 255. Like going reverse in beach buggy when releasing trigger.
    private static int scale127_255(int value) {
        return (int) Math.round(127 + value * 128.0 / 255.0);
    }
    private static BluetoothHidManager.DPad resolveDPad(boolean up, boolean down, boolean left, boolean right) {
        if (up   && right) return BluetoothHidManager.DPad.UP_RIGHT;
        if (down && right) return BluetoothHidManager.DPad.DOWN_RIGHT;
        if (down && left)  return BluetoothHidManager.DPad.DOWN_LEFT;
        if (up   && left)  return BluetoothHidManager.DPad.UP_LEFT;
        if (up)            return BluetoothHidManager.DPad.UP;
        if (down)          return BluetoothHidManager.DPad.DOWN;
        if (left)          return BluetoothHidManager.DPad.LEFT;
        if (right)         return BluetoothHidManager.DPad.RIGHT;
        return BluetoothHidManager.DPad.CENTERED;
    }

    private static final HashMap<String, Runnable> _commands = new HashMap<String, Runnable>() {{
        put("enter_down",       () -> BluetoothHidManager.INSTANCE.pressKey(BluetoothHidManager.KeyCode.ENTER));
        put("enter_up",         () -> BluetoothHidManager.INSTANCE.releaseKey(BluetoothHidManager.KeyCode.ENTER));

        put("tab_down",         () -> BluetoothHidManager.INSTANCE.pressKey(BluetoothHidManager.KeyCode.TAB));
        put("tab_up",           () -> BluetoothHidManager.INSTANCE.releaseKey(BluetoothHidManager.KeyCode.TAB));

        put("up_arrow_down",    () -> BluetoothHidManager.INSTANCE.pressKey(BluetoothHidManager.KeyCode.UP));
        put("up_arrow_up",      () -> BluetoothHidManager.INSTANCE.releaseKey(BluetoothHidManager.KeyCode.UP));

        put("down_arrow_down",  () -> BluetoothHidManager.INSTANCE.pressKey(BluetoothHidManager.KeyCode.DOWN));
        put("down_arrow_up",    () -> BluetoothHidManager.INSTANCE.releaseKey(BluetoothHidManager.KeyCode.DOWN));

        put("left_arrow_down",  () -> BluetoothHidManager.INSTANCE.pressKey(BluetoothHidManager.KeyCode.LEFT));
        put("left_arrow_up",    () -> BluetoothHidManager.INSTANCE.releaseKey(BluetoothHidManager.KeyCode.LEFT));

        put("right_arrow_down", () -> BluetoothHidManager.INSTANCE.pressKey(BluetoothHidManager.KeyCode.RIGHT));
        put("right_arrow_up",   () -> BluetoothHidManager.INSTANCE.releaseKey(BluetoothHidManager.KeyCode.RIGHT));

        put("backspace_down",   () -> BluetoothHidManager.INSTANCE.pressKey(BluetoothHidManager.KeyCode.BACKSPACE));
        put("backspace_up",     () -> BluetoothHidManager.INSTANCE.releaseKey(BluetoothHidManager.KeyCode.BACKSPACE));

        put("delete_down",      () -> BluetoothHidManager.INSTANCE.pressKey(BluetoothHidManager.KeyCode.DELETE));
        put("delete_up",        () -> BluetoothHidManager.INSTANCE.releaseKey(BluetoothHidManager.KeyCode.DELETE));

        put("esc_down",         () -> BluetoothHidManager.INSTANCE.pressKey(BluetoothHidManager.KeyCode.ESCAPE));
        put("esc_up",           () -> BluetoothHidManager.INSTANCE.releaseKey(BluetoothHidManager.KeyCode.ESCAPE));

        put("home_down",        () -> BluetoothHidManager.INSTANCE.pressKey(BluetoothHidManager.KeyCode.HOME));
        put("home_up",          () -> BluetoothHidManager.INSTANCE.releaseKey(BluetoothHidManager.KeyCode.HOME));

        put("end_down",         () -> BluetoothHidManager.INSTANCE.pressKey(BluetoothHidManager.KeyCode.END));
        put("end_up",           () -> BluetoothHidManager.INSTANCE.releaseKey(BluetoothHidManager.KeyCode.END));

        put("pgup_down",        () -> BluetoothHidManager.INSTANCE.pressKey(BluetoothHidManager.KeyCode.PAGE_UP));
        put("pgup_up",          () -> BluetoothHidManager.INSTANCE.releaseKey(BluetoothHidManager.KeyCode.PAGE_UP));

        put("pgdown_down",      () -> BluetoothHidManager.INSTANCE.pressKey(BluetoothHidManager.KeyCode.PAGE_DOWN));
        put("pgdown_up",        () -> BluetoothHidManager.INSTANCE.releaseKey(BluetoothHidManager.KeyCode.PAGE_DOWN));

        put("ctrl_down",        () -> BluetoothHidManager.INSTANCE.pressKey(BluetoothHidManager.KeyCode.NONE, BluetoothHidManager.Modifier.LEFT_CTRL));
        put("ctrl_up",          () -> BluetoothHidManager.INSTANCE.releaseModifiers(BluetoothHidManager.Modifier.LEFT_CTRL));

        put("shift_down",       () -> BluetoothHidManager.INSTANCE.pressKey(BluetoothHidManager.KeyCode.NONE, BluetoothHidManager.Modifier.LEFT_SHIFT));
        put("shift_up",         () -> BluetoothHidManager.INSTANCE.releaseModifiers(BluetoothHidManager.Modifier.LEFT_SHIFT));

        put("alt_down",         () -> BluetoothHidManager.INSTANCE.pressKey(BluetoothHidManager.KeyCode.NONE, BluetoothHidManager.Modifier.LEFT_ALT));
        put("alt_up",           () -> BluetoothHidManager.INSTANCE.releaseModifiers(BluetoothHidManager.Modifier.LEFT_ALT));

        put("win_down",         () -> BluetoothHidManager.INSTANCE.pressKey(BluetoothHidManager.KeyCode.NONE, BluetoothHidManager.Modifier.LEFT_GUI));
        put("win_up",           () -> BluetoothHidManager.INSTANCE.releaseModifiers(BluetoothHidManager.Modifier.LEFT_GUI));

        put("mute",             () -> BluetoothHidManager.INSTANCE.tapConsumerKey(BluetoothHidManager.ConsumerKey.VOL_MUTE));
        put("up",               () -> BluetoothHidManager.INSTANCE.tapConsumerKey(BluetoothHidManager.ConsumerKey.VOL_UP));
        put("down",             () -> BluetoothHidManager.INSTANCE.tapConsumerKey(BluetoothHidManager.ConsumerKey.VOL_DOWN));
        put("play",             () -> BluetoothHidManager.INSTANCE.tapConsumerKey(BluetoothHidManager.ConsumerKey.PLAY_PAUSE));
        put("prev",             () -> BluetoothHidManager.INSTANCE.tapConsumerKey(BluetoothHidManager.ConsumerKey.PREV));
        put("next",             () -> BluetoothHidManager.INSTANCE.tapConsumerKey(BluetoothHidManager.ConsumerKey.NEXT));

        put("mousedown",        () -> BluetoothHidManager.INSTANCE.pressMouseButton(BluetoothHidManager.MouseButton.LEFT));
        put("mouseup",          () -> BluetoothHidManager.INSTANCE.releaseMouseButton(BluetoothHidManager.MouseButton.LEFT));

        put("rmb_down",         () -> BluetoothHidManager.INSTANCE.pressMouseButton(BluetoothHidManager.MouseButton.RIGHT));
        put("rmb_up",           () -> BluetoothHidManager.INSTANCE.releaseMouseButton(BluetoothHidManager.MouseButton.RIGHT));

        put("mmb_down",         () -> BluetoothHidManager.INSTANCE.pressMouseButton(BluetoothHidManager.MouseButton.MIDDLE));
        put("mmb_up",           () -> BluetoothHidManager.INSTANCE.releaseMouseButton(BluetoothHidManager.MouseButton.MIDDLE));

        put("lmb",              () -> BluetoothHidManager.INSTANCE.clickMouseButton(BluetoothHidManager.MouseButton.LEFT));
        put("rmb",              () -> BluetoothHidManager.INSTANCE.clickMouseButton(BluetoothHidManager.MouseButton.RIGHT));
        put("mmb",              () -> BluetoothHidManager.INSTANCE.clickMouseButton(BluetoothHidManager.MouseButton.MIDDLE));
    }};
}