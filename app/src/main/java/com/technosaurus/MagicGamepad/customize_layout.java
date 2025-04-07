package com.technosaurus.MagicGamepad;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.zerokol.views.joystickView.JoystickView;

import java.util.Set;

public class customize_layout extends AppCompatActivity {
    private static final String PREFERENCES_FILE = "com.technosaurus.MagicGamepad.preferences";
    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;
    private int[][] Positions = new int[18][2];
    private int[][] Sizes = new int[18][2];
    private boolean[] isHidden = {true,true,true,true,true,true,true,true,true,true,true,true,true,true,true,true,true,true};
    private CustomLayout CustomLayout;
    private StringBuilder positionsString = new StringBuilder();
    private StringBuilder sizeString = new StringBuilder();

    private AlertDialog dialog;

    private int parent_height,parent_width;
    private Button Lt, Rt, Rb, Lb, LS, RS;
    private ImageButton a, b, x, y, dpadUp, dpadLeft, dpadRight, dpadDown, Menu, view_button;
    private JoystickView Rstick, Lstick;

    private TextView textView;
    private Button add_button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE);
        setContentView(R.layout.custom_layout);
        CustomLayout = findViewById(R.id.custom_layout);
        preferences = getSharedPreferences(PREFERENCES_FILE, MODE_PRIVATE);
        editor = preferences.edit();
        setFullscreen(this);
        add_button = findViewById(R.id.add);
        textView = findViewById(R.id.text);
        Lt = findViewById(R.id.lt);
        Lb = findViewById(R.id.Lb);
        Rb = findViewById(R.id.Rb);
        Rt = findViewById(R.id.rt);
        RS = findViewById(R.id.RS);
        LS = findViewById(R.id.LS);
        Rstick = findViewById(R.id.right_joystick);
        Lstick = findViewById(R.id.left_joystick);
        a = findViewById(R.id.a);
        b = findViewById(R.id.b);
        x = findViewById(R.id.x);
        y = findViewById(R.id.y);
        dpadUp = findViewById(R.id.dpad_up);
        dpadDown = findViewById(R.id.dpad_down);
        dpadLeft = findViewById(R.id.dpad_left);
        dpadRight = findViewById(R.id.dpad_right);
        view_button = findViewById(R.id.view);
        Menu = findViewById(R.id.menu);


        // Load positions and setup listeners
        Load_position("positions", Lt, 0);
        Load_size("sizes", Lt, 0);
        setupMoveAndResizeListener(Lt, 0);

        Load_position("positions", Lb, 1);
        Load_size("sizes", Lb, 1);
        setupMoveAndResizeListener(Lb, 1);

        Load_position("positions", Rb, 2);
        Load_size("sizes", Rb, 2);
        setupMoveAndResizeListener(Rb, 2);

        Load_position("positions", Rt, 3);
        Load_size("sizes", Rt, 3);
        setupMoveAndResizeListener(Rt, 3);

        Load_position("positions", RS, 4);
        Load_size("sizes", RS, 4);
        setupMoveAndResizeListener(RS, 4);

        Load_position("positions", LS, 5);
        Load_size("sizes", LS, 5);
        setupMoveAndResizeListener(LS, 5);

        Load_position("positions", Rstick, 6);
        Load_size("sizes", Rstick, 6);
        setupMoveAndResizeListener(Rstick, 6);

        Load_position("positions", Lstick, 7);
        Load_size("sizes", Lstick, 7);
        setupMoveAndResizeListener(Lstick, 7);

        Load_position("positions", a, 8);
        Load_size("sizes", a, 8);
        setupMoveAndResizeListener(a, 8);

        Load_position("positions", b, 9);
        Load_size("sizes", b, 9);
        setupMoveAndResizeListener(b, 9);

        Load_position("positions", x, 10);
        Load_size("sizes", x, 10);
        setupMoveAndResizeListener(x, 10);

        Load_position("positions", y, 11);
        Load_size("sizes", y, 11);
        setupMoveAndResizeListener(y, 11);

        Load_position("positions", dpadUp, 12);
        Load_size("sizes", dpadUp, 12);
        setupMoveAndResizeListener(dpadUp, 12);

        Load_position("positions", dpadDown, 13);
        Load_size("sizes", dpadDown, 13);
        setupMoveAndResizeListener(dpadDown, 13);

        Load_position("positions", dpadLeft, 14);
        Load_size("sizes", dpadLeft, 14);
        setupMoveAndResizeListener(dpadLeft, 14);

        Load_position("positions", dpadRight, 15);
        Load_size("sizes", dpadRight, 15);
        setupMoveAndResizeListener(dpadRight, 15);

        Load_position("positions", view_button, 16);
        Load_size("sizes", view_button, 16);
        setupMoveAndResizeListener(view_button, 16);

        Load_position("positions", Menu, 17);
        Load_size("sizes", Menu, 17);
        setupMoveAndResizeListener(Menu, 17);


        // Initialize OnGlobalLayoutListener
        ViewTreeObserver observer = CustomLayout.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                // Remove the listener to avoid multiple calls
                CustomLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                CustomLayout.moveViewTo(add_button,0,((CustomLayout.parentHeight/2)-CustomLayout.getViewHeight(add_button))*-1);
                parent_height=(CustomLayout.parentHeight/2);
                parent_width=(CustomLayout.parentWidth/2);
                CustomLayout.hideView(a);
                CustomLayout.hideView(b);
                CustomLayout.hideView(x);
                CustomLayout.hideView(y);
                CustomLayout.hideView(dpadUp);
                CustomLayout.hideView(dpadLeft);
                CustomLayout.hideView(dpadRight);
                CustomLayout.hideView(dpadDown);
                CustomLayout.hideView(Lt);
                CustomLayout.hideView(Rt);
                CustomLayout.hideView(Rb);
                CustomLayout.hideView(Lb);
                CustomLayout.hideView(LS);
                CustomLayout.hideView(RS);
                CustomLayout.hideView(Rstick);
                CustomLayout.hideView(Lstick);
                CustomLayout.hideView(Menu);
                CustomLayout.hideView(view_button);
                CustomLayout.hideView(textView);
                // You may need to call hideView again here if it relies on layout height
            }
        });
        isHidden=loadBooleanArray("isHidden");
        ViewTreeObserver observer2 = CustomLayout.getViewTreeObserver();
        observer2.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                                               @Override
                                               public void onGlobalLayout() {
                                                   CustomLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                                                   if (!isHidden[0]) {
                                                       CustomLayout.showView(Lt);
                                                   }
                                                   if (!isHidden[1]) {
                                                       CustomLayout.showView(Lb);
                                                   }
                                                   if (!isHidden[2]) {
                                                       CustomLayout.showView(Rb);
                                                   }
                                                   if (!isHidden[3]) {
                                                       CustomLayout.showView(Rt);
                                                   }
                                                   if (!isHidden[4]) {
                                                       CustomLayout.showView(RS);
                                                   }
                                                   if (!isHidden[5]) {
                                                       CustomLayout.showView(LS);
                                                   }
                                                   if (!isHidden[6]) {
                                                       CustomLayout.showView(Rstick);
                                                   }
                                                   if (!isHidden[7]) {
                                                       CustomLayout.showView(Lstick);
                                                   }
                                                   if (!isHidden[8]) {
                                                       CustomLayout.showView(a);
                                                   }
                                                   if (!isHidden[9]) {
                                                       CustomLayout.showView(b);
                                                   }
                                                   if (!isHidden[10]) {
                                                       CustomLayout.showView(x);
                                                   }
                                                   if (!isHidden[11]) {
                                                       CustomLayout.showView(y);
                                                   }
                                                   if (!isHidden[12]) {
                                                       CustomLayout.showView(dpadUp);
                                                   }
                                                   if (!isHidden[13]) {
                                                       CustomLayout.showView(dpadDown);
                                                   }
                                                   if (!isHidden[14]) {
                                                       CustomLayout.showView(dpadLeft);
                                                   }
                                                   if (!isHidden[15]) {
                                                       CustomLayout.showView(dpadRight);
                                                   }
                                                   if (!isHidden[16]) {
                                                       CustomLayout.showView(view_button);
                                                   }
                                                   if (!isHidden[17]) {
                                                       CustomLayout.showView(Menu);
                                                   }
                                               }
                                           });
        add_button.setOnClickListener(v -> showDialog());
    }

    private void showViewWithIndex(int index, View view) {
        dismissDialog();
        CustomLayout.showView(view);
        if (index >= 0 && index < isHidden.length) {
            isHidden[index] = false; // Set the corresponding index to true
        }
        saveBooleanArray("isHidden",isHidden);
    }
    private void hideViewWithIndex(int index, View view) {
        dismissDialog();
        CustomLayout.hideView(view);
        if (index >= 0 && index < isHidden.length) {
            isHidden[index] = true; // Set the corresponding index to true
        }
        saveBooleanArray("isHidden",isHidden);
    }
    private void setupMoveAndResizeListener(View view, int buttonNumber) {
        // Create a ScaleGestureDetector for handling pinch gestures
        final ScaleGestureDetector scaleDetector = new ScaleGestureDetector(view.getContext(), new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float scaleFactor = detector.getScaleFactor();
                CustomLayout.scaleViewSize(view,scaleFactor,((float) view.getWidth() / view.getHeight()));
                return true;
            }
        });

        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                scaleDetector.onTouchEvent(event);

                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        break;

                    case MotionEvent.ACTION_MOVE:
                        if (event.getPointerCount() == 1) {
                            int deltaX = (int) event.getX() - v.getWidth() / 2;
                            int deltaY = (int) event.getY() - v.getHeight() / 2;

                            // Move the view considering the boundaries
                            CustomLayout.moveViewToWithBoundaryCheck(v, deltaX, deltaY);
                        }
                        break;

                    case MotionEvent.ACTION_UP:
                        int[] coordinates = CustomLayout.getViewCoordinates(view);
                        save_position(coordinates, buttonNumber);
                        int[] size = {view.getWidth(), view.getHeight()};
                        save_size(size, buttonNumber);
                        break;
                }
                return true;
            }
        });

    }


    private void showDialog() {
        // Create a builder for the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select an element");

        // Get the layout inflater
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_selectcontrols, null);
        builder.setView(dialogView);

        // Find buttons within the dialog's view hierarchy
        Button[] buttons = {
                dialogView.findViewById(R.id.button1), dialogView.findViewById(R.id.button2),
                dialogView.findViewById(R.id.button3), dialogView.findViewById(R.id.button4),
                dialogView.findViewById(R.id.button5), dialogView.findViewById(R.id.button6),
                dialogView.findViewById(R.id.button7), dialogView.findViewById(R.id.button8),
                dialogView.findViewById(R.id.button9), dialogView.findViewById(R.id.button10),
                dialogView.findViewById(R.id.button11), dialogView.findViewById(R.id.button12),
                dialogView.findViewById(R.id.button13), dialogView.findViewById(R.id.button14),
                dialogView.findViewById(R.id.button15), dialogView.findViewById(R.id.button16),
                dialogView.findViewById(R.id.button18), dialogView.findViewById(R.id.button17)// swapped for a reason
        };

        View[] views = { Lt, Lb, Rb, Rt, RS, LS, Rstick, Lstick, a, b, x, y, dpadUp, dpadDown, dpadLeft, dpadRight, view_button, Menu };

        for (int i = 0; i < buttons.length; i++) {
            final int index = i;
            if (!isHidden[index]) {
                buttons[i].setBackgroundColor(ContextCompat.getColor(this, R.color.clicked_button_color));
            }
            buttons[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isHidden[index]) {
                        showViewWithIndex(index, views[index]);
                    } else {
                        hideViewWithIndex(index, views[index]);
                    }
                }
            });
        }
        // Create and show the dialog
        dialog = builder.create();
        dialog.show();
    }

    private void dismissDialog() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
            CustomLayout.postInvalidate();
            CustomLayout.invalidate();
        }
        setFullscreen(this);
        CustomLayout.postInvalidate();
        CustomLayout.invalidate();

    }
    public void setFullscreen(Activity activity) {
        View decorView = activity.getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }


    private void saveBooleanArray(String key, boolean[] array) {
        // Convert boolean array to a single string
        StringBuilder sb = new StringBuilder();
        for (boolean b : array) {
            sb.append(b ? '1' : '0').append(',');
        }
        // Remove the last comma
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }

        editor.putString(key, sb.toString());
        editor.apply();
    }

    private void save_position(int[] Coordinates, int buttonNumber) {
        Positions[buttonNumber][0] = Coordinates[0];
        Positions[buttonNumber][1] = Coordinates[1];

        // Clear the StringBuilder before appending new data
        positionsString.setLength(0);

        // Convert the 2D array to a single string with delimiters
        for (int i = 0; i < Positions.length; i++) {
            positionsString.append(Positions[i][0]).append(",").append(Positions[i][1]);
            if (i < Positions.length - 1) {
                positionsString.append(";");
            }
        }

        // Save the string to SharedPreferences
        editor.putString("positions", positionsString.toString());
        editor.apply();
    }
    private void save_size(int[] Dimensions, int buttonNumber) {
        Sizes[buttonNumber][0] = Dimensions[0];
        Sizes[buttonNumber][1] = Dimensions[1];

        // Clear the StringBuilder before appending new data
        sizeString.setLength(0);

        // Convert the 2D array to a single string with delimiters
        for (int i = 0; i < Sizes.length; i++) {
            sizeString.append(Sizes[i][0]).append(",").append(Sizes[i][1]);
            if (i < Sizes.length - 1) {
                sizeString.append(";");
            }
        }

        // Save the string to SharedPreferences
        editor.putString("sizes", sizeString.toString());
        editor.apply();
    }

    public boolean[] loadBooleanArray(String key) {
        boolean[] def = {true,true,true,true,true,true,true,true,true,true,true,true,true,true,true,true,true,true};
        String savedString = preferences.getString(key, null);
        if (savedString!=null) {
            boolean[] array = new boolean[18];
            String[] parts = savedString.split(",");
            for (int i = 0; i < parts.length; i++) {
                array[i] = parts[i].equals("1");
            }
            return array;
        }
        else {
            return def;
        }
    }

    private void Load_position(String key, View view, int buttonNumber) {
        String positionsString = preferences.getString(key, "");
        if (!positionsString.isEmpty()) {
            String[] positionsArray = positionsString.split(";");
            for (int i = 0; i < positionsArray.length; i++) {
                String[] coords = positionsArray[i].split(",");
                Positions[i][0] = Integer.parseInt(coords[0]);
                Positions[i][1] = Integer.parseInt(coords[1]);
            }
        }
        CustomLayout.moveViewTo(view, Positions[buttonNumber][0], Positions[buttonNumber][1]);
    }
    private void Load_size(String key, View view, int buttonNumber) {
        String sizeString = preferences.getString(key, "");
        if (!sizeString.isEmpty()) {
            String[] sizesArray = sizeString.split(";");
            for (int i = 0; i < sizesArray.length; i++) {
                String[] dimensions = sizesArray[i].split(",");
                Sizes[i][0] = Integer.parseInt(dimensions[0]);
                Sizes[i][1] = Integer.parseInt(dimensions[1]);
            }
        }
        if(Sizes[buttonNumber][0]!=0) {
            CustomLayout.setViewSize(view, Sizes[buttonNumber][0], Sizes[buttonNumber][1]);
        }
    }
}
