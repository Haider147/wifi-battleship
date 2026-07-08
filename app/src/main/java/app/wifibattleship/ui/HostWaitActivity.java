package app.wifibattleship.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.net.ServerSocket;

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
        new Thread(() -> {
            try {
                int port = NetUtils.bindFreePort();
                ServerSocket server = new ServerSocket(port);
                server.setReuseAddress(true);
                GameSession.get().setServerSocket(server);

                String ip = NetUtils.getWifiIpAddress(this);
                String name = NetUtils.generateServiceName();
                GameSession.get().setServiceName(name);

                runOnUiThread(() -> tvInfo.setText(
                        getString(R.string.host_waiting) + "\nIP: " + ip + "\nPuerto: " + port));

                NsdHelper nsd = GameSession.get().getNsdHelper(this);
                nsd.registerService(port, name, new NsdHelper.RegistrationCallback() {
                    @Override
                    public void onRegistered(String serviceName) {
                        runOnUiThread(() -> tvInfo.append("\nServicio: " + serviceName));
                    }

                    @Override
                    public void onFailed(String reason) {
                        runOnUiThread(() -> tvStatus.setText(reason));
                    }
                });

                GameConnection.acceptAsHost(server, new GameConnection.ConnectCallback() {
                    @Override
                    public void onConnected(GameConnection connection) {
                        if (accepted) {
                            connection.close();
                            return;
                        }
                        accepted = true;
                        GameSession.get().setConnection(connection);
                        connection.start();
                        runOnUiThread(() -> {
                            tvStatus.setText(R.string.status_connected);
                            tvInfo.append("\nCliente conectado.");
                            goToPlacement();
                        });
                    }

                    @Override
                    public void onFailed(String reason) {
                        runOnUiThread(() -> tvStatus.setText(getString(R.string.err_connection) + "\n" + reason));
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> tvStatus.setText(getString(R.string.err_connection) + "\n" + e.getMessage()));
            }
        }, "wbs-host").start();
    }

    private void goToPlacement() {
        Intent intent = new Intent(this, PlacementActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!accepted) {
            GameSession.reset();
        }
    }
}
