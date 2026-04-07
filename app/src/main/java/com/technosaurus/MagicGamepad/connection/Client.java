package com.technosaurus.MagicGamepad.connection;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

public class Client extends WebSocketClient {

    public boolean closed = false;

    public Client(URI serverUri) {
        super(serverUri);
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
        //System.out.println("Connection closed, code: " + code + ", reason: " + reason);
    }

    @Override
    public void onError(Exception ex) {

        //System.err.println("Error: " + ex.getMessage());
    }
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