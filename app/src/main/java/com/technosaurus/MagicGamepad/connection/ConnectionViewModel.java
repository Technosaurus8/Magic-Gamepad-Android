package com.technosaurus.MagicGamepad.connection;

import android.app.Application;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConnectionViewModel extends AndroidViewModel {
    private boolean isBt;
    private Client client;
    private UdpClient udp;
    private final ExecutorService sendExecutor = Executors.newSingleThreadExecutor();
    private final MutableLiveData<Boolean> disconnectedLiveData = new MutableLiveData<>(false);

    public ConnectionViewModel(@NonNull Application application) {
        super(application);
    }
    public interface ConnectCallback {
        void onConnected();
    }

    public void connect(boolean isBt, String ip, Intent intent,ConnectCallback callback) {
        Log.d("Connecting","");
        this.isBt = isBt;
        new Thread(() -> {
            if (isBt) {
                try {
                    BtSocket.connectToServer(BtSocket.getDeviceByName(intent.getStringExtra("selected_device")));
                    callback.onConnected();
                } catch (Exception e) {
                    e.printStackTrace();
                    disconnectedLiveData.postValue(true);
                }
            } else {
                try {
                    client = new Client(new URI("ws://" + ip));
                    udp = new UdpClient(ip);
                    client.connect();
                    callback.onConnected();
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
                    disconnectedLiveData.postValue(true);
                }
            } else {
                try {
                    if (client != null && !client.closed) {
                        udp.send(msg);
                    } else {
                        disconnectedLiveData.postValue(true);
                    }
                } catch (RuntimeException e) {
                    disconnectedLiveData.postValue(true);
                }
            }
        });
    }

    public LiveData<Boolean> getDisconnectedLiveData() {
        return disconnectedLiveData;
    }

    public Client getClient() {
        return client;
    }

    public UdpClient getUdp() {
        return udp;
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
