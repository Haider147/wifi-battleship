package app.wifibattleship.net;

import android.content.Context;
import android.net.wifi.WifiManager;

import java.util.concurrent.atomic.AtomicInteger;

import app.wifibattleship.game.GameConfig;

public final class NetUtils {

    private static final AtomicInteger NAME_SEQ = new AtomicInteger(0);

    private NetUtils() {
    }

    /** WiFi Direct requiere el radio WiFi encendido (no hace falta estar conectado a una red). */
    public static boolean isWifiEnabled(Context context) {
        WifiManager wm = (WifiManager) context.getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
        return wm != null && wm.isWifiEnabled();
    }

    public static String generateServiceName() {
        int n = NAME_SEQ.incrementAndGet();
        return GameConfig.SERVICE_PREFIX + (n & 0xFFFF) + "-" + android.os.Build.MODEL;
    }
}
