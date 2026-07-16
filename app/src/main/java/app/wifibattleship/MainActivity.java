package app.wifibattleship;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import app.wifibattleship.game.Role;
import app.wifibattleship.net.NetUtils;
import app.wifibattleship.net.WifiStateMonitor;
import app.wifibattleship.ui.ClientDiscoverActivity;
import app.wifibattleship.ui.HostWaitActivity;

public class MainActivity extends AppCompatActivity {

    private TextView tvWifiStatus;
    private Button btnStart;
    private View cardHost;
    private View cardClient;
    private View wifiBanner;
    private WifiStateMonitor wifiMonitor;
    private boolean destroyed = false;
    private boolean roleHost = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvWifiStatus = findViewById(R.id.tvWifiStatus);
        btnStart = findViewById(R.id.btnStart);
        cardHost = findViewById(R.id.cardHost);
        cardClient = findViewById(R.id.cardClient);
        wifiBanner = findViewById(R.id.wifiBanner);

        cardHost.setOnClickListener(v -> selectRole(true));
        cardClient.setOnClickListener(v -> selectRole(false));
        selectRole(true);

        btnStart.setOnClickListener(v -> startGame());

        wifiMonitor = new WifiStateMonitor(this, ready -> {
            if (!destroyed) runOnUiThread(() -> updateWifiStatus(ready));
        });
    }

    private void selectRole(boolean host) {
        roleHost = host;
        cardHost.setSelected(host);
        cardClient.setSelected(!host);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        selectRole(true);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (wifiMonitor != null) wifiMonitor.register();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (wifiMonitor != null) wifiMonitor.unregister();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateWifiStatus(NetUtils.isWifiEnabled(this));
    }

    @Override
    protected void onDestroy() {
        destroyed = true;
        super.onDestroy();
    }

    private void updateWifiStatus(boolean ready) {
        if (ready) {
            tvWifiStatus.setText(R.string.wifi_ok_title);
            tvWifiStatus.setTextColor(ContextCompat.getColor(this, R.color.ok_green_dark));
            wifiBanner.setBackgroundResource(R.drawable.bg_status_card_ok);
            wifiBanner.setOnClickListener(null);
            wifiBanner.setClickable(false);
            btnStart.setEnabled(true);
        } else {
            tvWifiStatus.setText(R.string.wifi_off_title);
            tvWifiStatus.setTextColor(ContextCompat.getColor(this, R.color.err_text));
            wifiBanner.setBackgroundResource(R.drawable.bg_status_card_err);
            wifiBanner.setOnClickListener(v -> promptEnableWifi());
            btnStart.setEnabled(false);
        }
    }

    private void promptEnableWifi() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("WiFi desactivado")
                .setMessage("WiFi Direct necesita el WiFi encendido. ¿Deseas activarlo para continuar?")
                .setPositiveButton("Activar WiFi", (d, w) ->
                        startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS)))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void startGame() {
        if (!NetUtils.isWifiEnabled(this)) {
            Toast.makeText(this, R.string.err_wifi_off, Toast.LENGTH_SHORT).show();
            return;
        }
        boolean isHost = roleHost;
        Role role = isHost ? Role.HOST : Role.CLIENT;
        Intent intent;
        if (isHost) {
            intent = new Intent(this, HostWaitActivity.class);
        } else {
            intent = new Intent(this, ClientDiscoverActivity.class);
        }
        intent.putExtra(HostWaitActivity.EXTRA_ROLE, role.name());
        startActivity(intent);
    }
}
