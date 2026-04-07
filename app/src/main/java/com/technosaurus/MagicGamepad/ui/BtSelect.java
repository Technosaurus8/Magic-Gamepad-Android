package com.technosaurus.MagicGamepad.ui;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.technosaurus.MagicGamepad.R;
import com.technosaurus.MagicGamepad.connection.BtSocket;

public class BtSelect extends AppCompatActivity {
    ListView listView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bt_select);
        listView = findViewById(R.id.listView);
        request_permission();
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(bluetoothReceiver, filter);
    }
    void request_permission(){
        // Request Bluetooth permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            android.Manifest.permission.BLUETOOTH,
                            android.Manifest.permission.BLUETOOTH_CONNECT,
                            android.Manifest.permission.BLUETOOTH_SCAN,
                    }, 1);
        }
        else {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.BLUETOOTH_ADMIN,
                            android.Manifest.permission.BLUETOOTH,
                    }, 1);
        }
    }
    void setListViewEnabled(){
        String[] pairedDevices = BtSocket.getPairedDevicesList();
        if (pairedDevices != null) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_list_item_1,
                    pairedDevices
            );
            listView.setAdapter(adapter);
            listView.setOnItemClickListener((parent, view, position, id) -> {
                String clickedItem = pairedDevices[position];
                Intent intent = new Intent(this, RemoteActivity.class);
                intent.putExtra("selected_device", clickedItem);
                startActivity(intent);
            });
        } else {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_list_item_1,
                    new String[]{"No devices found"}
            );
            listView.setAdapter(adapter);
        }
    }
    void setListViewDisabled(){
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                new String[]{"Bluetooth not enabled"}
        );
        listView.setAdapter(adapter);
    }
    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                if (state == BluetoothAdapter.STATE_TURNING_OFF || state == BluetoothAdapter.STATE_OFF) {
                    setListViewDisabled();
                } else if (state == BluetoothAdapter.STATE_ON) {
                    setListViewEnabled();
                }

            }
        }
    };
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0) {
                boolean allGranted = true;
                for (int result : grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        allGranted = false;
                        break;
                    }
                }
                if(allGranted) {
                    Log.d("Permission:", "Allowed");
                    if (!(BtSocket.isBluetoothAvailable())) {
                        setListViewDisabled();
                        return;
                    }
                    setListViewEnabled();
                } else {
                    Log.d("Permission:","Denied");
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this,
                            android.R.layout.simple_list_item_1,
                            new String[]{"Request Bluetooth Permissions"}
                    );
                    listView.setAdapter(adapter);
                    listView.setOnItemClickListener((parent, view, position, id) -> {
                        request_permission();
                    });
                }
            }
        }
    }
}