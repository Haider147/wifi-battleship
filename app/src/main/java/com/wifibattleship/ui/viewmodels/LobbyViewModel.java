package com.wifibattleship.ui.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import java.util.ArrayList;
import java.util.List;

public class LobbyViewModel extends ViewModel {

    private final MutableLiveData<Boolean> connectionReady = new MutableLiveData<>(false);
    private final MutableLiveData<List<String[]>> discoveredDevices = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> statusMessage = new MutableLiveData<>("Listo para buscar");

    public LiveData<Boolean> getConnectionReady() { return connectionReady; }
    public LiveData<List<String[]>> getDiscoveredDevices() { return discoveredDevices; }
    public LiveData<String> getStatusMessage() { return statusMessage; }

    public void startDiscovery(String connectionType) {
        statusMessage.setValue("Buscando dispositivos…");
        // TODO: delegate to WifiDirectManager o BluetoothConnectionManager
    }

    public void connectToDevice(String address) {
        statusMessage.setValue("Conectando…");
        // TODO: ConnectionManager.connect()
    }

    public void onConnected(boolean isHost) {
        statusMessage.setValue(isHost ? "Conectado como host" : "Conectado como cliente");
        connectionReady.setValue(true);
    }
}
