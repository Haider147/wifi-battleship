package app.wifibattleship.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import app.wifibattleship.GameSession;
import app.wifibattleship.MainActivity;
import app.wifibattleship.R;

public class ResultActivity extends AppCompatActivity {

    public static final String EXTRA_I_WON = "extra_i_won";
    public static final String EXTRA_DISCONNECTED = "extra_disconnected";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        boolean iWon = getIntent().getBooleanExtra(EXTRA_I_WON, false);
        boolean disconnected = getIntent().getBooleanExtra(EXTRA_DISCONNECTED, false);

        int titleRes;
        int descRes;
        int stripRes;

        if (disconnected) {
            titleRes = R.string.conn_lost;
            descRes = R.string.conn_lost_desc;
            stripRes = R.drawable.bg_verdict_lose;
        } else {
            titleRes = iWon ? R.string.win : R.string.lose;
            descRes = iWon ? R.string.win_desc : R.string.lose_desc;
            stripRes = iWon ? R.drawable.bg_verdict_win : R.drawable.bg_verdict_lose;
        }

        TextView tvResult = findViewById(R.id.tvResult);
        tvResult.setText(titleRes);

        TextView tvResultDesc = findViewById(R.id.tvResultDesc);
        tvResultDesc.setText(descRes);

        View strip = findViewById(R.id.vVerdictStrip);
        strip.setBackgroundResource(stripRes);

        Button btnAgain = findViewById(R.id.btnAgain);
        Button btnExit = findViewById(R.id.btnExit);
        btnAgain.setOnClickListener(v -> {
            GameSession.reset();
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
        btnExit.setOnClickListener(v -> {
            GameSession.reset();
            finishAffinity();
        });
    }
}
