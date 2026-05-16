package com.wifibattleship.network;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SuppressLint("MissingPermission")
public class BluetoothConnectionManager implements ConnectionManager {

    public static final UUID SERVICE_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    public static final String SERVICE_NAME = "WiFiBattleship";

    private final BluetoothAdapter bluetoothAdapter;
    private BluetoothServerSocket serverSocket;
    private BluetoothSocket socket;
    private PrintWriter writer;
    private MessageListener messageListener;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public BluetoothConnectionManager(Context context) {
        BluetoothManager btManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = btManager.getAdapter();
    }

    @Override
    public void startDiscovery(DiscoveryCallback callback) {
        // Registrar BroadcastReceiver para BluetoothDevice.ACTION_FOUND en el Fragment/Activity
        bluetoothAdapter.startDiscovery();
    }

    @Override
    public void connect(String deviceAddress, ConnectionCallback callback) {
        executor.execute(() -> {
            try {
                bluetoothAdapter.cancelDiscovery();
                BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
                socket = device.createRfcommSocketToServiceRecord(SERVICE_UUID);
                socket.connect();
                writer = new PrintWriter(socket.getOutputStream(), true);
                callback.onConnected(false);
                listenForMessages();
            } catch (IOException e) {
                callback.onConnectionFailed(e.getMessage());
            }
        });
    }

    public void listenAsHost(ConnectionCallback callback) {
        executor.execute(() -> {
            try {
                serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(SERVICE_NAME, SERVICE_UUID);
                socket = serverSocket.accept();
                serverSocket.close();
                writer = new PrintWriter(socket.getOutputStream(), true);
                callback.onConnected(true);
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
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
        try { if (serverSocket != null) serverSocket.close(); } catch (IOException ignored) {}
        socket = null;
        writer = null;
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
}
