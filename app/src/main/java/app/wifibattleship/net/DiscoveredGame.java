package app.wifibattleship.net;

import android.net.wifi.p2p.WifiP2pDevice;

public record DiscoveredGame(String name, WifiP2pDevice device, int port) {
    public String getDeviceAddress() {
        return device.deviceAddress;
    }

    public String getDeviceName() {
        return device.deviceName;
    }
}
