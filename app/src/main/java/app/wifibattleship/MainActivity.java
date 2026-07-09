package app.wifibattleship;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import app.wifibattleship.game.Role;
import app.wifibattleship.net.NetUtils;
import app.wifibattleship.net.WifiStateMonitor;
import app.wifibattleship.ui.ClientDiscoverActivity;
import app.wifibattleship.ui.HostWaitActivity;

public class MainActivity extends AppCompatActivity {

    private TextView tvWifiStatus;
    private Button btnStart;
    private RadioGroup rgRole;
    private View wifiBanner;
    private WifiStateMonitor wifiMonitor;
    private boolean destroyed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvWifiStatus = findViewById(R.id.tvWifiStatus);
        btnStart = findViewById(R.id.btnStart);
        rgRole = findViewById(R.id.rgRole);
        wifiBanner = findViewById(R.id.wifiBanner);

        btnStart.setOnClickListener(v -> startGame());

        wifiMonitor = new WifiStateMonitor(this, ready -> {
            if (!destroyed) runOnUiThread(() -> updateWifiStatus(ready));
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        rgRole.check(R.id.rbHost);
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
        updateWifiStatus(NetUtils.isWifiReady(this));
    }

    @Override
    protected void onDestroy() {
        destroyed = true;
        super.onDestroy();
    }

    private void updateWifiStatus(boolean ready) {
        if (ready) {
            tvWifiStatus.setText(R.string.status_connected);
            wifiBanner.setBackgroundResource(R.drawable.bg_status_connected);
            wifiBanner.setOnClickListener(null);
            btnStart.setEnabled(true);
        } else {
            tvWifiStatus.setText(R.string.err_wifi_off);
            wifiBanner.setBackgroundResource(R.drawable.bg_status_disconnected);
            wifiBanner.setOnClickListener(v -> promptEnableWifi());
            btnStart.setEnabled(false);
        }
    }

    private void promptEnableWifi() {
        new AlertDialog.Builder(this)
                .setTitle("WiFi desactivado")
                .setMessage("El WiFi no está activado. ¿Deseas activarlo para continuar?")
                .setPositiveButton("Activar WiFi", (d, w) ->
                        startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS)))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void startGame() {
        if (!NetUtils.isWifiReady(this)) {
            Toast.makeText(this, R.string.err_wifi_off, Toast.LENGTH_SHORT).show();
            return;
        }
        int checkedId = rgRole.getCheckedRadioButtonId();
        if (checkedId == -1) {
            Toast.makeText(this, "Selecciona un rol.", Toast.LENGTH_SHORT).show();
            return;
        }
        boolean isHost = checkedId == R.id.rbHost;
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
