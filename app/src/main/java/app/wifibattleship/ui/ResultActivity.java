package app.wifibattleship.ui;

import android.content.Intent;
import android.os.Bundle;
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
        tvResult.setTextColor(getColor(iWon ? R.color.primary : R.color.hit));

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
