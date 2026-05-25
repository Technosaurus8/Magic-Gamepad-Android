package com.technosaurus.MagicGamepad.connection;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import java.io.IOException;
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
        return pairedDevicesNames.toArray(new String[0]);  /* Convert List to Array. Why new String[0]?
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
    public static boolean connectToServer(BluetoothDevice device) {
        try {
            bluetoothAdapter.cancelDiscovery();
            if(socket!=null){
                socket.close();
            }
            socket = device.createRfcommSocketToServiceRecord(MY_UUID);
            socket.connect();
            return true;
        } catch (IOException e) {
            return false;
        }
    }
    public static boolean isConnected(){
        if(socket != null && socket.isConnected()){
            return true;
        }
        return false;
    }
    public static void disconnect(){
        if(socket!=null) {
            try {
                socket.close();
            }catch (Exception e){
                e.printStackTrace();
            }

        }
    }
    public static void sendToServer(String messageToSend) throws Exception {
        if(!isConnected()){
            throw new Exception();
        }
        OutputStream outputStream = socket.getOutputStream();
        outputStream.write((messageToSend+"\n").getBytes());
        outputStream.flush();
    }
}
