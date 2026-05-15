package com.technosaurus.MagicGamepad.screens;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import com.technosaurus.MagicGamepad.R;

public class SettingsActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    String TouchFeedback;
    private static final String PREFERENCES_FILE = "com.technosaurus.MagicGamepad.preferences";

    private static final String TOUCH_FEEDBACK_KEY = "touch_feedback_key";
    public void OpenActivity(Class<?> activityClass) {
        Intent intent = new Intent(this, activityClass);
        startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Button button = findViewById(R.id.button4);
        Spinner spinner = findViewById(R.id.spinner);
        spinner.setOnItemSelectedListener(this);

        button.setOnClickListener(v -> OpenActivity(CustomizeLayoutActivity.class));

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.items,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        // Retrieve saved value from SharedPreferences and set the spinner position
        SharedPreferences preferences = getSharedPreferences(PREFERENCES_FILE, MODE_PRIVATE);
        TouchFeedback = preferences.getString(TOUCH_FEEDBACK_KEY, "Sound");
        if (!TouchFeedback.isEmpty()) {
            int spinnerPosition = adapter.getPosition(TouchFeedback);
            spinner.setSelection(spinnerPosition);
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        TouchFeedback = parent.getItemAtPosition(pos).toString();

        // Save the selected value in SharedPreferences
        SharedPreferences preferences = getSharedPreferences(PREFERENCES_FILE, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(TOUCH_FEEDBACK_KEY, TouchFeedback);
        editor.apply();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // Another interface callback
    }
}
