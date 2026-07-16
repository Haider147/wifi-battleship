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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        boolean iWon = getIntent().getBooleanExtra(EXTRA_I_WON, false);

        TextView tvResult = findViewById(R.id.tvResult);
        tvResult.setText(iWon ? R.string.win : R.string.lose);

        TextView tvResultDesc = findViewById(R.id.tvResultDesc);
        tvResultDesc.setText(iWon ? R.string.win_desc : R.string.lose_desc);

        View strip = findViewById(R.id.vVerdictStrip);
        strip.setBackgroundResource(iWon
                ? R.drawable.bg_verdict_win : R.drawable.bg_verdict_lose);

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        GameSession.reset();
    }
}
