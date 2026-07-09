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
    private NsdManager.ResolveListener currentResolveListener;

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
        try {
            nsdManager.registerService(info, NsdManager.PROTOCOL_DNS_SD, registrationListener);
        } catch (IllegalArgumentException e) {
            callback.onFailed("Registro NSD fallido: " + e.getMessage());
        }
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
                NetUtils.releaseMulticastLock();
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
                NetUtils.releaseMulticastLock();
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
        try {
            nsdManager.discoverServices(GameConfig.SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener);
        } catch (IllegalArgumentException | IllegalStateException e) {
            NetUtils.releaseMulticastLock();
            callback.onFailed("Inicio de búsqueda fallido: " + e.getMessage());
        }
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
        cancelResolve();
        NsdManager.ResolveListener listener = new NsdManager.ResolveListener() {
            @Override
            public void onServiceResolved(NsdServiceInfo info) {
                synchronized (NsdHelper.this) {
                    if (currentResolveListener != this) return;
                    currentResolveListener = null;
                }
                callback.onResolved(info);
            }

            @Override
            public void onResolveFailed(NsdServiceInfo info, int errorCode) {
                synchronized (NsdHelper.this) {
                    if (currentResolveListener != this) return;
                    currentResolveListener = null;
                }
                callback.onFailed("Resolución fallida: " + errorCode);
            }
        };
        synchronized (this) {
            currentResolveListener = listener;
        }
        try {
            nsdManager.resolveService(serviceInfo, listener);
        } catch (IllegalArgumentException e) {
            synchronized (this) {
                currentResolveListener = null;
            }
            callback.onFailed("Resolución fallida: " + e.getMessage());
        }
    }

    public void cancelResolve() {
        NsdManager.ResolveListener listener;
        synchronized (this) {
            listener = currentResolveListener;
            currentResolveListener = null;
        }
    }

    public NsdManager getManager() {
        return nsdManager;
    }
}
