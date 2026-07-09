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
    private Button btnRotate;
    private TextView tvStatus;
    private TextView tvOrientation;

    private final List<Integer> traySizes = new ArrayList<>();
    private Orientation currentOrientation = Orientation.HORIZONTAL;
    private boolean readySent = false;
    private boolean advanced = false;
    private boolean destroyed = false;
    private GameController.Listener controllerListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_placement);

        boardView = findViewById(R.id.boardView);
        tray = findViewById(R.id.tray);
        btnReady = findViewById(R.id.btnReady);
        btnRotate = findViewById(R.id.btnRotate);
        tvStatus = findViewById(R.id.tvStatus);
        tvOrientation = findViewById(R.id.tvOrientation);

        Board board = GameSession.get().getController().getMyBoard();
        boardView.setBoard(board);
        boardView.setMode(BoardView.Mode.PLACEMENT);
        boardView.setDraggedOrientation(currentOrientation);

        for (int size : GameConfig.SHIP_SIZES) {
            traySizes.add(size);
        }
        refreshTray();
        updateOrientationLabel();

        boardView.setOnShipDropListener(this::onShipDrop);
        boardView.setOnCellTapListener(this::onCellTap);

        btnRotate.setOnClickListener(v -> {
            currentOrientation = currentOrientation.toggle();
            boardView.setDraggedOrientation(currentOrientation);
            updateOrientationLabel();
        });

        btnReady.setOnClickListener(v -> onReady());

        wireController();
    }

    private void wireController() {
        GameController controller = GameSession.get().getController();
        controllerListener = new GameController.Listener() {
            @Override
            public void onPhaseChanged(GamePhase phase) {
                runOnUiThread(() -> {
                    if (destroyed) return;
                    if (phase == GamePhase.PLAYING && !advanced) {
                        advanced = true;
                        goToGame();
                    }
                });
            }

            @Override
            public void onTurnChanged(boolean myTurn) {
            }

            @Override
            public void onOpponentReady() {
                runOnUiThread(() -> {
                    if (destroyed) return;
                    Toast.makeText(PlacementActivity.this,
                            R.string.opponent_ready, Toast.LENGTH_SHORT).show();
                });
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
            public void onDisconnected(boolean voluntaryExit) {
                runOnUiThread(() -> {
                    if (destroyed) return;
                    Toast.makeText(PlacementActivity.this, R.string.err_disconnected,
                            Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        };
        controller.setListener(controllerListener);
    }

    private void refreshTray() {
        tray.removeAllViews();
        for (int size : traySizes) {
            tray.addView(createChip(size));
        }
        btnReady.setEnabled(traySizes.isEmpty());
        if (traySizes.isEmpty()) {
            tvStatus.setText("Todos los barcos colocados. Pulsa «Listo».");
        } else {
            tvStatus.setText(R.string.placement_hint);
        }
    }

    private void updateOrientationLabel() {
        tvOrientation.setText(currentOrientation == Orientation.HORIZONTAL
                ? "Orientación: Horizontal" : "Orientación: Vertical");
    }

    private View createChip(int size) {
        TextView chip = new TextView(this);
        chip.setText(shipLabel(size));
        chip.setPadding(dp(20), dp(14), dp(20), dp(14));
        chip.setBackgroundResource(R.drawable.bg_ship_chip);
        chip.setTextColor(getColor(R.color.white));
        chip.setTextSize(15);
        chip.setAllCaps(false);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMarginEnd(dp(10));
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
        if (destroyed) return;
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
        if (destroyed) return;
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
        if (destroyed) return;
        if (!traySizes.isEmpty()) {
            Toast.makeText(this, R.string.all_placed, Toast.LENGTH_SHORT).show();
            return;
        }
        if (readySent) {
            return;
        }
        readySent = true;
        btnReady.setEnabled(false);
        btnRotate.setEnabled(false);
        tvStatus.setText(R.string.placement_done);
        tray.setVisibility(View.GONE);
        GameSession.get().getController().setLocalReady();
    }

    private void goToGame() {
        Intent intent = new Intent(this, GameActivity.class);
        startActivity(intent);
        finish();
    }

    private String shipLabel(int size) {
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

    @Override
    protected void onDestroy() {
        destroyed = true;
        GameController controller = GameSession.get().getController();
        if (controller != null) {
            controller.setListener(null);
        }
        super.onDestroy();
    }
}
