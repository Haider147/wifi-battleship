package app.wifibattleship.ui;

import android.content.ClipData;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

import app.wifibattleship.GameSession;
import app.wifibattleship.R;
import app.wifibattleship.game.Board;
import app.wifibattleship.game.GameConfig;
import app.wifibattleship.game.GameController;
import app.wifibattleship.game.GamePhase;
import app.wifibattleship.game.Orientation;
import app.wifibattleship.ui.view.BoardView;

public class PlacementActivity extends AppCompatActivity {

    private BoardView boardView;
    private LinearLayout tray;
    private Button btnReady;
    private TextView tvStatus;

    private final List<Integer> traySizes = new ArrayList<>();
    private Orientation currentOrientation = Orientation.HORIZONTAL;
    private boolean readySent = false;
    private boolean advanced = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_placement);

        boardView = findViewById(R.id.boardView);
        tray = findViewById(R.id.tray);
        btnReady = findViewById(R.id.btnReady);
        tvStatus = findViewById(R.id.tvStatus);
        Button btnRotate = findViewById(R.id.btnRotate);

        Board board = GameSession.get().getController().getMyBoard();
        boardView.setBoard(board);
        boardView.setMode(BoardView.Mode.PLACEMENT);
        boardView.setDraggedOrientation(currentOrientation);

        for (int size : GameConfig.SHIP_SIZES) {
            traySizes.add(size);
        }
        refreshTray();

        boardView.setOnShipDropListener(this::onShipDrop);
        boardView.setOnCellTapListener(this::onCellTap);

        btnRotate.setOnClickListener(v -> {
            currentOrientation = currentOrientation.toggle();
            boardView.setDraggedOrientation(currentOrientation);
        });

        btnReady.setOnClickListener(v -> onReady());

        wireController();
    }

    private void wireController() {
        GameController controller = GameSession.get().getController();
        controller.setListener(new GameController.Listener() {
            @Override
            public void onPhaseChanged(GamePhase phase) {
                if (phase == GamePhase.PLAYING && !advanced) {
                    advanced = true;
                    goToGame();
                }
            }

            @Override
            public void onTurnChanged(boolean myTurn) {
            }

            @Override
            public void onOpponentReady() {
                runOnUiThread(() -> Toast.makeText(PlacementActivity.this,
                        R.string.opponent_ready, Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onMyBoardChanged() {
            }

            @Override
            public void onEnemyBoardChanged() {
            }

            @Override
            public void onIncomingAttack(int x, int y) {
            }

            @Override
            public void onAttackResult(int x, int y, app.wifibattleship.game.AttackResult result) {
            }

            @Override
            public void onGameOver(boolean iWon) {
            }

            @Override
            public void onDisconnected() {
                runOnUiThread(() -> {
                    Toast.makeText(PlacementActivity.this, R.string.err_disconnected,
                            Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        });
    }

    private void refreshTray() {
        tray.removeAllViews();
        for (int size : traySizes) {
            tray.addView(createChip(size));
        }
        btnReady.setEnabled(traySizes.isEmpty());
    }

    private View createChip(int size) {
        TextView chip = new TextView(this);
        chip.setText(shipName(size));
        chip.setPadding(dp(16), dp(12), dp(16), dp(12));
        chip.setBackgroundResource(R.color.ship);
        chip.setTextColor(getColor(R.color.white));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMarginEnd(dp(8));
        chip.setLayoutParams(lp);
        chip.setOnTouchListener((v, event) -> onChipTouch(v, size, event));
        return chip;
    }

    @android.annotation.SuppressLint("ClickableViewAccessibility")
    private boolean onChipTouch(View v, int size, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            boardView.startDragMode(size);
            boardView.setDraggedOrientation(currentOrientation);
            ClipData clip = ClipData.newPlainText("ship", String.valueOf(size));
            View.DragShadowBuilder shadow = new View.DragShadowBuilder(v);
            v.startDragAndDrop(clip, shadow, null, 0);
            return true;
        }
        return false;
    }

    private void onShipDrop(int shipSize, int row, int col, Orientation orientation) {
        Board board = GameSession.get().getController().getMyBoard();
        if (board.placeShip(shipSize, orientation, row, col)) {
            traySizes.remove(Integer.valueOf(shipSize));
            refreshTray();
            boardView.invalidate();
        } else {
            Toast.makeText(this, R.string.err_placement, Toast.LENGTH_SHORT).show();
            boardView.resetPreview();
        }
    }

    private void onCellTap(int row, int col) {
        if (readySent) {
            return;
        }
        Board board = GameSession.get().getController().getMyBoard();
        int size = board.getShipSizeAt(row, col);
        if (size > 0) {
            board.removeShipAt(row, col);
            traySizes.add(size);
            refreshTray();
            boardView.invalidate();
        }
    }

    private void onReady() {
        if (!traySizes.isEmpty()) {
            Toast.makeText(this, R.string.all_placed, Toast.LENGTH_SHORT).show();
            return;
        }
        if (readySent) {
            return;
        }
        readySent = true;
        btnReady.setEnabled(false);
        tvStatus.setText(R.string.placement_done);
        tray.setVisibility(View.GONE);
        GameSession.get().getController().setLocalReady();
    }

    private void goToGame() {
        Intent intent = new Intent(this, GameActivity.class);
        startActivity(intent);
        finish();
    }

    private String shipName(int size) {
        switch (size) {
            case 4:
                return getString(R.string.ship_4);
            case 3:
                return getString(R.string.ship_3);
            default:
                return getString(R.string.ship_2);
        }
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
