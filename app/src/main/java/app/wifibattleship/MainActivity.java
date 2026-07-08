package app.wifibattleship;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import app.wifibattleship.game.Role;
import app.wifibattleship.net.NetUtils;
import app.wifibattleship.ui.ClientDiscoverActivity;
import app.wifibattleship.ui.HostWaitActivity;

public class MainActivity extends AppCompatActivity {

    private TextView tvWifiStatus;
    private Button btnStart;
    private RadioGroup rgRole;
    private View wifiBanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvWifiStatus = findViewById(R.id.tvWifiStatus);
        btnStart = findViewById(R.id.btnStart);
        rgRole = findViewById(R.id.rgRole);
        wifiBanner = findViewById(R.id.wifiBanner);

        btnStart.setOnClickListener(v -> startGame());
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateWifiStatus();
    }

    private void updateWifiStatus() {
        boolean ready = NetUtils.isWifiReady(this);
        if (ready) {
            tvWifiStatus.setText(R.string.status_connected);
            wifiBanner.setBackgroundResource(R.drawable.bg_status_connected);
            btnStart.setEnabled(true);
        } else {
            tvWifiStatus.setText(R.string.err_wifi_off);
            wifiBanner.setBackgroundResource(R.drawable.bg_status_disconnected);
            btnStart.setEnabled(false);
        }
    }

    private void startGame() {
        if (!NetUtils.isWifiReady(this)) {
            Toast.makeText(this, R.string.err_wifi_off, Toast.LENGTH_SHORT).show();
            return;
        }
        boolean isHost = rgRole.getCheckedRadioButtonId() == R.id.rbHost;
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
