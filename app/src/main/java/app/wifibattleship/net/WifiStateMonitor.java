package app.wifibattleship.net;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pManager;

/**
 * Observa en vivo el estado de WiFi P2P mediante el broadcast (sticky)
 * WIFI_P2P_STATE_CHANGED_ACTION: al registrarse entrega el estado actual.
 */
@SuppressWarnings("deprecation")
public final class WifiStateMonitor {

    public interface Listener {
        void onWifiChanged(boolean ready);
    }

    private final Context appContext;
    private final Listener listener;
    private final BroadcastReceiver receiver;
    private boolean registered = false;

    public WifiStateMonitor(Context context, Listener listener) {
        this.appContext = context.getApplicationContext();
        this.listener = listener;
        this.receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE,
                        WifiP2pManager.WIFI_P2P_STATE_DISABLED);
                notifyChange(state == WifiP2pManager.WIFI_P2P_STATE_ENABLED);
            }
        };
    }

    public void register() {
        if (registered) return;
        IntentFilter filter = new IntentFilter(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        appContext.registerReceiver(receiver, filter);
        registered = true;
    }

    public void unregister() {
        if (!registered) return;
        try {
            appContext.unregisterReceiver(receiver);
        } catch (IllegalArgumentException ignored) {
        }
        registered = false;
    }

    private void notifyChange(boolean ready) {
        if (listener != null) {
            listener.onWifiChanged(ready);
        }
    }
}
