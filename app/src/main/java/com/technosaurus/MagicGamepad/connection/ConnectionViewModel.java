package com.technosaurus.MagicGamepad.connection;

import android.app.Application;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ConnectionViewModel extends AndroidViewModel {
    private boolean isBt;
    private Client client;
    private UdpClient udp;
    private final ExecutorService sendExecutor = Executors.newSingleThreadExecutor();
    private final MutableLiveData<Boolean> disconnectedLiveData = new MutableLiveData<>(false);

    //for Wi-Fi connect other users can in the same network can connect without permission. so added a host approval mechanism.
    //no host approval is needed for bt connection because bluetooth already has a pairing mechanism.
    private final MutableLiveData<Boolean> approvedLiveData = new MutableLiveData<>(false);
    public ConnectionViewModel(@NonNull Application application) {
        super(application);
    }
    public void connect(boolean isBt, String ip, Intent intent) {
        Log.d("Connecting","");
        this.isBt = isBt;
        new Thread(() -> {
            if (isBt) {
                try {
                    if(BtSocket.connectToServer(BtSocket.getDeviceByName(intent.getStringExtra("selected_device")))){

                        approvedLiveData.postValue(true);
                    }
                    else{
                        disconnectedLiveData.postValue(true);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    disconnectedLiveData.postValue(true);
                }
            } else {
                try {
                    client = new Client(new URI("ws://" + ip),this);
                    udp = new UdpClient(ip);
                    // normal client.connect() method does not block ui, but I am using the connectBlocking method.
                    // because I already created a non-blocking thread for connecting so calling connect method here will result in
                    // execution of onConnected callback immediately even if the device is connected or not.
                    if(client.connectBlocking(5, TimeUnit.SECONDS)) {
                        client.send(Build.MODEL);
                        // auto reject after 30 seconds
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            if (approvedLiveData.getValue() != Boolean.TRUE) {
                                disconnectedLiveData.postValue(true);
                                client.close();
                            }
                        }, 30000);
                    }
                    else {
                        disconnectedLiveData.postValue(true);
                    }
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                    disconnectedLiveData.postValue(true);
                } catch (Exception e) {
                    e.printStackTrace();
                    disconnectedLiveData.postValue(true);
                }
            }
        }).start();
    }

    public void send(String msg) {
        sendExecutor.execute(() -> {
            if (isBt) {
                try {
                    BtSocket.sendToServer(msg);
                } catch (Exception e) {
                    Log.d("Disconnected: ",e.toString());
                    disconnectedLiveData.postValue(true);
                }
            } else {
                try {
                    if (client != null && !client.closed) {
                        udp.send(msg);
                    } else {
                        Log.d("Disconnected: ","Closed, Client:"+(client!=null?(client +"Closed: "+client.closed):"null"));
                        disconnectedLiveData.postValue(true);
                    }
                } catch (RuntimeException e) {
                    Log.d("Disconnected: ",e.toString());
                    disconnectedLiveData.postValue(true);
                }
            }
        });
    }

    public LiveData<Boolean> getDisconnectedLiveData() {
        return disconnectedLiveData;
    }
    public LiveData<Boolean> getApprovedLiveData() {
        return approvedLiveData;
    }
    public void onApproved() {
        approvedLiveData.postValue(true);
    }
    public void onDisconnect(){
        disconnectedLiveData.postValue(true);
    }
    public Client getClient() {
        return client;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        Log.d("Disconnecting","");
        if (isBt) {
            BtSocket.disconnect();
        } else {
            if (client != null) {
                client.close();
            }
            if (udp != null) {
                udp.close();
            }
        }
        sendExecutor.shutdownNow();
    }
}
