package app.wifibattleship.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import app.wifibattleship.GameSession;
import app.wifibattleship.R;
import app.wifibattleship.game.AttackResult;
import app.wifibattleship.game.GameController;
import app.wifibattleship.game.GamePhase;
import app.wifibattleship.ui.view.BoardView;

public class GameActivity extends AppCompatActivity {

    private BoardView boardOwn;
    private BoardView boardEnemy;
    private TextView tvTurn;
    private TextView tvConnection;
    private TextView tvLastResult;
    private View turnBanner;
    private boolean ended = false;
    private boolean localWon = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        boardOwn = findViewById(R.id.boardOwn);
        boardEnemy = findViewById(R.id.boardEnemy);
        tvTurn = findViewById(R.id.tvTurn);
        tvConnection = findViewById(R.id.tvConnection);
        tvLastResult = findViewById(R.id.tvLastResult);
        turnBanner = findViewById(R.id.turnBanner);

        GameController controller = GameSession.get().getController();
        boardOwn.setBoard(controller.getMyBoard());
        boardOwn.setMode(BoardView.Mode.OWN);
        boardEnemy.setBoard(controller.getEnemyBoard());
        boardEnemy.setMode(BoardView.Mode.ENEMY);

        boardEnemy.setOnCellTapListener(this::onEnemyCellTap);

        controller.setListener(new GameController.Listener() {
            @Override
            public void onPhaseChanged(GamePhase phase) {
            }

            @Override
            public void onTurnChanged(boolean myTurn) {
                runOnUiThread(() -> updateTurnUI(myTurn));
            }

            @Override
            public void onOpponentReady() {
            }

            @Override
            public void onMyBoardChanged() {
                runOnUiThread(() -> boardOwn.invalidate());
            }

            @Override
            public void onEnemyBoardChanged() {
                runOnUiThread(() -> boardEnemy.invalidate());
            }

            @Override
            public void onIncomingAttack(int x, int y) {
                runOnUiThread(() -> boardOwn.setLastHit(x, y));
            }

            @Override
            public void onAttackResult(int x, int y, AttackResult result) {
                runOnUiThread(() -> {
                    boardEnemy.setLastHit(x, y);
                    boardEnemy.invalidate();
                    int resId;
                    int color;
                    switch (result) {
                        case HIT:
                            resId = R.string.result_hit;
                            color = R.color.hit;
                            break;
                        case SUNK:
                            resId = R.string.result_sunk;
                            color = R.color.sunk;
                            break;
                        default:
                            resId = R.string.result_water;
                            color = R.color.miss;
                            break;
                    }
                    tvLastResult.setText(getString(resId) + " — " + BoardView.cellLabel(x, y));
                    tvLastResult.setTextColor(getColor(color));
                });
            }

            @Override
            public void onGameOver(boolean iWon) {
                runOnUiThread(() -> {
                    localWon = iWon;
                    if (!ended) {
                        ended = true;
                        goToResult();
                    }
                });
            }

            @Override
            public void onDisconnected() {
                runOnUiThread(() -> {
                    tvConnection.setText(R.string.status_disconnected);
                    tvConnection.setTextColor(getColor(R.color.hit));
                    Toast.makeText(GameActivity.this, R.string.err_disconnected,
                            Toast.LENGTH_LONG).show();
                    if (!ended) {
                        ended = true;
                        localWon = false;
                        goToResult();
                    }
                });
            }
        });

        updateTurnUI(controller.isMyTurn());
    }

    private void updateTurnUI(boolean myTurn) {
        tvTurn.setText(myTurn ? R.string.your_turn : R.string.enemy_turn);
        tvTurn.setTextColor(getColor(R.color.white));
        turnBanner.setBackgroundResource(myTurn
                ? R.drawable.bg_turn_mine : R.drawable.bg_turn_enemy);
        boardEnemy.setEnabled(myTurn);
        boardEnemy.invalidate();
        if (!myTurn) {
            tvLastResult.setText("Esperando jugada del enemigo…");
            tvLastResult.setTextColor(getColor(R.color.miss));
        } else {
            tvLastResult.setText("Toca una celda del tablero enemigo para atacar.");
            tvLastResult.setTextColor(getColor(R.color.white));
        }
    }

    private void onEnemyCellTap(int row, int col) {
        GameController controller = GameSession.get().getController();
        if (!controller.isMyTurn()) {
            Toast.makeText(this, R.string.enemy_turn, Toast.LENGTH_SHORT).show();
            return;
        }
        controller.localAttack(row, col);
    }

    private void goToResult() {
        Intent intent = new Intent(this, ResultActivity.class);
        intent.putExtra(ResultActivity.EXTRA_I_WON, localWon);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        GameSession.get().getController().leave();
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ended && GameSession.get().getConnection() != null) {
            GameSession.get().getConnection().close();
        }
    }
}
