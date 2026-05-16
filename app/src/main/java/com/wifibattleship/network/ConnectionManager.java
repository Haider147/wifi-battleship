package com.wifibattleship.network;

public interface ConnectionManager {

    void startDiscovery(DiscoveryCallback callback);
    void connect(String deviceAddress, ConnectionCallback callback);
    void sendMessage(String message);
    void disconnect();
    void setMessageListener(MessageListener listener);

    interface DiscoveryCallback {
        void onDeviceFound(String name, String address);
        void onDiscoveryFailed(String reason);
    }

    interface ConnectionCallback {
        void onConnected(boolean isHost);
        void onConnectionFailed(String reason);
        void onDisconnected();
    }

    interface MessageListener {
        void onMessageReceived(String message);
    }
}
