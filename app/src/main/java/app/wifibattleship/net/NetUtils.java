package app.wifibattleship.net;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicInteger;

import app.wifibattleship.game.GameConfig;

public final class NetUtils {

    private static final AtomicInteger NAME_SEQ = new AtomicInteger(0);
    private static WifiManager.MulticastLock multicastLock;

    private NetUtils() {
    }

    public static boolean isWifiEnabled(Context context) {
        WifiManager wm = (WifiManager) context.getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
        return wm != null && wm.isWifiEnabled();
    }

    public static boolean isWifiConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            return false;
        }
        Network active = cm.getActiveNetwork();
        if (active == null) {
            return false;
        }
        NetworkCapabilities caps = cm.getNetworkCapabilities(active);
        return caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
    }

    public static boolean isWifiReady(Context context) {
        return isWifiEnabled(context) && isWifiConnected(context);
    }

    public static String getWifiIpAddress(Context context) {
        WifiManager wm = (WifiManager) context.getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
        if (wm == null) {
            return getLocalIpv4();
        }
        WifiInfo info = wm.getConnectionInfo();
        int ip = info.getIpAddress();
        if (ip == 0) {
            return getLocalIpv4();
        }
        @SuppressWarnings("deprecation")
        String dotted = Formatter.formatIpAddress(ip);
        return dotted;
    }

    private static String getLocalIpv4() {
        try {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            while (ifaces != null && ifaces.hasMoreElements()) {
                NetworkInterface ni = ifaces.nextElement();
                if (!ni.isUp() || ni.isLoopback()) {
                    continue;
                }
                Enumeration<InetAddress> addrs = ni.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress a = addrs.nextElement();
                    if (!a.isLoopbackAddress() && a.getHostAddress() != null
                            && a.getAddress().length == 4) {
                        return a.getHostAddress();
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public static int bindFreePort() {
        try (ServerSocket s = new ServerSocket(0)) {
            s.setReuseAddress(true);
            return s.getLocalPort();
        } catch (Exception e) {
            return GameConfig.SERVICE_PORT;
        }
    }

    public static String generateServiceName() {
        int n = NAME_SEQ.incrementAndGet();
        return GameConfig.SERVICE_PREFIX + (n & 0xFFFF) + "-" + android.os.Build.MODEL;
    }

    public static synchronized void acquireMulticastLock(Context context) {
        if (multicastLock != null && multicastLock.isHeld()) {
            return;
        }
        WifiManager wm = (WifiManager) context.getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
        if (wm == null) {
            return;
        }
        multicastLock = wm.createMulticastLock("wbs-nsd");
        multicastLock.setReferenceCounted(false);
        multicastLock.acquire();
    }

    public static synchronized void releaseMulticastLock() {
        if (multicastLock != null && multicastLock.isHeld()) {
            multicastLock.release();
        }
        multicastLock = null;
    }
}
