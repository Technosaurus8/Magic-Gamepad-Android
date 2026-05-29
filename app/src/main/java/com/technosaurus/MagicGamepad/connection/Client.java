package com.technosaurus.MagicGamepad.connection;

import android.util.Log;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.Arrays;

public class Client extends WebSocketClient {

    public boolean closed = false;

    public Client(URI serverUri) {
        super(serverUri);
        super.setConnectionLostTimeout(0);
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        //System.out.println("Connected to server");
    }

    @Override
    public void onMessage(String message) {

    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        closed=true;
        Log.d("Disconnected: ","Connection closed, code: " + code + ", reason: " + reason + "remote: "+ remote);
    }

    @Override
    public void onError(Exception ex) {
        Log.d("Error: " , Arrays.toString(ex.getStackTrace()));
    }

    //not used messages are sent via udp
    @Override
    public void send(String message) {
        if (isOpen()) {
            new Thread(() -> super.send(message)).start();
        } else {
            throw new RuntimeException("Disconnected");
            //showToast(context, "Disconnected", 300);
            //Log.d("","Disconnected");
        }
    }
}