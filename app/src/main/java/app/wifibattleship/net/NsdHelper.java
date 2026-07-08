package app.wifibattleship.net;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;

import app.wifibattleship.game.GameConfig;

public class NsdHelper {

    public interface RegistrationCallback {
        void onRegistered(String serviceName);

        void onFailed(String reason);
    }

    public interface DiscoveryCallback {
        void onDiscoveryStarted();

        void onDiscoveryStopped();

        void onServiceFound(NsdServiceInfo serviceInfo);

        void onServiceLost(NsdServiceInfo serviceInfo);

        void onFailed(String reason);
    }

    public interface ResolveCallback {
        void onResolved(NsdServiceInfo serviceInfo);

        void onFailed(String reason);
    }

    private final NsdManager nsdManager;
    private final Context context;
    private NsdManager.RegistrationListener registrationListener;
    private NsdManager.DiscoveryListener discoveryListener;

    public NsdHelper(Context context) {
        this.context = context.getApplicationContext();
        this.nsdManager = (NsdManager) this.context.getSystemService(Context.NSD_SERVICE);
    }

    public void registerService(int port, String serviceName, RegistrationCallback callback) {
        NsdServiceInfo info = new NsdServiceInfo();
        info.setServiceName(serviceName);
        info.setServiceType(GameConfig.SERVICE_TYPE);
        info.setPort(port);

        registrationListener = new NsdManager.RegistrationListener() {
            @Override
            public void onServiceRegistered(NsdServiceInfo serviceInfo) {
                callback.onRegistered(serviceInfo.getServiceName());
            }

            @Override
            public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                callback.onFailed("Registro NSD fallido: " + errorCode);
            }

            @Override
            public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo serviceInfo) {
            }
        };
        nsdManager.registerService(info, NsdManager.PROTOCOL_DNS_SD, registrationListener);
    }

    public void unregisterService() {
        if (registrationListener != null) {
            try {
                nsdManager.unregisterService(registrationListener);
            } catch (IllegalArgumentException ignored) {
            }
            registrationListener = null;
        }
    }

    public void discoverServices(DiscoveryCallback callback) {
        NetUtils.acquireMulticastLock(context);
        discoveryListener = new NsdManager.DiscoveryListener() {
            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                callback.onFailed("Inicio de búsqueda fallido: " + errorCode);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
            }

            @Override
            public void onDiscoveryStarted(String serviceType) {
                callback.onDiscoveryStarted();
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                callback.onDiscoveryStopped();
            }

            @Override
            public void onServiceFound(NsdServiceInfo serviceInfo) {
                callback.onServiceFound(serviceInfo);
            }

            @Override
            public void onServiceLost(NsdServiceInfo serviceInfo) {
                callback.onServiceLost(serviceInfo);
            }
        };
        nsdManager.discoverServices(GameConfig.SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener);
    }

    public void stopDiscovery() {
        if (discoveryListener != null) {
            try {
                nsdManager.stopServiceDiscovery(discoveryListener);
            } catch (IllegalArgumentException ignored) {
            }
            discoveryListener = null;
        }
        NetUtils.releaseMulticastLock();
    }

    public void resolveService(NsdServiceInfo serviceInfo, ResolveCallback callback) {
        NsdManager.ResolveListener listener = new NsdManager.ResolveListener() {
            @Override
            public void onServiceResolved(NsdServiceInfo info) {
                callback.onResolved(info);
            }

            @Override
            public void onResolveFailed(NsdServiceInfo info, int errorCode) {
                callback.onFailed("Resolución fallida: " + errorCode);
            }
        };
        nsdManager.resolveService(serviceInfo, listener);
    }

    public NsdManager getManager() {
        return nsdManager;
    }
}
