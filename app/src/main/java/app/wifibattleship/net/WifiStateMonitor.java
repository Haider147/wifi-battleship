package app.wifibattleship.net;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;

import androidx.annotation.NonNull;

public final class WifiStateMonitor {

    public interface Listener {
        void onWifiChanged(boolean ready);
    }

    private final Context appContext;
    private final Listener listener;
    private final ConnectivityManager.NetworkCallback callback;

    public WifiStateMonitor(Context context, Listener listener) {
        this.appContext = context.getApplicationContext();
        this.listener = listener;
        this.callback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                notifyChange();
            }

            @Override
            public void onLost(@NonNull Network network) {
                notifyChange();
            }

            @Override
            public void onCapabilitiesChanged(@NonNull Network network,
                                               @NonNull NetworkCapabilities caps) {
                notifyChange();
            }
        };
    }

    public void register() {
        ConnectivityManager cm = (ConnectivityManager) appContext
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return;
        NetworkRequest req = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build();
        try {
            cm.registerNetworkCallback(req, callback);
        } catch (IllegalArgumentException ignored) {
        }
    }

    public void unregister() {
        ConnectivityManager cm = (ConnectivityManager) appContext
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return;
        try {
            cm.unregisterNetworkCallback(callback);
        } catch (IllegalArgumentException ignored) {
        }
    }

    private void notifyChange() {
        if (listener != null) {
            listener.onWifiChanged(NetUtils.isWifiReady(appContext));
        }
    }
}
