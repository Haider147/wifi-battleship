package app.wifibattleship.ui;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.net.ServerSocket;
import java.util.concurrent.atomic.AtomicBoolean;

import app.wifibattleship.GameSession;
import app.wifibattleship.R;
import app.wifibattleship.game.Role;
import app.wifibattleship.net.GameConnection;
import app.wifibattleship.net.NetUtils;
import app.wifibattleship.net.NsdHelper;

public class HostWaitActivity extends AppCompatActivity {

    public static final String EXTRA_ROLE = "extra_role";

    private TextView tvStatus;
    private TextView tvInfo;
    private boolean accepted = false;
    private boolean destroyed = false;
    private Thread hostThread;
    private NsdHelper nsd;
    private final AtomicBoolean hostStarted = new AtomicBoolean(false);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_host_wait);

        tvStatus = findViewById(R.id.tvStatus);
        tvInfo = findViewById(R.id.tvInfo);
        Button btnCancel = findViewById(R.id.btnCancel);
        btnCancel.setOnClickListener(v -> finish());

        GameSession.reset();
        Role role = readRole();
        GameSession.get().setRole(role);

        if (!NetUtils.isWifiReady(this)) {
            promptEnableWifi();
            return;
        }

        startHosting();
    }

    private Role readRole() {
        String name = getIntent().getStringExtra(EXTRA_ROLE);
        if (name == null) {
            return Role.HOST;
        }
        try {
            return Role.valueOf(name);
        } catch (IllegalArgumentException e) {
            return Role.HOST;
        }
    }

    private void startHosting() {
        hostStarted.set(true);
        hostThread = new Thread(() -> {
            try {
                int port = NetUtils.bindFreePort();
                ServerSocket server = new ServerSocket(port);
                server.setReuseAddress(true);
                GameSession.get().setServerSocket(server);

                String ip = NetUtils.getWifiIpAddress(this);
                String name = NetUtils.generateServiceName();
                GameSession.get().setServiceName(name);

                runOnUiThread(() -> {
                    if (destroyed) return;
                    tvInfo.setText(
                            getString(R.string.host_waiting) + "\nIP: " + ip + "\nPuerto: " + port);
                });

                nsd = GameSession.get().getNsdHelper(this);
                nsd.registerService(port, name, new NsdHelper.RegistrationCallback() {
                    @Override
                    public void onRegistered(String serviceName) {
                        runOnUiThread(() -> {
                            if (destroyed) return;
                            tvInfo.append("\nServicio: " + serviceName);
                        });
                    }

                    @Override
                    public void onFailed(String reason) {
                        runOnUiThread(() -> {
                            if (destroyed) return;
                            tvStatus.setText(reason);
                        });
                    }
                });

                GameConnection.acceptAsHost(server, new GameConnection.ConnectCallback() {
                    @Override
                    public void onConnected(GameConnection connection) {
                        if (destroyed) {
                            connection.close();
                            return;
                        }
                        if (accepted) {
                            connection.close();
                            return;
                        }
                        accepted = true;
                        GameSession.get().setConnection(connection);
                        GameSession.get().getController();
                        connection.start();
                        runOnUiThread(() -> {
                            if (destroyed) {
                                connection.close();
                                return;
                            }
                            tvStatus.setText(R.string.status_connected);
                            tvInfo.append("\nCliente conectado.");
                            goToPlacement();
                        });
                    }

                    @Override
                    public void onFailed(String reason) {
                        runOnUiThread(() -> {
                            if (destroyed) return;
                            tvStatus.setText(getString(R.string.err_connection) + "\n" + reason);
                        });
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (destroyed) return;
                    tvStatus.setText(getString(R.string.err_connection) + "\n" + e.getMessage());
                });
            }
        }, "wbs-host");
        hostThread.setDaemon(true);
        hostThread.start();
    }

    private void goToPlacement() {
        Intent intent = new Intent(this, PlacementActivity.class);
        startActivity(intent);
        finish();
    }

    private void promptEnableWifi() {
        new AlertDialog.Builder(this)
                .setTitle("WiFi desactivado")
                .setMessage("El WiFi se ha desactivado. ¿Deseas activarlo para crear la partida?")
                .setPositiveButton("Activar WiFi", (d, w) -> {
                    startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                    finish();
                })
                .setNegativeButton("Salir", (d, w) -> finish())
                .setCancelable(false)
                .show();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (nsd != null && !accepted) {
            nsd.unregisterService();
        }
    }

    @Override
    protected void onDestroy() {
        destroyed = true;
        if (!accepted) {
            if (nsd != null) {
                nsd.unregisterService();
            }
            GameSession.reset();
        }
        super.onDestroy();
    }
}
