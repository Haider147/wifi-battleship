package app.wifibattleship.ui.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
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

    private static final int COLOR_LINE = Color.parseColor("#0D47A1");
    private static final int COLOR_BORDER = Color.parseColor("#002171");
    private static final int COLOR_LABEL = Color.parseColor("#B0BEC5");
    private static final int COLOR_WATER = Color.parseColor("#1565C0");
    private static final int COLOR_SHIP_OWN = Color.parseColor("#546E7A");
    private static final int COLOR_HIT = Color.parseColor("#D32F2F");
    private static final int COLOR_MISS_CELL = Color.parseColor("#263238");
    private static final int COLOR_SUNK = Color.parseColor("#212121");
    private static final int COLOR_MISS_MARKER = Color.parseColor("#FFFFFF");
    private static final int COLOR_HIT_MARKER = Color.parseColor("#FFEB3B");
    private static final int COLOR_LAST_HIT = Color.parseColor("#FFC107");
    private static final int COLOR_PREVIEW_OK = Color.argb(130, 76, 175, 80);
    private static final int COLOR_PREVIEW_BAD = Color.argb(130, 244, 67, 54);
    private static final int COLOR_DIM = Color.argb(90, 0, 0, 0);

    private Board board;
    private Mode mode = Mode.OWN;

    private final Paint cellPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint previewPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint markerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint overlayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

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
    private int lastHitRow = -1;
    private int lastHitCol = -1;
    private float density;

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
        density = getResources().getDisplayMetrics().density;
        linePaint.setColor(COLOR_LINE);
        linePaint.setStrokeWidth(Math.max(1f, density));
        linePaint.setStyle(Paint.Style.STROKE);
        borderPaint.setColor(COLOR_BORDER);
        borderPaint.setStrokeWidth(2f * density);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setAntiAlias(true);
        labelPaint.setColor(COLOR_LABEL);
        labelPaint.setTextSize(11f * density);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        previewPaint.setStyle(Paint.Style.FILL);
        markerPaint.setStyle(Paint.Style.STROKE);
        markerPaint.setStrokeWidth(2.5f * density);
        markerPaint.setAntiAlias(true);
        overlayPaint.setStyle(Paint.Style.FILL);
    }

    public void setBoard(Board board) {
        this.board = board;
        invalidate();
    }

    public void setMode(Mode mode) {
        this.mode = mode;
        if (mode != Mode.PLACEMENT) {
            setOnDragListener(null);
            draggedSize = 0;
            previewRow = -1;
            previewCol = -1;
        }
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

    public void setLastHit(int row, int col) {
        if (row < 0 || row >= SIZE || col < 0 || col >= SIZE) {
            clearLastHit();
            return;
        }
        this.lastHitRow = row;
        this.lastHitCol = col;
        invalidate();
    }

    public void clearLastHit() {
        this.lastHitRow = -1;
        this.lastHitCol = -1;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desired = (int) (300 * density);
        int w = resolveSize(desired, widthMeasureSpec);
        int h = resolveSize(desired, heightMeasureSpec);
        int side = Math.min(w, h);
        setMeasuredDimension(side, side);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        int side = Math.min(w, h);
        int pad = (int) (16 * density);
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
                drawCellMarker(canvas, cell, left, top);
            }
        }
        for (int i = 0; i <= SIZE; i++) {
            float x = gridLeft + i * cellSize;
            canvas.drawLine(x, gridTop, x, gridTop + SIZE * cellSize, linePaint);
            float y = gridTop + i * cellSize;
            canvas.drawLine(gridLeft, y, gridLeft + SIZE * cellSize, y, linePaint);
        }
        RectF gridRect = new RectF(gridLeft, gridTop,
                gridLeft + SIZE * cellSize, gridTop + SIZE * cellSize);
        canvas.drawRoundRect(gridRect, 4 * density, 4 * density, borderPaint);

        if (labels) {
            drawLabels(canvas);
        }
        if (mode == Mode.PLACEMENT && previewRow >= 0 && previewCol >= 0 && draggedSize > 0) {
            drawPreview(canvas);
        }
        if (lastHitRow >= 0 && lastHitCol >= 0) {
            drawLastHitHighlight(canvas);
        }
        if (!isEnabled() && mode == Mode.ENEMY) {
            drawDimOverlay(canvas);
        }
    }

    private void drawCellMarker(Canvas canvas, Cell cell, float left, float top) {
        float cx = left + cellSize / 2f;
        float cy = top + cellSize / 2f;
        float radius = cellSize * 0.18f;
        switch (cell) {
            case MISS:
                markerPaint.setColor(COLOR_MISS_MARKER);
                markerPaint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(cx, cy, radius, markerPaint);
                markerPaint.setStyle(Paint.Style.STROKE);
                break;
            case HIT:
                markerPaint.setColor(COLOR_HIT_MARKER);
                markerPaint.setStyle(Paint.Style.STROKE);
                float cross = cellSize * 0.28f;
                canvas.drawLine(cx - cross, cy - cross, cx + cross, cy + cross, markerPaint);
                canvas.drawLine(cx - cross, cy + cross, cx + cross, cy - cross, markerPaint);
                break;
            case SUNK:
                markerPaint.setColor(COLOR_HIT_MARKER);
                markerPaint.setStyle(Paint.Style.STROKE);
                float r = cellSize * 0.32f;
                canvas.drawCircle(cx, cy, r, markerPaint);
                float cross2 = cellSize * 0.22f;
                canvas.drawLine(cx - cross2, cy - cross2, cx + cross2, cy + cross2, markerPaint);
                canvas.drawLine(cx - cross2, cy + cross2, cx + cross2, cy - cross2, markerPaint);
                break;
            default:
                break;
        }
    }

    private void drawLabels(Canvas canvas) {
        for (int c = 0; c < SIZE; c++) {
            String letter = String.valueOf((char) ('A' + c));
            float x = gridLeft + c * cellSize + cellSize / 2f;
            float y = gridTop - 4 * density;
            canvas.drawText(letter, x, y, labelPaint);
        }
        for (int r = 0; r < SIZE; r++) {
            String num = String.valueOf(r + 1);
            float x = gridLeft - 10 * density;
            float y = gridTop + r * cellSize + cellSize / 2f + labelPaint.getTextSize() / 3f;
            canvas.drawText(num, x, y, labelPaint);
        }
    }

    private void drawPreview(Canvas canvas) {
        boolean valid = board.isValidPlacement(previewRow, previewCol, draggedSize, draggedOrientation);
        previewPaint.setColor(valid ? COLOR_PREVIEW_OK : COLOR_PREVIEW_BAD);
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

    private void drawLastHitHighlight(Canvas canvas) {
        float left = gridLeft + lastHitCol * cellSize;
        float top = gridTop + lastHitRow * cellSize;
        borderPaint.setColor(COLOR_LAST_HIT);
        borderPaint.setStrokeWidth(3f * density);
        RectF rect = new RectF(left, top, left + cellSize, top + cellSize);
        canvas.drawRect(rect, borderPaint);
        borderPaint.setColor(COLOR_BORDER);
        borderPaint.setStrokeWidth(2f * density);
    }

    private void drawDimOverlay(Canvas canvas) {
        overlayPaint.setColor(COLOR_DIM);
        RectF gridRect = new RectF(gridLeft, gridTop,
                gridLeft + SIZE * cellSize, gridTop + SIZE * cellSize);
        canvas.drawRect(gridRect, overlayPaint);
    }

    private int colorFor(Cell cell) {
        switch (cell) {
            case WATER:
                return COLOR_WATER;
            case SHIP:
                if (mode == Mode.ENEMY) {
                    return COLOR_WATER;
                }
                return COLOR_SHIP_OWN;
            case HIT:
                return COLOR_HIT;
            case MISS:
                return COLOR_MISS_CELL;
            case SUNK:
                return COLOR_SUNK;
            default:
                return COLOR_WATER;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            return false;
        }
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
        android.content.ClipDescription desc = event.getClipDescription();
        if (desc != null && desc.getLabel() != null && !"ship".equals(desc.getLabel().toString())) {
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

    @SuppressWarnings("unused")
    public void resetPreview() {
        previewRow = -1;
        previewCol = -1;
        draggedSize = 0;
        invalidate();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        tapListener = null;
        dropListener = null;
        setOnDragListener(null);
    }

    public static String cellLabel(int row, int col) {
        if (col < 0 || col >= 26 || row < 0) {
            return "?";
        }
        return String.valueOf((char) ('A' + col)) + (row + 1);
    }
}
