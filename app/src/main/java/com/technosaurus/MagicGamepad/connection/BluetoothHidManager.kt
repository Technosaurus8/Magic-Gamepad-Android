package com.technosaurus.MagicGamepad.connection

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDevice.Callback
import android.bluetooth.BluetoothHidDevice.HID_DEVICE
import android.bluetooth.BluetoothHidDevice.STATE_CONNECTED
import android.bluetooth.BluetoothHidDevice.STATE_DISCONNECTED
import android.bluetooth.BluetoothHidDevice.SUBCLASS1_COMBO
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import java.util.concurrent.Executors

/**
 * BluetoothHidManager  — Kotlin singleton (object)
 *
 * Emulates a combo HID device over Bluetooth Classic:
 *   - Report ID 1 → Gamepad  (12 buttons, D-pad HAT, 4 axes, 2 triggers)
 *   - Report ID 2 → Keyboard (8 modifier bits + 6 simultaneous key codes)
 *   - Report ID 3 → Mouse    (3 buttons, X/Y relative, scroll wheel)
 *
 * Usage:
 *   BluetoothHidManager.init(context)         // call once in Application.onCreate()
 *   BluetoothHidManager.listener = ...
 *   BluetoothHidManager.register()            // call in Activity/Service onStart
 *   BluetoothHidManager.connect(device)
 *
 *   BluetoothHidManager.pressButton(GamepadButton.A)
 *   BluetoothHidManager.setLeftStick(x, y)    // -127..127
 *   BluetoothHidManager.tapKey(KeyCode.ENTER)
 *   BluetoothHidManager.moveMouse(dx, dy)
 *
 * Requires (AndroidManifest.xml):
 *   API 31+: BLUETOOTH_CONNECT, BLUETOOTH_ADVERTISE
 *   API <31: BLUETOOTH, BLUETOOTH_ADMIN
 */
@RequiresApi(api = Build.VERSION_CODES.P)
@SuppressLint("MissingPermission")
object BluetoothHidManager {

    private const val TAG = "BluetoothHidManager"

    private const val REPORT_ID_GAMEPAD  = 1
    private const val REPORT_ID_KEYBOARD = 2
    private const val REPORT_ID_MOUSE    = 3
    private const val REPORT_ID_CONSUMER = 4

    enum class DescriptorMode { ANDROID_GAMEPAD, JOYSTICK }
    lateinit var descriptorMode: DescriptorMode


    // ─────────────────────────────────────────────────────────────────────────
    // HID DESCRIPTOR  (three top-level collections, one per Report ID)
    // ─────────────────────────────────────────────────────────────────────────

    private val WINDOWS_JOYSTICK: ByteArray = byteArrayOf(
        // ═══════════════════════════════════════════════════════════════════
        // REPORT ID 1 — Gamepad
        // Maximum compatibility for windows.
        // ═══════════════════════════════════════════════════════════════════
        0x05, 0x01,                          // Usage Page (Generic Desktop)
        0x09, 0x04,                          // Usage (Joystick) ← shows in joy.cpl
        0xA1.toByte(), 0x01,                 // Collection (Application)
        0x85.toByte(), 0x01,                 //   Report ID (1)

        // Buttons 1–10 + 2 padding bits (+ 4-bit padding nibble) = 2 bytes
        0x05, 0x09,                          //   Usage Page (Button)
        0x19, 0x01,                          //   Usage Minimum (1)
        0x29, 0x0A,                          //   Usage Maximum (10)
        0x15, 0x00,                          //   Logical Minimum (0)
        0x25, 0x01,                          //   Logical Maximum (1)
        0x75, 0x01,                          //   Report Size (1 bit)
        0x95.toByte(), 0x0A,                 //   Report Count (10)
        0x81.toByte(), 0x02,                 //   Input (Data, Variable, Absolute)
        // 6-bit padding to align to 2 bytes
        0x75, 0x06,                          //   Report Size (6)
        0x95.toByte(), 0x01,                 //   Report Count (1)
        0x81.toByte(), 0x03,                 //   Input (Constant)

        // HAT switch (1 byte, 0–7, 8=centered)
        0x05, 0x01,                          //   Usage Page (Generic Desktop)
        0x09, 0x39,                          //   Usage (Hat Switch)
        0x15, 0x00,                          //   Logical Minimum (0)
        0x25, 0x07,                          //   Logical Maximum (7)
        0x35, 0x00,                          //   Physical Minimum (0°)
        0x46, 0x3B, 0x01,                    //   Physical Maximum (315°)
        0x65, 0x14,                          //   Unit (Degrees)
        0x75, 0x08,                          //   Report Size (8 bits)
        0x95.toByte(), 0x01,                 //   Report Count (1)
        0x81.toByte(), 0x42,                 //   Input (Data, Variable, Absolute, Null State)
        0x65, 0x00,                          //   Unit (None) — reset

        // Left stick: X/Y — 16-bit signed
        0x05, 0x01,                          //   Usage Page (Generic Desktop)
        0x09, 0x30,                          //   Usage (X)  ← LX
        0x09, 0x31,                          //   Usage (Y)  ← LY
        0x16, 0x00, 0x80.toByte(),           //   Logical Minimum (-32768)
        0x26, 0xFF.toByte(), 0x7F,           //   Logical Maximum (32767)
        0x75, 0x10,                          //   Report Size (16 bits)
        0x95.toByte(), 0x02,                 //   Report Count (2)
        0x81.toByte(), 0x02,                 //   Input (Data, Variable, Absolute)

        // Right stick: Rx/Ry — 16-bit signed
        0x09, 0x33,                          //   Usage (Rx) ← RX
        0x09, 0x34,                          //   Usage (Ry) ← RY
        0x16, 0x00, 0x80.toByte(),           //   Logical Minimum (-32768)
        0x26, 0xFF.toByte(), 0x7F,           //   Logical Maximum (32767)
        0x75, 0x10,                          //   Report Size (16 bits)
        0x95.toByte(), 0x02,                 //   Report Count (2)
        0x81.toByte(), 0x02,                 //   Input (Data, Variable, Absolute)

        // Triggers: Z/Rz — 8-bit unsigned
        0x09, 0x32,                          //   Usage (Z)  ← LT
        0x09, 0x35,                          //   Usage (Rz) ← RT
        0x15, 0x00,                          //   Logical Minimum (0)
        0x26, 0xFF.toByte(), 0x00,           //   Logical Maximum (255)
        0x75, 0x08,                          //   Report Size (8 bits)
        0x95.toByte(), 0x02,                 //   Report Count (2)
        0x81.toByte(), 0x02,                 //   Input (Data, Variable, Absolute)

        0xC0.toByte(),                       // End Collection (Joystick)
    )
    private val ANDROID_GAMEPAD: ByteArray = byteArrayOf(
        // ═══════════════════════════════════════════════════════════════════
        // REPORT ID 1 — Gamepad
        // Maximum compatibility for android.
        // ═══════════════════════════════════════════════════════════════════
        0x05, 0x01,        // Usage Page (Generic Desktop)
        0x09, 0x05,        // Usage (Gamepad)
        0xA1.toByte(), 0x01,        // Collection (Application)
        0x85.toByte(), 0x01,

        // ── Buttons: 17 bits + 7 padding = 3 bytes ──────────────────────────
        0x05, 0x09,        //   Usage Page (Button)
        0x19, 0x01,        //   Usage Minimum (1)
        0x29, 0x11,        //   Usage Maximum (17)  ← buttons 1–17
        0x15, 0x00,        //   Logical Minimum (0)
        0x25, 0x01,        //   Logical Maximum (1)
        0x75, 0x01,        //   Report Size (1 bit)
        0x95.toByte(), 0x11,        //   Report Count (17)
        0x81.toByte(), 0x02,        //   Input (Data, Variable, Absolute)
        // padding: 7 bits
        0x75, 0x07,        //   Report Size (7)
        0x95.toByte(), 0x01,        //   Report Count (1)
        0x81.toByte(), 0x03,        //   Input (Constant)

        // ── HAT switch: 4 bits + 4 padding = 1 byte ─────────────────────────
        0x05, 0x01,        //   Usage Page (Generic Desktop)
        0x09, 0x39,        //   Usage (Hat Switch)
        0x15, 0x00,        //   Logical Minimum (0)
        0x25, 0x07,        //   Logical Maximum (7)
        0x35, 0x00,        //   Physical Minimum (0)
        0x46, 0x3B, 0x01,  //   Physical Maximum (315°)
        0x65, 0x14,        //   Unit (Degrees)
        0x75, 0x04,        //   Report Size (4 bits)
        0x95.toByte(), 0x01,        //   Report Count (1)
        0x81.toByte(), 0x42,        //   Input (Data, Variable, Absolute, Null State)
        0x65, 0x00,        //   Unit (None) — reset
        // padding: 4 bits
        0x75, 0x04,
        0x95.toByte(), 0x01,
        0x81.toByte(), 0x03,        //   Input (Constant)

        // ── Axes: LX, LY, RX, RY — 16-bit signed each = 8 bytes ────────────
        0x09, 0x30,        //   Usage (X)   ← LX
        0x09, 0x31,        //   Usage (Y)   ← LY
        0x09, 0x32,        //   Usage (Z)   ← RX
        0x09, 0x35,        //   Usage (Rz)  ← RY
        0x16, 0x00, 0x80.toByte(),  //   Logical Minimum (-32768)
        0x26, 0xFF.toByte(), 0x7F,  //   Logical Maximum (32767)
        0x75, 0x10,        //   Report Size (16 bits)
        0x95.toByte(), 0x04,        //   Report Count (4)
        0x81.toByte(), 0x02,        //   Input (Data, Variable, Absolute)

        // ── Triggers: LT, RT — 8-bit unsigned each = 2 bytes ────────────────
        0x05, 0x02,        //   Usage Page (Simulation Controls)
        0x09, 0xC5.toByte(),        //   Usage (Brake)        ← LT
        0x09, 0xC4.toByte(),        //   Usage (Accelerator)  ← RT
        0x15, 0x00,        //   Logical Minimum (0)
        0x25, 0xFF.toByte(),        //   Logical Maximum (255)
        0x75, 0x08,        //   Report Size (8 bits)
        0x95.toByte(), 0x02,        //   Report Count (2)
        0x81.toByte(), 0x02,        //   Input (Data, Variable, Absolute)

        0xC0.toByte()               // End Collection
    )
    private val KEYBOARD_MOUSE: ByteArray = byteArrayOf(
        // ═══════════════════════════════════════════════════════════════════
        // REPORT ID 2 — KEYBOARD  (8 bytes)
        //   [0]     Modifier bitmask (LCtrl LShift LAlt LGUI RCtrl RShift RAlt RGUI)
        //   [1]     Reserved (0x00)
        //   [2..7]  Key codes (HID usage IDs, up to 6 simultaneous)
        // ═══════════════════════════════════════════════════════════════════
        0x05, 0x01,                    // Usage Page (Generic Desktop)
        0x09, 0x06,                    // Usage (Keyboard)
        0xA1.toByte(), 0x01,           // Collection (Application)
        0x85.toByte(), 0x02,           //   Report ID (2)

        // Modifier byte
        0x05, 0x07,                    //   Usage Page (Key Codes)
        0x19, 0xE0.toByte(),           //   Usage Minimum (Left Control)
        0x29, 0xE7.toByte(),           //   Usage Maximum (Right GUI)
        0x15, 0x00,                    //   Logical Minimum (0)
        0x25, 0x01,                    //   Logical Maximum (1)
        0x75, 0x01,                    //   Report Size (1 bit)
        0x95.toByte(), 0x08,           //   Report Count (8)
        0x81.toByte(), 0x02,           //   Input (Data, Var, Abs)

        // Reserved byte
        0x75, 0x08,                    //   Report Size (8)
        0x95.toByte(), 0x01,           //   Report Count (1)
        0x81.toByte(), 0x03,           //   Input (Const)

        // Key code array (6 keys)
        0x05, 0x07,                    //   Usage Page (Key Codes)
        0x19, 0x00,                    //   Usage Minimum (0)
        0x29, 0x91.toByte(),           //   Usage Maximum (0x91)
        0x15, 0x00,                    //   Logical Minimum (0)
        0x25, 0x91.toByte(),           //   Logical Maximum (0x91)
        0x75, 0x08,                    //   Report Size (8)
        0x95.toByte(), 0x06,           //   Report Count (6)
        0x81.toByte(), 0x00,           //   Input (Data, Array)

        0xC0.toByte(),                 // End Collection (Keyboard)

        // ═══════════════════════════════════════════════════════════════════
        // REPORT ID 3 — MOUSE  (4 bytes)
        //   [0]     Button bitmask (bit0=Left  bit1=Right  bit2=Middle)
        //   [1]     X delta  (-127..127, relative)
        //   [2]     Y delta  (-127..127, relative)
        //   [3]     Scroll wheel (-127..127, relative)
        // ═══════════════════════════════════════════════════════════════════
        0x05, 0x01,                    // Usage Page (Generic Desktop)
        0x09, 0x02,                    // Usage (Mouse)
        0xA1.toByte(), 0x01,           // Collection (Application)
        0x85.toByte(), 0x03,           //   Report ID (3)
        0x09, 0x01,                    //   Usage (Pointer)
        0xA1.toByte(), 0x00,           //   Collection (Physical)

        // Mouse buttons
        0x05, 0x09,                    //     Usage Page (Button)
        0x19, 0x01,                    //     Usage Minimum (1)
        0x29, 0x03,                    //     Usage Maximum (3)
        0x15, 0x00,                    //     Logical Minimum (0)
        0x25, 0x01,                    //     Logical Maximum (1)
        0x75, 0x01,                    //     Report Size (1 bit)
        0x95.toByte(), 0x03,           //     Report Count (3)
        0x81.toByte(), 0x02,           //     Input (Data, Var, Abs)

        // 5-bit padding
        0x75, 0x05,                    //     Report Size (5)
        0x95.toByte(), 0x01,           //     Report Count (1)
        0x81.toByte(), 0x03,           //     Input (Const)

        // X/Y relative movement
        0x05, 0x01,                    //     Usage Page (Generic Desktop)
        0x09, 0x30,                    //     Usage (X)
        0x09, 0x31,                    //     Usage (Y)
        0x15, 0x81.toByte(),           //     Logical Minimum (-127)
        0x25, 0x7F,                    //     Logical Maximum (127)
        0x75, 0x08,                    //     Report Size (8)
        0x95.toByte(), 0x02,           //     Report Count (2)
        0x81.toByte(), 0x06,           //     Input (Data, Var, Rel)

        // Scroll wheel
        0x09, 0x38,                    //     Usage (Wheel)
        0x15, 0x81.toByte(),           //     Logical Minimum (-127)
        0x25, 0x7F,                    //     Logical Maximum (127)
        0x75, 0x08,                    //     Report Size (8)
        0x95.toByte(), 0x01,           //     Report Count (1)
        0x81.toByte(), 0x06,           //     Input (Data, Var, Rel)

        0xC0.toByte(),                 //   End Collection (Physical)
        0xC0.toByte(),                  // End Collection (Mouse)
    )
    private val CONSUMER_CONTROL: ByteArray = byteArrayOf(
        // ═══════════════════════════════════════════════════════
        // REPORT ID 4 — Consumer Control (volume, media keys)
        //   [0..1]  Usage ID (16-bit, one key at a time)
        // ═══════════════════════════════════════════════════════
        0x05, 0x0C,                    // Usage Page (Consumer)
        0x09, 0x01,                    // Usage (Consumer Control)
        0xA1.toByte(), 0x01,           // Collection (Application)
        0x85.toByte(), 0x04,           //   Report ID (4)
        0x15, 0x00,                    //   Logical Minimum (0)
        0x26, 0xFF.toByte(), 0x03,     //   Logical Maximum (1023)
        0x19, 0x00,                    //   Usage Minimum (0)
        0x2A.toByte(), 0xFF.toByte(), 0x03, //   Usage Maximum (0x03FF)
        0x75, 0x10,                    //   Report Size (16 bits)
        0x95.toByte(), 0x01,           //   Report Count (1)
        0x81.toByte(), 0x00,           //   Input (Data, Array)
        0xC0.toByte(),                 // End Collection
    )
    // Combined descriptor = keyboard + mouse + gamepad (platform-specific)
    private val FULL_HID_DESCRIPTOR get() = when (descriptorMode) {
        DescriptorMode.ANDROID_GAMEPAD -> KEYBOARD_MOUSE + ANDROID_GAMEPAD + CONSUMER_CONTROL
        DescriptorMode.JOYSTICK -> KEYBOARD_MOUSE + WINDOWS_JOYSTICK + CONSUMER_CONTROL
    }

    // Android: specific bits to match KeyEvent.BUTTON_* constants
    private fun packAndroidButtons(
        a: Boolean, b: Boolean, x: Boolean, y: Boolean,
        lb: Boolean, rb: Boolean, back: Boolean, start: Boolean,
        ls: Boolean, rs: Boolean
    ): Int {
        var bits = 0
        if (a)     bits = bits or (1 shl 0)   // Button 1  → BUTTON_A
        if (b)     bits = bits or (1 shl 1)   // Button 2  → BUTTON_B
        if (x)     bits = bits or (1 shl 3)   // Button 4  → BUTTON_X
        if (y)     bits = bits or (1 shl 4)   // Button 5  → BUTTON_Y
        if (lb)    bits = bits or (1 shl 6)   // Button 7  → BUTTON_L1
        if (rb)    bits = bits or (1 shl 7)   // Button 8  → BUTTON_R1
        if (back)  bits = bits or (1 shl 10)  // Button 11 → BUTTON_SELECT
        if (start) bits = bits or (1 shl 11)  // Button 12 → BUTTON_START
        if (ls)    bits = bits or (1 shl 13)  // Button 14 → BUTTON_THUMBL
        if (rs)    bits = bits or (1 shl 14)  // Button 15 → BUTTON_THUMBR
        return bits
    }

    // Windows: sequential, no gaps — DirectInput button 1–10
    private fun packWindowsButtons(
        a: Boolean, b: Boolean, x: Boolean, y: Boolean,
        lb: Boolean, rb: Boolean, back: Boolean, start: Boolean,
        ls: Boolean, rs: Boolean
    ): Int {
        var bits = 0
        if (a)     bits = bits or (1 shl 0)  // Button 1
        if (b)     bits = bits or (1 shl 1)  // Button 2
        if (x)     bits = bits or (1 shl 2)  // Button 3
        if (y)     bits = bits or (1 shl 3)  // Button 4
        if (lb)    bits = bits or (1 shl 4)  // Button 5
        if (rb)    bits = bits or (1 shl 5)  // Button 6
        if (back)  bits = bits or (1 shl 6)  // Button 7
        if (start) bits = bits or (1 shl 7)  // Button 8
        if (ls)    bits = bits or (1 shl 8)  // Button 9
        if (rs)    bits = bits or (1 shl 9)  // Button 10
        return bits
    }
    /** D-Pad HAT direction. Use [CENTERED] to release. */
    enum class DPad(val value: Byte) {
        UP(0), UP_RIGHT(1), RIGHT(2), DOWN_RIGHT(3),
        DOWN(4), DOWN_LEFT(5), LEFT(6), UP_LEFT(7),
        CENTERED(8)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Keyboard modifier bitmask constants
    // ─────────────────────────────────────────────────────────────────────────
    object Modifier {
        const val NONE        = 0x00
        const val LEFT_CTRL   = 0x01
        const val LEFT_SHIFT  = 0x02
        const val LEFT_ALT    = 0x04
        const val LEFT_GUI    = 0x08
        const val RIGHT_CTRL  = 0x10
        const val RIGHT_SHIFT = 0x20
        const val RIGHT_ALT   = 0x40
        const val RIGHT_GUI   = 0x80
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HID keyboard usage IDs
    // ─────────────────────────────────────────────────────────────────────────
    object KeyCode {
        const val NONE      = 0x00
        const val A         = 0x04
        const val B         = 0x05
        const val C         = 0x06
        const val D         = 0x07
        const val E         = 0x08
        const val F         = 0x09
        const val G         = 0x0A
        const val H         = 0x0B
        const val I         = 0x0C
        const val J         = 0x0D
        const val K         = 0x0E
        const val L         = 0x0F
        const val M         = 0x10
        const val N         = 0x11
        const val O         = 0x12
        const val P         = 0x13
        const val Q         = 0x14
        const val R         = 0x15
        const val S         = 0x16
        const val T         = 0x17
        const val U         = 0x18
        const val V         = 0x19
        const val W         = 0x1A
        const val X         = 0x1B
        const val Y         = 0x1C
        const val Z         = 0x1D
        const val NUM_1     = 0x1E
        const val NUM_2     = 0x1F
        const val NUM_3     = 0x20
        const val NUM_4     = 0x21
        const val NUM_5     = 0x22
        const val NUM_6     = 0x23
        const val NUM_7     = 0x24
        const val NUM_8     = 0x25
        const val NUM_9     = 0x26
        const val NUM_0     = 0x27
        const val ENTER     = 0x28
        const val ESCAPE    = 0x29
        const val BACKSPACE = 0x2A
        const val TAB       = 0x2B
        const val SPACE     = 0x2C
        const val MINUS     = 0x2D
        const val EQUAL     = 0x2E
        const val F1        = 0x3A
        const val F2        = 0x3B
        const val F3        = 0x3C
        const val F4        = 0x3D
        const val F5        = 0x3E
        const val F6        = 0x3F
        const val F7        = 0x40
        const val F8        = 0x41
        const val F9        = 0x42
        const val F10       = 0x43
        const val F11       = 0x44
        const val F12       = 0x45
        const val PRINT_SCR = 0x46
        const val INSERT    = 0x49
        const val HOME      = 0x4A
        const val PAGE_UP   = 0x4B
        const val DELETE    = 0x4C
        const val END       = 0x4D
        const val PAGE_DOWN = 0x4E
        const val RIGHT     = 0x4F
        const val LEFT      = 0x50
        const val DOWN      = 0x51
        const val UP        = 0x52
    }
    object ConsumerKey {
        const val NONE       = 0x0000
        const val PLAY_PAUSE = 0x00CD
        const val NEXT       = 0x00B5
        const val PREV       = 0x00B6
        const val STOP       = 0x00B7
        const val VOL_UP     = 0x00E9
        const val VOL_DOWN   = 0x00EA
        const val VOL_MUTE   = 0x00E2
    }
    // ─────────────────────────────────────────────────────────────────────────
    // Mouse button bitmask constants
    // ─────────────────────────────────────────────────────────────────────────
    object MouseButton {
        const val LEFT   = 0x01
        const val RIGHT  = 0x02
        const val MIDDLE = 0x04
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Connection listener
    // ─────────────────────────────────────────────────────────────────────────
    interface Listener {
        fun onRegistered(success: Boolean)
        fun onConnectionStateChanged(device: BluetoothDevice, state: Int)
    }

    var listener: Listener? = null

    // ─────────────────────────────────────────────────────────────────────────
    // Internal state
    // ─────────────────────────────────────────────────────────────────────────
    private lateinit var appContext: Context

    private val _gamepadReportAndroid = ByteArray(14).also { it[3] = 8 }
    private val _gamepadReportWindows = ByteArray(13).also { it[2] = 8 }

    private val gamepadReport get() = when (descriptorMode) {
        DescriptorMode.ANDROID_GAMEPAD -> _gamepadReportAndroid
        DescriptorMode.JOYSTICK -> _gamepadReportWindows
    }
    private val keyboardReport = ByteArray(8)
    private val consumerReport = ByteArray(2)
    private val mouseReport    = ByteArray(4)
    private val pressedKeys    = mutableSetOf<Int>()
    private var heldModifiers: Int = Modifier.NONE

    private var hidDevice: BluetoothHidDevice? = null
    private var connectedHost: BluetoothDevice? = null
    private var isRegistered = false

    // using volatile to get the latest value from memory if different thread access the same variable
    // different thread may get different value for eg: UI thread(listener from setListener) false and other thread(internal Listener) true. so using volatile
    @Volatile
    private var ignoreDisconnect = false

    private val executor = Executors.newSingleThreadExecutor()

    // ─────────────────────────────────────────────────────────────────────────
    // HID callbacks
    // ─────────────────────────────────────────────────────────────────────────
    private val hidCallback = object : Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            isRegistered = registered
            Log.d(TAG, "HID registered: $registered")
            listener?.onRegistered(registered)
        }

        override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
            
            Log.d(TAG, "Connection: $state — ${device.name}")
            when (state) {
                STATE_CONNECTED -> {
                    connectedHost = device
                    resetGamepad()
                    releaseAllKeys()
                    releaseAllMouseButtons()
                    listener?.onConnectionStateChanged(device, state)
                }
                STATE_DISCONNECTED -> {
                    resetGamepad()
                    releaseAllKeys()
                    releaseAllMouseButtons()
                    if (connectedHost?.address == device.address)
                        connectedHost = null

                    if (ignoreDisconnect) {
                        ignoreDisconnect = false
                        return  // Ignore when device switch occurs.
                    }

                    listener?.onConnectionStateChanged(device, state)
                }
            }
        }

        override fun onGetReport(device: BluetoothDevice, type: Byte, id: Byte, bufferSize: Int) {
            val report = when (id.toInt()) {
                REPORT_ID_GAMEPAD  -> gamepadReport
                REPORT_ID_KEYBOARD -> keyboardReport
                REPORT_ID_MOUSE    -> mouseReport
                REPORT_ID_CONSUMER -> consumerReport
                else               -> return
            }
            
            hidDevice?.replyReport(device, type, id, report)
        }

        override fun onSetReport(device: BluetoothDevice, type: Byte, id: Byte, data: ByteArray) {}
        override fun onInterruptData(device: BluetoothDevice, reportId: Byte, data: ByteArray) {}
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Profile proxy listener
    // ─────────────────────────────────────────────────────────────────────────
    private val profileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile == HID_DEVICE) {
                hidDevice = proxy as BluetoothHidDevice
                Log.d(TAG, "HID profile connected — registering app")
                registerApp()
            }
        }
        override fun onServiceDisconnected(profile: Int) {
            hidDevice = null
            isRegistered = false
            Log.d(TAG, "HID profile disconnected")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Must be called once before any other method — ideally in Application.onCreate().
     * Stores application context so the singleton never leaks an Activity.
     */
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    /**
     * Register the HID app with the Bluetooth stack.
     * Call this in your Activity/Service onStart (after Bluetooth permissions are granted).
     */
    fun register(descriptorMode: DescriptorMode) {
        if (isRegistered) return
        val adapter = (appContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
        adapter.getProfileProxy(appContext, profileListener, HID_DEVICE)
        this.descriptorMode = descriptorMode
    }

    /**
     * Unregister and release the HID profile proxy.
     * Call this in onStop/onDestroy.
     */
    fun unregister() {
        
        hidDevice?.unregisterApp()
        val adapter = (appContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
        adapter.closeProfileProxy(HID_DEVICE, hidDevice)
        hidDevice = null
        isRegistered = false
    }

    /**
     * Connect to a paired host device.
     *
     * @return
     * - true  : device is already connected
     * - false : connection request failed
     * - null  : connection request initiated successfully, wait for
     *           [Listener.onConnectionStateChanged] for the final result
     */
    fun connect(device: BluetoothDevice): Boolean? {
        if (!isRegistered) { Log.w(TAG, "HID not registered yet"); return false }
        // the below if block is to return the connected device if the device is already connected.
        // this happens when pairing a device with hid profile it sometimes automatically connects.
        // so trying to execute connect again will not return and cause infinite loading. so returning already connected device.
        if(connectedHost==device){
            Log.d("Connection State: ", "Already connected")
            return true
        }
        if(connectedHost!=null){
            ignoreDisconnect = true
            
            hidDevice?.disconnect(connectedHost)
        }
        
        if (hidDevice?.connect(device) != true) return false
        return null
        // hidDevice?.connect(device) true does not mean it is connected it is just successful connection initiation.
        // if returning true for successful connection initiation it will remove the progressbar even if the device is not connected while still in connecting phase.
    }

    fun disconnect(device: BluetoothDevice) {
        
        hidDevice?.disconnect(device) ?: false
        connectedHost = null
    }



    val isConnected:     Boolean         get() = connectedHost != null
    val connectedDevice: BluetoothDevice? get() = connectedHost

    // ─────────────────────────────────────────────────────────────────────────
    // Gamepad API
    // ─────────────────────────────────────────────────────────────────────────
    private fun writeShort(index: Int, value: Int) {
        val v = value.coerceIn(-32768, 32767).toShort().toInt()
        gamepadReport[index]     = (v and 0xFF).toByte()
        gamepadReport[index + 1] = ((v shr 8) and 0xFF).toByte()
    }

    fun setGamepadState(
        lx: Int, ly: Int, rx: Int, ry: Int,
        lt: Int, rt: Int,
        a: Boolean, b: Boolean, x: Boolean, y: Boolean,
        lb: Boolean, rb: Boolean, ls: Boolean, rs: Boolean,
        start: Boolean, back: Boolean,
        guide: Boolean,
        dpad: DPad
    ) {
        when (descriptorMode) {
            DescriptorMode.ANDROID_GAMEPAD -> {
                val bits = packAndroidButtons(a, b, x, y, lb, rb, back, start, ls, rs)
                gamepadReport[0] = (bits and 0xFF).toByte()
                gamepadReport[1] = ((bits shr 8) and 0xFF).toByte()
                gamepadReport[2] = ((bits shr 16) and 0xFF).toByte()
                gamepadReport[3] = dpad.value
                writeShort(4,  lx)
                writeShort(6, -ly)
                writeShort(8,  rx)
                writeShort(10, -ry)
                gamepadReport[12] = lt.coerceIn(0, 255).toByte()
                gamepadReport[13] = rt.coerceIn(0, 255).toByte()
            }
            DescriptorMode.JOYSTICK -> {
                val bits = packWindowsButtons(a, b, x, y, lb, rb, back, start, ls, rs)
                gamepadReport[0] = (bits and 0xFF).toByte()
                gamepadReport[1] = ((bits shr 8) and 0xFF).toByte()
                gamepadReport[2] = dpad.value
                writeShort(3,  lx)
                writeShort(5, -ly)
                writeShort(7,  rx)
                writeShort(9, -ry)
                gamepadReport[11] = lt.coerceIn(0, 255).toByte()
                gamepadReport[12] = rt.coerceIn(0, 255).toByte()
            }
        }
        sendGamepad()
    }

    /** Reset all inputs to neutral. */
    fun resetGamepad() {
        gamepadReport.fill(0)
        when (descriptorMode) {
            DescriptorMode.ANDROID_GAMEPAD -> {
                gamepadReport[3] = 8
                gamepadReport[12] = 127
                gamepadReport[13] = 127
            }
            DescriptorMode.JOYSTICK -> {
                gamepadReport[2] = 8
                gamepadReport[11] = 127
                gamepadReport[12] = 127
            }
        }
        sendGamepad()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Keyboard API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Hold a key down with optional modifiers. Up to 6 simultaneous keys supported.
     *
     * Examples:
     *   pressKey(KeyCode.C, Modifier.LEFT_CTRL)
     *   pressKey(KeyCode.A, Modifier.LEFT_CTRL or Modifier.LEFT_SHIFT)
     */
    @JvmOverloads
    fun pressKey(keyCode: Int, modifiers: Int = Modifier.NONE) {
        if (modifiers != Modifier.NONE) heldModifiers = heldModifiers or modifiers
        keyboardReport[0] = heldModifiers.toByte()
        if (keyCode != KeyCode.NONE && keyCode !in pressedKeys && pressedKeys.size < 6)
            pressedKeys.add(keyCode)
        flushKeyboard()
    }

    /** Release a single key (modifiers remain until cleared). */
    fun releaseKey(keyCode: Int) {
        pressedKeys.remove(keyCode)
        flushKeyboard()
    }

    /** Clear specific modifier bits. */
    fun releaseModifiers(modifiers: Int) {
        heldModifiers = heldModifiers and modifiers.inv()
        keyboardReport[0] = heldModifiers.toByte()
        flushKeyboard()
    }

    /** Release all keys and modifiers immediately. */
    fun releaseAllKeys() {
        heldModifiers = Modifier.NONE
        keyboardReport.fill(0)
        pressedKeys.clear()
        sendKeyboard()
    }

    /**
     * Tap a key: press then immediately release.
     * Convenience for single keypresses that don't need hold behaviour.
     */
    @JvmOverloads
    fun tapKey(keyCode: Int, modifiers: Int = Modifier.NONE) {
        pressKey(keyCode, modifiers)
        releaseAllKeys()
    }
    fun tapConsumerKey(usage: Int) {
        consumerReport[0] = (usage and 0xFF).toByte()
        consumerReport[1] = ((usage shr 8) and 0xFF).toByte()
        sendConsumer()
        // release
        consumerReport[0] = 0; consumerReport[1] = 0
        sendConsumer()
    }
    fun typeString(text: String) {
        executor.execute {
            for (c in text) {
                val keyCode = charToHidKeyCode(c)
                if (keyCode == KeyCode.NONE) continue
                val modifier = if (c.isUpperCase()) Modifier.LEFT_SHIFT else Modifier.NONE

                // press
                if (modifier != Modifier.NONE) heldModifiers = heldModifiers or modifier
                keyboardReport[0] = heldModifiers.toByte()
                if (keyCode !in pressedKeys && pressedKeys.size < 6) pressedKeys.add(keyCode)
                flushKeyboardDirect()
                Thread.sleep(32)

                // release
                heldModifiers = Modifier.NONE
                keyboardReport.fill(0)
                pressedKeys.clear()
                sendDirect(REPORT_ID_KEYBOARD, keyboardReport)
                Thread.sleep(32)
            }
        }
    }

    private fun flushKeyboardDirect() {
        for (i in 2..7) keyboardReport[i] = 0
        pressedKeys.forEachIndexed { i, code -> keyboardReport[2 + i] = code.toByte() }
        sendDirect(REPORT_ID_KEYBOARD, keyboardReport)
    }
    fun charToHidKeyCode(c: Char): Int {
        if (c in 'a'..'z') return KeyCode.A + (c.code - 'a'.code)
        if (c in 'A'..'Z') return KeyCode.A + (c.code - 'A'.code)
        if (c in '1'..'9') return KeyCode.NUM_1 + (c.code - '1'.code)
        if (c == '0') return KeyCode.NUM_0
        if (c == ' ') return KeyCode.SPACE
        if (c == '\n') return KeyCode.ENTER
        return KeyCode.NONE
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Mouse API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Move the mouse cursor by a relative delta.
     * dx/dy are clamped to -127..127 per call; call repeatedly for large movements.
     */
    fun moveMouse(dx: Int, dy: Int) {
        mouseReport[1] = dx.coerceIn(-127, 127).toByte()
        mouseReport[2] = dy.coerceIn(-127, 127).toByte()
        sendMouse()
        mouseReport[1] = 0; mouseReport[2] = 0  // reset delta after sending
    }

    /** Scroll the wheel. Positive = up, negative = down. */
    fun scrollMouse(delta: Int) {
        mouseReport[3] = delta.coerceIn(-127, 127).toByte()
        sendMouse()
        mouseReport[3] = 0
    }

    /** Hold a mouse button. */
    fun pressMouseButton(button: Int) {
        mouseReport[0] = (mouseReport[0].toInt() or button).toByte()
        sendMouse()
    }

    /** Release a mouse button. */
    fun releaseMouseButton(button: Int) {
        mouseReport[0] = (mouseReport[0].toInt() and button.inv()).toByte()
        sendMouse()
    }

    /** Click a mouse button (press + release). */
    fun clickMouseButton(button: Int) {
        pressMouseButton(button)
        releaseMouseButton(button)
    }

    fun releaseAllMouseButtons() {
        mouseReport[0] = 0
        sendMouse()
    }


    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private fun registerApp() {
        val sdp = BluetoothHidDeviceAppSdpSettings(
            "Magic Gamepad Pro",
            "Android BT HID Controller",
            "Technosaurus",
            SUBCLASS1_COMBO,
            FULL_HID_DESCRIPTOR
        )
        
        hidDevice?.registerApp(sdp, null, null, executor, hidCallback)
    }

    private fun flushKeyboard() {
        for (i in 2..7) keyboardReport[i] = 0
        pressedKeys.forEachIndexed { i, code -> keyboardReport[2 + i] = code.toByte() }
        sendKeyboard()
    }

    private fun sendGamepad() = send(REPORT_ID_GAMEPAD, gamepadReport)
    private fun sendKeyboard() = send(REPORT_ID_KEYBOARD, keyboardReport)
    private fun sendConsumer() = send(REPORT_ID_CONSUMER, consumerReport)
    private fun sendMouse()    = send(REPORT_ID_MOUSE,    mouseReport)
    private fun send(reportId: Int, report: ByteArray): Boolean {
        val host = connectedHost ?: run {
            Log.w(TAG, "send($reportId) — connectedHost is null")
            return false
        }
        val snapshot = report.copyOf()  // ← capture state NOW, before any mutation
        executor.execute {
            
            val ok = hidDevice?.sendReport(host, reportId, snapshot) ?: false
            if (!ok) Log.d(TAG, "send($reportId) — failed")
        }
        return true
    }
    // sending without queuing used for typing string because typeString has its own queue.
    private fun sendDirect(reportId: Int, report: ByteArray): Boolean {
        val host = connectedHost ?: return false
        val snapshot = report.copyOf()
        
        return hidDevice?.sendReport(host, reportId, snapshot) ?: false
    }
}