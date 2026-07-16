package app.wifibattleship.net;

import android.net.wifi.p2p.WifiP2pDevice;

public final class DiscoveredGame {

    private final String name;
    private final WifiP2pDevice device;
    private final int port;

    public DiscoveredGame(String name, WifiP2pDevice device, int port) {
        this.name = name;
        this.device = device;
        this.port = port;
    }

    public String getName() {
        return name;
    }

    public WifiP2pDevice getDevice() {
        return device;
    }

    public String getDeviceAddress() {
        return device.deviceAddress;
    }

    public String getDeviceName() {
        return device.deviceName;
    }

    public int getPort() {
        return port;
    }
}
