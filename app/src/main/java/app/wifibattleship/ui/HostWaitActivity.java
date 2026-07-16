package app.wifibattleship.ui;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.net.ServerSocket;

import app.wifibattleship.GameSession;
import app.wifibattleship.R;
import app.wifibattleship.game.GameConfig;
import app.wifibattleship.game.Role;
import app.wifibattleship.net.GameConnection;
import app.wifibattleship.net.NetUtils;
import app.wifibattleship.net.WifiDirectHelper;

public class HostWaitActivity extends AppCompatActivity {

    public static final String EXTRA_ROLE = "extra_role";

    private TextView tvStatus;
    private TextView tvInfo;
    private boolean accepted = false;
    private boolean destroyed = false;
    private Thread hostThread;
    private WifiDirectHelper p2p;

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

        if (!NetUtils.isWifiEnabled(this)) {
            promptEnableWifi();
            return;
        }

        if (!P2pPermissions.granted(this)) {
            P2pPermissions.request(this);
            return;
        }

        startHosting();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != P2pPermissions.REQUEST_CODE) {
            return;
        }
        if (P2pPermissions.granted(this)) {
            startHosting();
        } else {
            Toast.makeText(this, R.string.err_permission, Toast.LENGTH_LONG).show();
            finish();
        }
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
        hostThread = new Thread(() -> {
            try {
                ServerSocket server;
                try {
                    server = new ServerSocket(GameConfig.SERVICE_PORT);
                } catch (IOException busy) {
                    server = new ServerSocket(0);
                }
                server.setReuseAddress(true);
                final int port = server.getLocalPort();
                GameSession.get().setServerSocket(server);

                String name = NetUtils.generateServiceName();
                GameSession.get().setServiceName(name);

                runOnUiThread(() -> {
                    if (destroyed) return;
                    tvInfo.setText(getString(R.string.host_waiting)
                            + "\nPartida: " + name + "\nPuerto: " + port);
                });

                p2p = GameSession.get().getWifiDirectHelper(this);
                p2p.startHost(port, name, new WifiDirectHelper.HostCallback() {
                    @Override
                    public void onGroupReady(String serviceName) {
                        runOnUiThread(() -> {
                            if (destroyed) return;
                            tvInfo.append("\n" + getString(R.string.host_group_ready));
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
                .setMessage("WiFi Direct necesita el WiFi encendido. ¿Deseas activarlo para crear la partida?")
                .setPositiveButton("Activar WiFi", (d, w) -> {
                    startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                    finish();
                })
                .setNegativeButton("Salir", (d, w) -> finish())
                .setCancelable(false)
                .show();
    }

    @Override
    protected void onDestroy() {
        destroyed = true;
        if (!accepted) {
            GameSession.reset();
        }
        super.onDestroy();
    }
}
