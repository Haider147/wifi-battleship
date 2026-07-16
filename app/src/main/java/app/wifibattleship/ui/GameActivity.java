package app.wifibattleship.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
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
    private boolean destroyed = false;
    private GameController.Listener controllerListener;
    private BroadcastReceiver p2pReceiver;

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

        controllerListener = new GameController.Listener() {
            @Override
            public void onPhaseChanged(GamePhase phase) {
            }

            @Override
            public void onTurnChanged(boolean myTurn) {
                runOnUiThread(() -> {
                    if (destroyed) return;
                    updateTurnUI(myTurn);
                });
            }

            @Override
            public void onOpponentReady() {
            }

            @Override
            public void onMyBoardChanged() {
                runOnUiThread(() -> {
                    if (destroyed) return;
                    boardOwn.invalidate();
                });
            }

            @Override
            public void onEnemyBoardChanged() {
                runOnUiThread(() -> {
                    if (destroyed) return;
                    boardEnemy.invalidate();
                });
            }

            @Override
            public void onIncomingAttack(int x, int y) {
                runOnUiThread(() -> {
                    if (destroyed) return;
                    boardOwn.setLastHit(x, y);
                });
            }

            @Override
            public void onAttackResult(int x, int y, AttackResult result) {
                runOnUiThread(() -> {
                    if (destroyed) return;
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
                    if (destroyed) return;
                    localWon = iWon;
                    if (!ended) {
                        ended = true;
                        goToResult();
                    }
                });
            }

            @Override
            public void onDisconnected(boolean voluntaryExit) {
                runOnUiThread(() -> {
                    if (destroyed) return;
                    tvConnection.setText(R.string.status_disconnected);
                    tvConnection.setTextColor(getColor(R.color.hit));
                    if (!ended) {
                        ended = true;
                        if (voluntaryExit) {
                            localWon = false;
                            goToResult();
                        } else {
                            Toast.makeText(GameActivity.this, R.string.err_disconnected,
                                    Toast.LENGTH_LONG).show();
                            localWon = false;
                            goToResult();
                        }
                    }
                });
            }
        };
        controller.setListener(controllerListener);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                GameSession.get().getController().leave();
                finish();
            }
        });

        updateTurnUI(controller.isMyTurn());

        p2pReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                if (destroyed) return;
                NetworkInfo info = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
                if (info != null && !info.isConnected()) {
                    tvConnection.setText(R.string.status_disconnected);
                    tvConnection.setTextColor(getColor(R.color.hit));
                }
            }
        };
        registerReceiver(p2pReceiver,
                new IntentFilter(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION));
    }

    @Override
    protected void onResume() {
        super.onResume();
        GameController controller = GameSession.get().getController();
        if (controller != null) {
            updateTurnUI(controller.isMyTurn());
        }
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
        if (destroyed) return;
        GameController controller = GameSession.get().getController();
        if (!controller.isMyTurn()) {
            Toast.makeText(this, R.string.enemy_turn, Toast.LENGTH_SHORT).show();
            return;
        }
        if (controller.getEnemyBoard().wasAlreadyAttacked(row, col)) {
            Toast.makeText(this, R.string.already_attacked, Toast.LENGTH_SHORT).show();
            return;
        }
        String label = BoardView.cellLabel(row, col);
        new AlertDialog.Builder(this)
                .setTitle(R.string.confirm_attack_title)
                .setMessage(getString(R.string.confirm_attack_msg, label))
                .setPositiveButton(R.string.attack, (d, w) -> controller.localAttack(row, col))
                .setNegativeButton(R.string.exit, null)
                .show();
    }

    private void goToResult() {
        Intent intent = new Intent(this, ResultActivity.class);
        intent.putExtra(ResultActivity.EXTRA_I_WON, localWon);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        destroyed = true;
        if (p2pReceiver != null) {
            try {
                unregisterReceiver(p2pReceiver);
            } catch (IllegalArgumentException ignored) {
            }
            p2pReceiver = null;
        }
        GameController controller = GameSession.get().getController();
        if (controller != null) {
            controller.setListener(null);
        }
        if (GameSession.get().getConnection() != null) {
            GameSession.get().getConnection().close();
        }
        super.onDestroy();
    }
}
