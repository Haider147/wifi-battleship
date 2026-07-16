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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import app.wifibattleship.GameSession;
import app.wifibattleship.R;
import app.wifibattleship.game.Role;
import app.wifibattleship.net.DiscoveredGame;
import app.wifibattleship.net.GameConnection;
import app.wifibattleship.net.NetUtils;
import app.wifibattleship.net.WifiDirectHelper;

public class ClientDiscoverActivity extends AppCompatActivity {

    public static final String EXTRA_ROLE = "extra_role";

    private TextView tvStatus;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private Button btnRetry;
    private DiscoveredGameAdapter adapter;
    private boolean connecting = false;
    private boolean connected = false;
    private boolean destroyed = false;
    private WifiDirectHelper p2p;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_discover);

        tvStatus = findViewById(R.id.tvStatus);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);
        btnRetry = findViewById(R.id.btnRetry);
        Button btnCancel = findViewById(R.id.btnCancel);

        RecyclerView rv = findViewById(R.id.rvGames);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DiscoveredGameAdapter(this::onGameSelected);
        rv.setAdapter(adapter);

        btnRetry.setOnClickListener(v -> startDiscovery());
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

        startDiscoveryChecked();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != P2pPermissions.REQUEST_CODE) {
            return;
        }
        if (P2pPermissions.granted(this)) {
            startDiscoveryChecked();
        } else {
            Toast.makeText(this, R.string.err_permission, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private Role readRole() {
        String name = getIntent().getStringExtra(EXTRA_ROLE);
        if (name == null) {
            return Role.CLIENT;
        }
        try {
            return Role.valueOf(name);
        } catch (IllegalArgumentException e) {
            return Role.CLIENT;
        }
    }

    /** En Android 8-12 el descubrimiento P2P exige la ubicación del sistema activa. */
    private void startDiscoveryChecked() {
        if (P2pPermissions.locationRequiredAndOff(this)) {
            promptEnableLocation();
            return;
        }
        startDiscovery();
    }

    private void startDiscovery() {
        if (destroyed || connecting || connected) {
            return;
        }
        adapter.clear();
        updateEmpty();
        progressBar.setVisibility(View.VISIBLE);
        tvStatus.setText(R.string.client_searching);
        btnRetry.setEnabled(false);

        p2p = GameSession.get().getWifiDirectHelper(this);
        p2p.discoverGames(new WifiDirectHelper.DiscoveryCallback() {
            @Override
            public void onDiscoveryStarted() {
                if (destroyed) return;
                btnRetry.setEnabled(true);
            }

            @Override
            public void onGameFound(DiscoveredGame game) {
                if (destroyed) return;
                adapter.add(game);
                updateEmpty();
            }

            @Override
            public void onFailed(String reason) {
                if (destroyed) return;
                progressBar.setVisibility(View.GONE);
                tvStatus.setText(getString(R.string.err_connection) + "\n" + reason);
                btnRetry.setEnabled(true);
            }
        });
    }

    private void updateEmpty() {
        tvEmpty.setVisibility(adapter.isEmpty() ? View.VISIBLE : View.GONE);
        progressBar.setVisibility(adapter.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void onGameSelected(DiscoveredGame game) {
        if (destroyed) return;
        if (connecting || connected) {
            return;
        }
        connecting = true;
        tvStatus.setText(R.string.status_connecting);
        progressBar.setVisibility(View.VISIBLE);

        p2p.connectTo(game, new WifiDirectHelper.ConnectCallback() {
            @Override
            public void onHostFound(String hostAddress) {
                if (destroyed) return;
                GameConnection.connectAsClient(hostAddress, game.getPort(),
                        new GameConnection.ConnectCallback() {
                            @Override
                            public void onConnected(GameConnection connection) {
                                if (destroyed) {
                                    connection.close();
                                    return;
                                }
                                connected = true;
                                GameSession.get().setConnection(connection);
                                GameSession.get().getController();
                                connection.start();
                                runOnUiThread(() -> {
                                    if (destroyed) {
                                        connection.close();
                                        return;
                                    }
                                    tvStatus.setText(R.string.status_connected);
                                    if (p2p != null) p2p.stopDiscovery();
                                    goToPlacement();
                                });
                            }

                            @Override
                            public void onFailed(String reason) {
                                connecting = false;
                                runOnUiThread(() -> {
                                    if (destroyed) return;
                                    progressBar.setVisibility(View.GONE);
                                    tvStatus.setText(getString(R.string.err_connection)
                                            + "\n" + reason);
                                    Toast.makeText(ClientDiscoverActivity.this,
                                            R.string.err_connection, Toast.LENGTH_SHORT).show();
                                });
                            }
                        });
            }

            @Override
            public void onFailed(String reason) {
                connecting = false;
                if (destroyed) return;
                progressBar.setVisibility(View.GONE);
                tvStatus.setText(getString(R.string.err_connection) + "\n" + reason);
                Toast.makeText(ClientDiscoverActivity.this,
                        R.string.err_connection, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void goToPlacement() {
        Intent intent = new Intent(this, PlacementActivity.class);
        startActivity(intent);
        finish();
    }

    private void promptEnableWifi() {
        new AlertDialog.Builder(this)
                .setTitle("WiFi desactivado")
                .setMessage("WiFi Direct necesita el WiFi encendido. ¿Deseas activarlo para buscar partidas?")
                .setPositiveButton("Activar WiFi", (d, w) -> {
                    startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                    finish();
                })
                .setNegativeButton("Salir", (d, w) -> finish())
                .setCancelable(false)
                .show();
    }

    private void promptEnableLocation() {
        new AlertDialog.Builder(this)
                .setTitle("Ubicación desactivada")
                .setMessage(getString(R.string.err_location_off))
                .setPositiveButton("Activar ubicación", (d, w) -> {
                    startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    finish();
                })
                .setNegativeButton("Salir", (d, w) -> finish())
                .setCancelable(false)
                .show();
    }

    @Override
    protected void onDestroy() {
        destroyed = true;
        if (!connected) {
            if (p2p != null) {
                p2p.cancelConnect();
                p2p.stopDiscovery();
            }
            GameSession.reset();
        }
        super.onDestroy();
    }
}
