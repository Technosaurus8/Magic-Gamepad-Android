package com.technosaurus.MagicGamepad;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class BtSocket {
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // SPP UUID
    private static final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    public static BluetoothSocket socket;
    private static Set<BluetoothDevice> pairedDevices;

    @SuppressLint("MissingPermission")
    public static String[] getPairedDevicesList() {
        pairedDevices = bluetoothAdapter.getBondedDevices();
        List<String> pairedDevicesNames = new ArrayList<>();
        for (BluetoothDevice device : pairedDevices) {
            String deviceName = device.getName();   // Get the device name
            pairedDevicesNames.add(deviceName);     // Add to list
        }
        return pairedDevicesNames.toArray(new String[0]);  /* Convert List to Array 🤔 Why new String[0]?
        This is a common and efficient Java idiom used to convert a List<String> to a String[] array.*/
    }
    public static boolean isBluetoothAvailable(){
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }
    @SuppressLint("MissingPermission")
    public static BluetoothDevice getDeviceByName(String deviceName) {
        for (BluetoothDevice device : pairedDevices) {
            if (Objects.equals(deviceName, device.getName())) {
                return device;
            }
        }
        return null;
    }
    @SuppressLint("MissingPermission")// permission is requested in activity
    public static void connectToServer(BluetoothDevice device) {
        try {
            socket = device.createRfcommSocketToServiceRecord(MY_UUID);
            socket.connect();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void disconnect(){
        if(socket!=null) {
            try {
                sendToServer("Disconnect");
                socket.close();
            }catch (Exception e){
                e.printStackTrace();
            }

        }
    }
    public static boolean sendToServer(String messageToSend) {
        if (socket == null || !socket.isConnected()) {
            return false;
        }
        // Send a message to the C# server
        try {
            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(messageToSend.getBytes());
            outputStream.flush();
            // Now, wait for acknowledgment from the server
            return waitForAcknowledgment();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static boolean waitForAcknowledgment() {
        try {
            // Wait for a response from the server
            InputStream inputStream = socket.getInputStream();
            byte[] buffer = new byte[1024];
            int bytesRead = inputStream.read(buffer);
            String response = new String(buffer, 0, bytesRead);
            if (response.equals("ACK")) {  // Assuming the server sends "ACK" after each message
                return true;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}
