package app.wifibattleship.ui;

import android.content.Intent;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import app.wifibattleship.GameSession;
import app.wifibattleship.R;
import app.wifibattleship.game.Role;
import app.wifibattleship.net.GameConnection;
import app.wifibattleship.net.NsdHelper;

public class ClientDiscoverActivity extends AppCompatActivity {

    public static final String EXTRA_ROLE = "extra_role";

    private TextView tvStatus;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private Button btnRetry;
    private DiscoveredGameAdapter adapter;
    private boolean connecting = false;
    private boolean connected = false;

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

        startDiscovery();
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

    private void startDiscovery() {
        adapter.clear();
        updateEmpty();
        progressBar.setVisibility(View.VISIBLE);
        tvStatus.setText(R.string.client_searching);
        btnRetry.setEnabled(false);

        NsdHelper nsd = GameSession.get().getNsdHelper(this);
        nsd.stopDiscovery();
        nsd.discoverServices(new NsdHelper.DiscoveryCallback() {
            @Override
            public void onDiscoveryStarted() {
                runOnUiThread(() -> btnRetry.setEnabled(true));
            }

            @Override
            public void onDiscoveryStopped() {
            }

            @Override
            public void onServiceFound(NsdServiceInfo serviceInfo) {
                runOnUiThread(() -> {
                    adapter.add(serviceInfo);
                    updateEmpty();
                });
            }

            @Override
            public void onServiceLost(NsdServiceInfo serviceInfo) {
                runOnUiThread(() -> {
                    adapter.remove(serviceInfo);
                    updateEmpty();
                });
            }

            @Override
            public void onFailed(String reason) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    tvStatus.setText(getString(R.string.err_connection) + "\n" + reason);
                    btnRetry.setEnabled(true);
                });
            }
        });
    }

    private void updateEmpty() {
        tvEmpty.setVisibility(adapter.isEmpty() ? View.VISIBLE : View.GONE);
        progressBar.setVisibility(adapter.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void onGameSelected(NsdServiceInfo info) {
        if (connecting || connected) {
            return;
        }
        connecting = true;
        tvStatus.setText(R.string.status_connecting);
        progressBar.setVisibility(View.VISIBLE);

        NsdHelper nsd = GameSession.get().getNsdHelper(this);
        nsd.resolveService(info, new NsdHelper.ResolveCallback() {
            @Override
            public void onResolved(NsdServiceInfo resolved) {
                String host = resolved.getHost().getHostAddress();
                int port = resolved.getPort();
                GameConnection.connectAsClient(host, port, new GameConnection.ConnectCallback() {
                    @Override
                    public void onConnected(GameConnection connection) {
                        connected = true;
                        GameSession.get().setConnection(connection);
                        connection.start();
                        runOnUiThread(() -> {
                            tvStatus.setText(R.string.status_connected);
                            nsd.stopDiscovery();
                            goToPlacement();
                        });
                    }

                    @Override
                    public void onFailed(String reason) {
                        connecting = false;
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            tvStatus.setText(getString(R.string.err_connection) + "\n" + reason);
                            Toast.makeText(ClientDiscoverActivity.this,
                                    R.string.err_connection, Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            }

            @Override
            public void onFailed(String reason) {
                connecting = false;
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    tvStatus.setText(getString(R.string.err_connection) + "\n" + reason);
                    Toast.makeText(ClientDiscoverActivity.this,
                            R.string.err_connection, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void goToPlacement() {
        Intent intent = new Intent(this, PlacementActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!connected) {
            NsdHelper nsd = GameSession.get().getNsdHelper(this);
            nsd.stopDiscovery();
            if (!connected) {
                GameSession.reset();
            }
        }
    }
}
