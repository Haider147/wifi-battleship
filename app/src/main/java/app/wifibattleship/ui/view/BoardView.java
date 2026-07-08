package app.wifibattleship.ui.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import app.wifibattleship.game.Board;
import app.wifibattleship.game.Cell;
import app.wifibattleship.game.GameConfig;
import app.wifibattleship.game.Orientation;

public class BoardView extends View {

    public enum Mode {
        PLACEMENT,
        OWN,
        ENEMY
    }

    public interface OnCellTapListener {
        void onCellTap(int row, int col);
    }

    public interface OnShipDropListener {
        void onShipDrop(int shipSize, int row, int col, Orientation orientation);
    }

    private static final int SIZE = GameConfig.BOARD_SIZE;

    private Board board;
    private Mode mode = Mode.OWN;

    private final Paint cellPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint previewPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private OnCellTapListener tapListener;
    private OnShipDropListener dropListener;

    private int draggedSize = 0;
    private Orientation draggedOrientation = Orientation.HORIZONTAL;
    private int previewRow = -1;
    private int previewCol = -1;

    private int cellSize;
    private int gridLeft;
    private int gridTop;
    private boolean labels = true;

    public BoardView(Context context) {
        super(context);
        init();
    }

    public BoardView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BoardView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        linePaint.setColor(Color.parseColor("#0D47A1"));
        linePaint.setStrokeWidth(1f);
        linePaint.setStyle(Paint.Style.STROKE);
        labelPaint.setColor(Color.parseColor("#90A4AE"));
        labelPaint.setTextSize(22f);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        previewPaint.setStyle(Paint.Style.FILL);
    }

    public void setBoard(Board board) {
        this.board = board;
        invalidate();
    }

    public void setMode(Mode mode) {
        this.mode = mode;
        invalidate();
    }

    public Mode getMode() {
        return mode;
    }

    public void setOnCellTapListener(OnCellTapListener l) {
        this.tapListener = l;
    }

    public void setOnShipDropListener(OnShipDropListener l) {
        this.dropListener = l;
    }

    public void setDraggedOrientation(Orientation orientation) {
        this.draggedOrientation = orientation;
    }

    public void setShowLabels(boolean show) {
        this.labels = show;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desired = dp(320);
        int w = resolveSize(desired, widthMeasureSpec);
        int h = resolveSize(desired, heightMeasureSpec);
        int side = Math.min(w, h);
        setMeasuredDimension(side, side);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        int side = Math.min(w, h);
        int pad = dp(4);
        cellSize = (side - 2 * pad) / SIZE;
        gridLeft = pad + (w - side) / 2;
        gridTop = pad + (h - side) / 2;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (board == null) {
            return;
        }
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                Cell cell = board.getCell(r, c);
                cellPaint.setColor(colorFor(cell));
                float left = gridLeft + c * cellSize;
                float top = gridTop + r * cellSize;
                canvas.drawRect(left, top, left + cellSize, top + cellSize, cellPaint);
            }
        }
        for (int i = 0; i <= SIZE; i++) {
            float x = gridLeft + i * cellSize;
            canvas.drawLine(x, gridTop, x, gridTop + SIZE * cellSize, linePaint);
            float y = gridTop + i * cellSize;
            canvas.drawLine(gridLeft, y, gridLeft + SIZE * cellSize, y, linePaint);
        }
        if (labels) {
            drawLabels(canvas);
        }
        if (mode == Mode.PLACEMENT && previewRow >= 0 && previewCol >= 0 && draggedSize > 0) {
            drawPreview(canvas);
        }
    }

    private void drawLabels(Canvas canvas) {
        for (int c = 0; c < SIZE; c++) {
            String letter = String.valueOf((char) ('A' + c));
            float x = gridLeft + c * cellSize + cellSize / 2f;
            float y = gridTop - dp(2);
            canvas.drawText(letter, x, y, labelPaint);
        }
        for (int r = 0; r < SIZE; r++) {
            String num = String.valueOf(r + 1);
            float x = gridLeft - dp(10);
            float y = gridTop + r * cellSize + cellSize / 2f + labelPaint.getTextSize() / 3f;
            canvas.drawText(num, x, y, labelPaint);
        }
    }

    private void drawPreview(Canvas canvas) {
        boolean valid = board.isValidPlacement(previewRow, previewCol, draggedSize, draggedOrientation);
        previewPaint.setColor(valid ? Color.argb(120, 76, 175, 80) : Color.argb(120, 244, 67, 54));
        for (int i = 0; i < draggedSize; i++) {
            int r = draggedOrientation == Orientation.HORIZONTAL ? previewRow : previewRow + i;
            int c = draggedOrientation == Orientation.HORIZONTAL ? previewCol + i : previewCol;
            if (r < 0 || r >= SIZE || c < 0 || c >= SIZE) {
                continue;
            }
            float left = gridLeft + c * cellSize;
            float top = gridTop + r * cellSize;
            canvas.drawRect(left, top, left + cellSize, top + cellSize, previewPaint);
        }
    }

    private int colorFor(Cell cell) {
        switch (cell) {
            case WATER:
                return Color.parseColor("#1565C0");
            case SHIP:
                if (mode == Mode.ENEMY) {
                    return Color.parseColor("#1565C0");
                }
                return Color.parseColor("#616161");
            case HIT:
                return Color.parseColor("#D32F2F");
            case MISS:
                return Color.parseColor("#90A4AE");
            case SUNK:
                return Color.parseColor("#212121");
            default:
                return Color.parseColor("#1565C0");
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP && tapListener != null) {
            int[] cell = cellFromPoint(event.getX(), event.getY());
            if (cell != null) {
                tapListener.onCellTap(cell[0], cell[1]);
            }
        }
        return true;
    }

    public boolean handleDrag(DragEvent event) {
        if (mode != Mode.PLACEMENT) {
            return false;
        }
        switch (event.getAction()) {
            case DragEvent.ACTION_DRAG_STARTED:
                return true;
            case DragEvent.ACTION_DRAG_LOCATION:
                int[] cell = cellFromPoint(event.getX(), event.getY());
                if (cell != null) {
                    previewRow = cell[0];
                    previewCol = cell[1];
                } else {
                    previewRow = -1;
                    previewCol = -1;
                }
                invalidate();
                return true;
            case DragEvent.ACTION_DROP:
                int[] drop = cellFromPoint(event.getX(), event.getY());
                if (drop != null && dropListener != null) {
                    dropListener.onShipDrop(draggedSize, drop[0], drop[1], draggedOrientation);
                }
                previewRow = -1;
                previewCol = -1;
                invalidate();
                return true;
            case DragEvent.ACTION_DRAG_ENDED:
                previewRow = -1;
                previewCol = -1;
                invalidate();
                return true;
            default:
                return true;
        }
    }

    @android.annotation.SuppressLint("ClickableViewAccessibility")
    public void startDragMode(int shipSize) {
        this.draggedSize = shipSize;
        setOnDragListener((v, event) -> handleDrag(event));
    }

    @Nullable
    private int[] cellFromPoint(float x, float y) {
        if (cellSize <= 0) {
            return null;
        }
        int col = (int) ((x - gridLeft) / cellSize);
        int row = (int) ((y - gridTop) / cellSize);
        if (row < 0 || row >= SIZE || col < 0 || col >= SIZE) {
            return null;
        }
        return new int[]{row, col};
    }

    public int getCellSize() {
        return cellSize;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    @SuppressWarnings("unused")
    public void resetPreview() {
        previewRow = -1;
        previewCol = -1;
        draggedSize = 0;
        invalidate();
    }

    public static String cellLabel(int row, int col) {
        return String.valueOf((char) ('A' + col)) + (row + 1);
    }
}
