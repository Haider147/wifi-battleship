package app.wifibattleship.ui;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

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
    private TextView[] stepIcons;
    private TextView[] stepTitles;
    private ProgressBar[] stepSpins;
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
        stepIcons = new TextView[]{findViewById(R.id.tvIcon1),
                findViewById(R.id.tvIcon2), findViewById(R.id.tvIcon3)};
        stepTitles = new TextView[]{findViewById(R.id.tvStep1),
                findViewById(R.id.tvStep2), findViewById(R.id.tvStep3)};
        stepSpins = new ProgressBar[]{findViewById(R.id.pbStep1),
                findViewById(R.id.pbStep2), findViewById(R.id.pbStep3)};
        Button btnCancel = findViewById(R.id.btnCancel);
        btnCancel.setOnClickListener(v -> finish());

        stepActive(0);

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
                    tvInfo.setText("Partida: " + name + " — Puerto: " + port);
                });

                p2p = GameSession.get().getWifiDirectHelper(this);
                p2p.startHost(port, name, new WifiDirectHelper.HostCallback() {
                    @Override
                    public void onGroupReady(String serviceName) {
                        runOnUiThread(() -> {
                            if (destroyed) return;
                            stepDone(0);
                            stepDone(1);
                            stepActive(2);
                        });
                    }

                    @Override
                    public void onFailed(String reason) {
                        runOnUiThread(() -> {
                            if (destroyed) return;
                            showError(reason);
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
                            stepDone(2);
                            goToPlacement();
                        });
                    }

                    @Override
                    public void onFailed(String reason) {
                        runOnUiThread(() -> {
                            if (destroyed) return;
                            showError(getString(R.string.err_connection) + "\n" + reason);
                        });
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (destroyed) return;
                    showError(getString(R.string.err_connection) + "\n" + e.getMessage());
                });
            }
        }, "wbs-host");
        hostThread.setDaemon(true);
        hostThread.start();
    }

    /** Paso en curso: oculta el número y muestra el indicador giratorio. */
    private void stepActive(int i) {
        stepIcons[i].setBackgroundResource(R.drawable.bg_step_active);
        stepIcons[i].setText("");
        stepSpins[i].setVisibility(View.VISIBLE);
        stepTitles[i].setTextColor(ContextCompat.getColor(this, R.color.navy));
    }

    /** Paso completado: círculo verde con su número. */
    private void stepDone(int i) {
        stepSpins[i].setVisibility(View.GONE);
        stepIcons[i].setBackgroundResource(R.drawable.bg_step_done);
        stepIcons[i].setText(String.valueOf(i + 1));
        stepTitles[i].setTextColor(ContextCompat.getColor(this, R.color.ok_green_dark));
    }

    private void showError(String message) {
        for (ProgressBar spin : stepSpins) {
            spin.setVisibility(View.GONE);
        }
        tvStatus.setText(message);
        tvStatus.setVisibility(View.VISIBLE);
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
