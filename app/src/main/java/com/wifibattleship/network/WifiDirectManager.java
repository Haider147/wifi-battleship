package com.wifibattleship.network;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WifiDirectManager implements ConnectionManager {

    public static final int PORT = 8888;
    public static final String HOST_IP = "192.168.49.1"; // Group Owner address in WiFi Direct

    private final WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private Socket socket;
    private PrintWriter writer;
    private MessageListener messageListener;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public WifiDirectManager(Context context) {
        manager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
    }

    public void initialize(Context context) {
        channel = manager.initialize(context, context.getMainLooper(), null);
    }

    @Override
    public void startDiscovery(DiscoveryCallback callback) {
        // Register WifiP2pBroadcastReceiver in Fragment for WIFI_P2P_PEERS_CHANGED_ACTION
        manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override public void onSuccess() {}
            @Override public void onFailure(int reason) {
                callback.onDiscoveryFailed("Error " + reason);
            }
        });
    }

    @Override
    public void connect(String deviceAddress, ConnectionCallback callback) {
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = deviceAddress;
        manager.connect(channel, config, new WifiP2pManager.ActionListener() {
            @Override public void onSuccess() {}
            @Override public void onFailure(int reason) {
                callback.onConnectionFailed("Error " + reason);
            }
        });
    }

    /** Llamar después de WIFI_P2P_CONNECTION_CHANGED_ACTION con formación de grupo confirmada. */
    public void openSocket(boolean isHost, ConnectionCallback callback) {
        executor.execute(() -> {
            try {
                if (isHost) {
                    ServerSocket server = new ServerSocket(PORT);
                    socket = server.accept();
                    server.close();
                } else {
                    socket = new Socket(HOST_IP, PORT);
                }
                writer = new PrintWriter(socket.getOutputStream(), true);
                callback.onConnected(isHost);
                listenForMessages();
            } catch (IOException e) {
                callback.onConnectionFailed(e.getMessage());
            }
        });
    }

    @Override
    public void sendMessage(String message) {
        executor.execute(() -> { if (writer != null) writer.println(message); });
    }

    @Override
    public void disconnect() {
        manager.removeGroup(channel, null);
        closeSocket();
    }

    @Override
    public void setMessageListener(MessageListener listener) {
        this.messageListener = listener;
    }

    private void listenForMessages() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            if (messageListener != null) messageListener.onMessageReceived(line);
        }
    }

    private void closeSocket() {
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
        socket = null;
        writer = null;
    }
}
