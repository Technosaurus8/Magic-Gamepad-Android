package com.technosaurus.MagicGamepad.connection;

import android.util.Log;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.Arrays;

public class Client extends WebSocketClient {

    public boolean closed = false;
    private ConnectionViewModel.ConnectCallback callback;

    private DisconnectListener disconnectListener;
    public Client(URI serverUri, ConnectionViewModel.ConnectCallback callback) {
        super(serverUri);
        super.setConnectionLostTimeout(0);
        this.callback = callback;
    }
    public interface DisconnectListener {
        void onDisconnected();
    }
    public void setDisconnectListener(DisconnectListener listener) {
        this.disconnectListener = listener;
    }
    @Override
    public void onOpen(ServerHandshake handshakedata) {
        //System.out.println("Connected to server");
    }

    @Override
    public void onMessage(String message) {
        if(message.equals("Approved")){
            if (callback != null) {
                callback.onConnected();
            }
        } else if (message.equals("Rejected")) {
            close();
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        closed=true;
        Log.d("Disconnected: ","Connection closed, code: " + code + ", reason: " + reason + "remote: "+ remote);
        if (disconnectListener != null) {
            disconnectListener.onDisconnected();
        }
    }

    @Override
    public void onError(Exception ex) {
        Log.d("Error: " , Arrays.toString(ex.getStackTrace()));
    }

    //used for sending device name to server
    @Override
    public void send(String message) {
        if (isOpen()) {
            new Thread(() -> super.send(message)).start();
        } else {
            Log.d("","Disconnected");
            //showToast(context, "Disconnected", 300);
        }
    }
}