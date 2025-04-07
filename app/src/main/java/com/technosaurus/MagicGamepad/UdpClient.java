package com.technosaurus.MagicGamepad;

import android.icu.text.SimpleDateFormat;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Locale;

public class UdpClient {

    private final String serverAddress;
    private final int serverPort;
    private DatagramSocket socket;
    private InetAddress address;

    public UdpClient(String serverAddressPort) throws Exception {
        int serverPort1;
        // Extract IP address and port from the combined string
        String[] parts = serverAddressPort.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid server address and port format. Expected format: ip:port");
        }
        this.serverAddress = parts[0];
        serverPort1 = Integer.parseInt(parts[1]);
        serverPort1++;
        this.serverPort = serverPort1;
        this.address = InetAddress.getByName(serverAddress);
        this.socket = new DatagramSocket();
    }

    public void send(String message) {
        if (socket != null && !socket.isClosed()) {
            byte[] buffer = message.getBytes(StandardCharsets.UTF_8);
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, serverPort);
            new Thread(() -> {
                try {
                    socket.send(packet);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        } else {
            throw new RuntimeException("Disconnected");
        }
    }

    public void close() {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}