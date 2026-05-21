package com.technosaurus.MagicGamepad.screens;

import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.technosaurus.MagicGamepad.util.FullscreenHelper;
import com.technosaurus.MagicGamepad.util.LayoutPrefsHelper;
import com.technosaurus.MagicGamepad.R;
import com.zerokol.views.joystickView.JoystickView;

/**
 * Activity for customizing the gamepad layout (drag/resize/show/hide buttons).
 * Refactored to use shared LayoutPrefsHelper and FullscreenHelper utilities.
 */
public class CustomizeLayoutActivity extends AppCompatActivity {

    private static final String PREFERENCES_FILE = "com.technosaurus.MagicGamepad.preferences";

    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;
    private int[][] Positions = new int[18][2];
    private int[][] Sizes = new int[18][2];
    private boolean[] isHidden;
    private CustomLayout customLayout;
    // All gamepad views, indexed consistently with LayoutPrefsHelper
    private View[] allViews;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        // Handle orientation for Android 15+ (API 35+)
        if (android.os.Build.VERSION.SDK_INT > 34) {
            int orientation = getResources().getConfiguration().orientation;
            if (orientation != Configuration.ORIENTATION_LANDSCAPE) {
                setContentView(R.layout.rotate_message);
                return;
            }
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE);
        }

        setContentView(R.layout.custom_layout);
        customLayout = findViewById(R.id.custom_layout);
        preferences = getSharedPreferences(PREFERENCES_FILE, MODE_PRIVATE);
        editor = preferences.edit();
        FullscreenHelper.setFullscreen(this);

        // Find all views
        Button addButton = findViewById(R.id.add);
        TextView textView = findViewById(R.id.text);
        Button Lt = findViewById(R.id.lt);
        Button Lb = findViewById(R.id.Lb);
        Button Rb = findViewById(R.id.Rb);
        Button Rt = findViewById(R.id.rt);
        Button RS = findViewById(R.id.RS);
        Button LS = findViewById(R.id.LS);
        JoystickView Rstick = findViewById(R.id.right_joystick);
        JoystickView Lstick = findViewById(R.id.left_joystick);
        ImageButton a = findViewById(R.id.a);
        ImageButton b = findViewById(R.id.b);
        ImageButton x = findViewById(R.id.x);
        ImageButton y = findViewById(R.id.y);
        ImageButton dpadUp = findViewById(R.id.dpad_up);
        ImageButton dpadDown = findViewById(R.id.dpad_down);
        ImageButton dpadLeft = findViewById(R.id.dpad_left);
        ImageButton dpadRight = findViewById(R.id.dpad_right);
        ImageButton viewButton = findViewById(R.id.view);
        ImageButton menuButton = findViewById(R.id.menu);

        // Indexed array for batch operations (same order as LayoutPrefsHelper)
        allViews = new View[]{Lt, Lb, Rb, Rt, RS, LS, Rstick, Lstick,
                a, b, x, y, dpadUp, dpadDown, dpadLeft, dpadRight, viewButton, menuButton};

        // Load positions, sizes, and setup listeners using loops instead of copy-paste
        Positions = LayoutPrefsHelper.loadPositions(preferences);
        Sizes = LayoutPrefsHelper.loadSizes(preferences);

        for (int i = 0; i < allViews.length; i++) {
            LayoutPrefsHelper.applyPosition(customLayout, allViews[i], Positions, i);
            LayoutPrefsHelper.applySize(customLayout, allViews[i], Sizes, i);
            setupMoveAndResizeListener(allViews[i], i);
        }

        // Hide all views initially, then show saved ones
        isHidden = LayoutPrefsHelper.loadBooleanArray(preferences);

        ViewTreeObserver observer = customLayout.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                customLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                customLayout.moveViewTo(addButton, 0,
                        ((customLayout.parentHeight / 2) - customLayout.getViewHeight(addButton)) * -1);

                // Hide all views first
                for (View v : allViews) {
                    customLayout.hideView(v);
                }
                customLayout.hideView(textView);
            }
        });

        ViewTreeObserver observer2 = customLayout.getViewTreeObserver();
        observer2.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                customLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                for (int i = 0; i < allViews.length; i++) {
                    if (!isHidden[i]) {
                        customLayout.showView(allViews[i]);
                    }
                }
            }
        });

        addButton.setOnClickListener(v -> showDialog());
    }

    // ── Move & Resize ───────────────────────────────────────────────

    private void setupMoveAndResizeListener(View view, int buttonNumber) {
        final ScaleGestureDetector scaleDetector = new ScaleGestureDetector(
                view.getContext(), new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float scaleFactor = detector.getScaleFactor();
                customLayout.scaleViewSize(view, scaleFactor,
                        ((float) view.getWidth() / view.getHeight()));
                return true;
            }
        });

        view.setOnTouchListener((v, event) -> {
            scaleDetector.onTouchEvent(event);
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_MOVE:
                    if (event.getPointerCount() == 1) {
                        int deltaX = (int) event.getX() - v.getWidth() / 2;
                        int deltaY = (int) event.getY() - v.getHeight() / 2;
                        customLayout.moveViewToWithBoundaryCheck(v, deltaX, deltaY);
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    int[] coordinates = customLayout.getViewCoordinates(view);
                    Positions[buttonNumber] = coordinates;
                    LayoutPrefsHelper.savePositions(editor, Positions);

                    Sizes[buttonNumber] = new int[]{view.getWidth(), view.getHeight()};
                    LayoutPrefsHelper.saveSizes(editor, Sizes);
                    break;
            }
            return true;
        });
    }

    // ── Dialog ───────────────────────────────────────────────────────

    private void showDialog() {
        ControlSelectDialogFragment dialog = ControlSelectDialogFragment.Companion.newInstance(
                isHidden,
                (index, hidden) -> {
                    if (hidden) {
                        customLayout.hideView(allViews[index]);
                        isHidden[index] = true;
                    } else {
                        customLayout.showView(allViews[index]);
                        isHidden[index] = false;
                    }
                    LayoutPrefsHelper.saveBooleanArray(editor, isHidden);
                    return null;
                }
        );
        dialog.show(getSupportFragmentManager(), "control_select");
    }
}
