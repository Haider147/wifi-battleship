package app.wifibattleship.net;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.Looper;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import app.wifibattleship.game.GameConfig;

/**
 * Capa de descubrimiento y conexión por WiFi Direct (WiFi P2P).
 * El Host crea el grupo (group owner) y anuncia la partida por DNS-SD sobre P2P;
 * el Cliente descubre partidas, se une al grupo y obtiene la IP del group owner
 * para abrir el socket TCP de GameConnection.
 *
 * Las llamadas a WifiP2pManager exigen el permiso NEARBY_WIFI_DEVICES (Android 13+)
 * o ACCESS_FINE_LOCATION (Android 8-12); las Activities lo piden antes de llamar.
 */
@SuppressLint("MissingPermission")
public class WifiDirectHelper {

    public interface HostCallback {
        void onGroupReady(String serviceName);

        void onFailed(String reason);
    }

    public interface DiscoveryCallback {
        void onDiscoveryStarted();

        void onGameFound(DiscoveredGame game);

        void onFailed(String reason);
    }

    public interface ConnectCallback {
        void onHostFound(String hostAddress);

        void onFailed(String reason);
    }

    private static final String TAG = "WbsWifiDirect";

    private final Context context;
    private final WifiP2pManager manager;
    private final WifiP2pManager.Channel channel;

    private final Map<String, Integer> txtPorts = new ConcurrentHashMap<>();
    private volatile DiscoveryCallback discoveryCallback;
    private volatile ConnectCallback pendingConnect;
    private boolean receiverRegistered = false;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ctx, Intent intent) {
            if (!WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(intent.getAction())) {
                return;
            }
            NetworkInfo info = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
            if (info != null && info.isConnected()) {
                requestGroupOwnerAddress();
            }
        }
    };

    public WifiDirectHelper(Context context) {
        this.context = context.getApplicationContext();
        this.manager = (WifiP2pManager) this.context.getSystemService(Context.WIFI_P2P_SERVICE);
        WifiP2pManager.Channel ch = null;
        if (manager != null) {
            ch = manager.initialize(this.context, Looper.getMainLooper(), null);
        }
        this.channel = ch;
        registerReceiver();
    }

    public boolean isAvailable() {
        return manager != null && channel != null;
    }

    // ---------------------------------------------------------------- Host

    /**
     * Crea el grupo P2P (el Host queda como group owner) y anuncia la partida
     * por DNS-SD con el puerto TCP en el registro TXT.
     */
    public void startHost(final int port, final String serviceName, final HostCallback callback) {
        if (!isAvailable()) {
            callback.onFailed("WiFi Direct no disponible en este dispositivo.");
            return;
        }
        // removeGroup preventivo: limpia grupos zombis de partidas anteriores.
        manager.removeGroup(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                createGroup(port, serviceName, callback);
            }

            @Override
            public void onFailure(int reason) {
                createGroup(port, serviceName, callback);
            }
        });
    }

    private void createGroup(final int port, final String serviceName, final HostCallback callback) {
        manager.createGroup(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                registerLocalService(port, serviceName, callback);
            }

            @Override
            public void onFailure(int reason) {
                if (reason == WifiP2pManager.BUSY) {
                    // Puede haber un grupo previo del sistema aún vivo: se anuncia igual.
                    registerLocalService(port, serviceName, callback);
                } else {
                    callback.onFailed("No se pudo crear el grupo WiFi Direct: " + reasonText(reason));
                }
            }
        });
    }

    private void registerLocalService(int port, final String serviceName, final HostCallback callback) {
        Map<String, String> txt = new HashMap<>();
        txt.put(GameConfig.TXT_KEY_PORT, String.valueOf(port));
        final WifiP2pDnsSdServiceInfo info = WifiP2pDnsSdServiceInfo.newInstance(
                serviceName, GameConfig.SERVICE_TYPE, txt);

        manager.clearLocalServices(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                addLocalService(info, serviceName, callback);
            }

            @Override
            public void onFailure(int reason) {
                addLocalService(info, serviceName, callback);
            }
        });
    }

    private void addLocalService(WifiP2pDnsSdServiceInfo info, final String serviceName,
                                 final HostCallback callback) {
        manager.addLocalService(channel, info, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                callback.onGroupReady(serviceName);
            }

            @Override
            public void onFailure(int reason) {
                callback.onFailed("No se pudo anunciar la partida: " + reasonText(reason));
            }
        });
    }

    // ------------------------------------------------------------- Cliente

    /** Busca partidas anunciadas por DNS-SD sobre WiFi Direct. */
    public void discoverGames(final DiscoveryCallback callback) {
        if (!isAvailable()) {
            callback.onFailed("WiFi Direct no disponible en este dispositivo.");
            return;
        }
        discoveryCallback = callback;
        txtPorts.clear();

        manager.setDnsSdResponseListeners(channel,
                (instanceName, registrationType, srcDevice) -> {
                    if (instanceName == null
                            || !instanceName.startsWith(GameConfig.SERVICE_PREFIX)) {
                        return;
                    }
                    Integer port = txtPorts.get(srcDevice.deviceAddress);
                    DiscoveredGame game = new DiscoveredGame(instanceName, srcDevice,
                            port != null ? port : GameConfig.SERVICE_PORT);
                    DiscoveryCallback cb = discoveryCallback;
                    if (cb != null) {
                        cb.onGameFound(game);
                    }
                },
                (fullDomainName, txtRecordMap, srcDevice) -> {
                    if (txtRecordMap == null) {
                        return;
                    }
                    String p = txtRecordMap.get(GameConfig.TXT_KEY_PORT);
                    if (p != null) {
                        try {
                            txtPorts.put(srcDevice.deviceAddress, Integer.parseInt(p));
                        } catch (NumberFormatException ignored) {
                        }
                    }
                });

        manager.clearServiceRequests(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                addServiceRequest(callback);
            }

            @Override
            public void onFailure(int reason) {
                addServiceRequest(callback);
            }
        });
    }

    private void addServiceRequest(final DiscoveryCallback callback) {
        manager.addServiceRequest(channel, WifiP2pDnsSdServiceRequest.newInstance(),
                new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        startServiceDiscovery(callback);
                    }

                    @Override
                    public void onFailure(int reason) {
                        callback.onFailed("Inicio de búsqueda fallido: " + reasonText(reason));
                    }
                });
    }

    private void startServiceDiscovery(final DiscoveryCallback callback) {
        manager.discoverServices(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                callback.onDiscoveryStarted();
            }

            @Override
            public void onFailure(int reason) {
                callback.onFailed("Inicio de búsqueda fallido: " + reasonText(reason));
            }
        });
    }

    /**
     * Se une al grupo del host elegido. Cuando el sistema forma el grupo,
     * el broadcast CONNECTION_CHANGED entrega la IP del group owner.
     */
    public void connectTo(DiscoveredGame game, final ConnectCallback callback) {
        if (!isAvailable()) {
            callback.onFailed("WiFi Direct no disponible en este dispositivo.");
            return;
        }
        pendingConnect = callback;

        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = game.getDeviceAddress();
        config.wps.setup = WpsInfo.PBC;
        config.groupOwnerIntent = 0; // el host debe quedar como group owner

        manager.connect(channel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                // Solo indica que la negociación arrancó; la IP llega por broadcast.
                requestGroupOwnerAddress();
            }

            @Override
            public void onFailure(int reason) {
                pendingConnect = null;
                callback.onFailed("Conexión WiFi Direct fallida: " + reasonText(reason));
            }
        });
    }

    private void requestGroupOwnerAddress() {
        if (!isAvailable() || pendingConnect == null) {
            return;
        }
        manager.requestConnectionInfo(channel, info -> {
            ConnectCallback cb = pendingConnect;
            if (cb == null || info == null || !info.groupFormed) {
                return;
            }
            if (info.isGroupOwner) {
                // Este dispositivo quedó como group owner: no es el caso cliente.
                return;
            }
            if (info.groupOwnerAddress == null) {
                return;
            }
            pendingConnect = null;
            cb.onHostFound(info.groupOwnerAddress.getHostAddress());
        });
    }

    public void cancelConnect() {
        pendingConnect = null;
        if (isAvailable()) {
            manager.cancelConnect(channel, null);
        }
    }

    public void stopDiscovery() {
        discoveryCallback = null;
        if (isAvailable()) {
            manager.clearServiceRequests(channel, null);
        }
    }

    // -------------------------------------------------------------- Limpieza

    /** Desmonta todo: servicios, búsquedas, conexión pendiente y grupo P2P. */
    public void teardown() {
        discoveryCallback = null;
        pendingConnect = null;
        if (isAvailable()) {
            try {
                manager.clearServiceRequests(channel, null);
                manager.clearLocalServices(channel, null);
                manager.stopPeerDiscovery(channel, null);
                manager.cancelConnect(channel, null);
                manager.removeGroup(channel, null);
            } catch (Exception e) {
                Log.w(TAG, "teardown", e);
            }
        }
        unregisterReceiver();
    }

    private void registerReceiver() {
        if (receiverRegistered) {
            return;
        }
        IntentFilter filter = new IntentFilter(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        context.registerReceiver(receiver, filter);
        receiverRegistered = true;
    }

    private void unregisterReceiver() {
        if (!receiverRegistered) {
            return;
        }
        try {
            context.unregisterReceiver(receiver);
        } catch (IllegalArgumentException ignored) {
        }
        receiverRegistered = false;
    }

    private static String reasonText(int reason) {
        switch (reason) {
            case WifiP2pManager.P2P_UNSUPPORTED:
                return "WiFi Direct no soportado";
            case WifiP2pManager.BUSY:
                return "sistema ocupado, reintenta";
            case WifiP2pManager.ERROR:
            default:
                return "error interno (" + reason + ")";
        }
    }
}
