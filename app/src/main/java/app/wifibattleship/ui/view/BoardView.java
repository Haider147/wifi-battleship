package app.wifibattleship.ui.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import app.wifibattleship.R;
import app.wifibattleship.game.Board;
import app.wifibattleship.game.Cell;
import app.wifibattleship.game.GameConfig;
import app.wifibattleship.game.Orientation;
import app.wifibattleship.game.Ship;

/**
 * Tablero oceánico: agua con degradado y oleaje, coordenadas A-H / 1-8,
 * barcos ilustrados vistos desde arriba, fogonazos en los impactos y
 * ondas donde el disparo cayó al agua.
 */
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

    private static final int COLOR_WATER_LIGHT = Color.parseColor("#2E9BE8");
    private static final int COLOR_WATER_DEEP = Color.parseColor("#0D47A1");
    private static final int COLOR_BORDER = Color.parseColor("#0B3D91");
    private static final int COLOR_LABEL = Color.parseColor("#7A8494");
    private static final int COLOR_GRID = Color.argb(40, 255, 255, 255);
    private static final int COLOR_WAVE = Color.argb(26, 255, 255, 255);
    private static final int COLOR_SPARK = Color.parseColor("#FFE082");
    private static final int COLOR_RIPPLE = Color.WHITE;
    private static final int COLOR_SUNK_PATCH = Color.argb(230, 26, 34, 43);
    private static final int COLOR_SUNK_SHIP = Color.parseColor("#232C35");
    private static final int COLOR_PREVIEW_OK = Color.parseColor("#FFE082");
    private static final int COLOR_PREVIEW_BAD = Color.parseColor("#FF5252");
    private static final int COLOR_GHOST_BAD = Color.parseColor("#E53935");
    private static final int COLOR_LAST_HIT = Color.parseColor("#FFC107");
    private static final int COLOR_DIM = Color.argb(90, 0, 0, 0);

    private static final int[] FIRE_COLORS = {
            Color.parseColor("#FFF7C4"),
            Color.parseColor("#FFB74D"),
            Color.parseColor("#E53935"),
            Color.argb(48, 183, 28, 28)
    };
    private static final float[] FIRE_STOPS = {0f, 0.45f, 0.8f, 1f};

    private Board board;
    private Mode mode = Mode.OWN;

    private final Paint waterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint wavePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint firePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sparkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ripplePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sunkPatchPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint previewPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint lastHitPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint overlayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Path wavePath = new Path();
    private final Path sparkPath = new Path();
    private final Path clipPath = new Path();
    private final RectF gridRect = new RectF();
    private final RectF tmpRect = new RectF();

    private final SparseArray<Drawable> shipArt = new SparseArray<>(3);
    private final ColorFilter sunkFilter =
            new PorterDuffColorFilter(COLOR_SUNK_SHIP, PorterDuff.Mode.SRC_IN);
    private final ColorFilter ghostBadFilter =
            new PorterDuffColorFilter(COLOR_GHOST_BAD, PorterDuff.Mode.SRC_IN);

    private OnCellTapListener tapListener;
    private OnShipDropListener dropListener;

    private int draggedSize = 0;
    private Orientation draggedOrientation = Orientation.HORIZONTAL;
    private int previewRow = -1;
    private int previewCol = -1;

    private int cellSize;
    private int gridLeft;
    private int gridTop;
    private float cornerRadius;
    private float fireRadius;
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

        wavePaint.setColor(COLOR_WAVE);
        wavePaint.setStyle(Paint.Style.STROKE);
        wavePaint.setStrokeWidth(1.5f * density);
        wavePaint.setStrokeCap(Paint.Cap.ROUND);

        gridPaint.setColor(COLOR_GRID);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(Math.max(1f, density));

        borderPaint.setColor(COLOR_BORDER);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(2f * density);

        labelPaint.setColor(COLOR_LABEL);
        labelPaint.setTextSize(10.5f * density);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setFakeBoldText(true);

        sparkPaint.setColor(COLOR_SPARK);
        sparkPaint.setStyle(Paint.Style.STROKE);
        sparkPaint.setStrokeWidth(1.6f * density);
        sparkPaint.setStrokeCap(Paint.Cap.ROUND);

        ripplePaint.setColor(COLOR_RIPPLE);
        ripplePaint.setStyle(Paint.Style.STROKE);

        sunkPatchPaint.setColor(COLOR_SUNK_PATCH);
        sunkPatchPaint.setStyle(Paint.Style.FILL);

        previewPaint.setStyle(Paint.Style.STROKE);
        previewPaint.setStrokeWidth(1.6f * density);
        previewPaint.setPathEffect(new DashPathEffect(
                new float[]{5f * density, 4f * density}, 0f));

        lastHitPaint.setColor(COLOR_LAST_HIT);
        lastHitPaint.setStyle(Paint.Style.STROKE);
        lastHitPaint.setStrokeWidth(3f * density);

        overlayPaint.setColor(COLOR_DIM);
        overlayPaint.setStyle(Paint.Style.FILL);

        Context context = getContext();
        loadShipArt(context, 4, R.drawable.ic_ship_battleship);
        loadShipArt(context, 3, R.drawable.ic_ship_cruiser);
        loadShipArt(context, 2, R.drawable.ic_ship_destroyer);
    }

    private void loadShipArt(Context context, int size, int resId) {
        Drawable d = ContextCompat.getDrawable(context, resId);
        if (d != null) {
            shipArt.put(size, d.mutate());
        }
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

        float gridSide = SIZE * cellSize;
        gridRect.set(gridLeft, gridTop, gridLeft + gridSide, gridTop + gridSide);
        cornerRadius = Math.min(8f * density, cellSize * 0.28f);
        fireRadius = cellSize * 0.30f;

        waterPaint.setShader(new LinearGradient(
                gridRect.left, gridRect.top, gridRect.right, gridRect.bottom,
                COLOR_WATER_LIGHT, COLOR_WATER_DEEP, Shader.TileMode.CLAMP));
        firePaint.setShader(new RadialGradient(
                0f, 0f, Math.max(1f, fireRadius),
                FIRE_COLORS, FIRE_STOPS, Shader.TileMode.CLAMP));

        clipPath.reset();
        clipPath.addRoundRect(gridRect, cornerRadius, cornerRadius, Path.Direction.CW);

        buildWavePath();
        buildSparkPath();
    }

    /** Oleaje sutil: arcos dispersos de forma determinista por el tablero. */
    private void buildWavePath() {
        wavePath.reset();
        if (cellSize <= 0) {
            return;
        }
        for (int r = 0; r < SIZE; r++) {
            for (int k = 0; k < 3; k++) {
                int c = (r * 2 + k * 3) % SIZE;
                float x = gridLeft + c * cellSize + cellSize * 0.22f;
                float y = gridTop + r * cellSize
                        + cellSize * (0.55f + 0.15f * ((r + k) % 2));
                wavePath.moveTo(x, y);
                wavePath.quadTo(x + cellSize * 0.18f, y - cellSize * 0.24f,
                        x + cellSize * 0.36f, y);
            }
        }
    }

    /** Chispas del fogonazo: ocho rayos cortos alrededor del centro. */
    private void buildSparkPath() {
        sparkPath.reset();
        float in = fireRadius * 1.05f;
        float out = fireRadius * 1.45f;
        for (int i = 0; i < 8; i++) {
            double a = Math.toRadians(i * 45);
            float cos = (float) Math.cos(a);
            float sin = (float) Math.sin(a);
            sparkPath.moveTo(cos * in, sin * in);
            sparkPath.lineTo(cos * out, sin * out);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (board == null || cellSize <= 0) {
            return;
        }

        int save = canvas.save();
        canvas.clipPath(clipPath);

        canvas.drawRoundRect(gridRect, cornerRadius, cornerRadius, waterPaint);
        canvas.drawPath(wavePath, wavePaint);
        for (int i = 1; i < SIZE; i++) {
            float x = gridLeft + i * cellSize;
            canvas.drawLine(x, gridRect.top, x, gridRect.bottom, gridPaint);
            float y = gridTop + i * cellSize;
            canvas.drawLine(gridRect.left, y, gridRect.right, y, gridPaint);
        }

        if (mode != Mode.ENEMY) {
            drawShips(canvas);
        }
        drawCellMarkers(canvas);

        if (mode == Mode.PLACEMENT && previewRow >= 0 && previewCol >= 0 && draggedSize > 0) {
            drawPreview(canvas);
        }
        if (lastHitRow >= 0 && lastHitCol >= 0) {
            drawLastHitHighlight(canvas);
        }
        if (!isEnabled() && mode == Mode.ENEMY) {
            canvas.drawRoundRect(gridRect, cornerRadius, cornerRadius, overlayPaint);
        }

        canvas.restoreToCount(save);

        canvas.drawRoundRect(gridRect, cornerRadius, cornerRadius, borderPaint);
        if (labels) {
            drawLabels(canvas);
        }
    }

    private void drawShips(Canvas canvas) {
        for (Ship ship : board.getShips()) {
            boolean sunk = mode == Mode.OWN && ship.isSunk();
            drawShipArt(canvas, ship.getRow(), ship.getCol(), ship.getSize(),
                    ship.getOrientation(), sunk ? sunkFilter : null, 255);
        }
    }

    private void drawShipArt(Canvas canvas, int row, int col, int size,
                             Orientation orientation, @Nullable ColorFilter filter, int alpha) {
        Drawable art = shipArt.get(size);
        float left = gridLeft + col * cellSize;
        float top = gridTop + row * cellSize;
        if (art == null) {
            // Barco sin ilustración: casco genérico redondeado.
            float w = orientation == Orientation.HORIZONTAL ? size * cellSize : cellSize;
            float h = orientation == Orientation.HORIZONTAL ? cellSize : size * cellSize;
            float inset = cellSize * 0.14f;
            tmpRect.set(left + inset, top + inset, left + w - inset, top + h - inset);
            sunkPatchPaint.setAlpha(alpha);
            canvas.drawRoundRect(tmpRect, cellSize * 0.3f, cellSize * 0.3f, sunkPatchPaint);
            sunkPatchPaint.setAlpha(230);
            return;
        }
        int canvasSave = canvas.save();
        if (orientation == Orientation.VERTICAL) {
            canvas.translate(left + cellSize, top);
            canvas.rotate(90f);
        } else {
            canvas.translate(left, top);
        }
        art.setBounds(0, 0, size * cellSize, cellSize);
        art.setColorFilter(filter);
        art.setAlpha(alpha);
        art.draw(canvas);
        art.setColorFilter(null);
        art.setAlpha(255);
        canvas.restoreToCount(canvasSave);
    }

    private void drawCellMarkers(Canvas canvas) {
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                Cell cell = board.getCell(r, c);
                if (cell == Cell.WATER || cell == Cell.SHIP) {
                    continue;
                }
                float left = gridLeft + c * cellSize;
                float top = gridTop + r * cellSize;
                float cx = left + cellSize / 2f;
                float cy = top + cellSize / 2f;
                switch (cell) {
                    case MISS -> drawRipples(canvas, cx, cy);
                    case SUNK -> {
                        if (mode == Mode.ENEMY) {
                            float inset = 1.2f * density;
                            tmpRect.set(left + inset, top + inset,
                                    left + cellSize - inset, top + cellSize - inset);
                            canvas.drawRoundRect(tmpRect, 4f * density, 4f * density,
                                    sunkPatchPaint);
                        }
                        drawFire(canvas, cx, cy);
                    }
                    case HIT -> drawFire(canvas, cx, cy);
                    default -> {}
                }
            }
        }
    }

    private void drawFire(Canvas canvas, float cx, float cy) {
        int save = canvas.save();
        canvas.translate(cx, cy);
        canvas.drawPath(sparkPath, sparkPaint);
        canvas.drawCircle(0f, 0f, fireRadius, firePaint);
        canvas.restoreToCount(save);
    }

    private void drawRipples(Canvas canvas, float cx, float cy) {
        ripplePaint.setStrokeWidth(1.7f * density);
        ripplePaint.setAlpha(210);
        canvas.drawCircle(cx, cy, cellSize * 0.15f, ripplePaint);
        ripplePaint.setStrokeWidth(1.4f * density);
        ripplePaint.setAlpha(90);
        canvas.drawCircle(cx, cy, cellSize * 0.29f, ripplePaint);
    }

    private void drawPreview(Canvas canvas) {
        boolean valid = board.isValidPlacement(previewRow, previewCol,
                draggedSize, draggedOrientation);

        drawShipArt(canvas, previewRow, previewCol, draggedSize, draggedOrientation,
                valid ? null : ghostBadFilter, 150);

        float w = draggedOrientation == Orientation.HORIZONTAL
                ? draggedSize * cellSize : cellSize;
        float h = draggedOrientation == Orientation.HORIZONTAL
                ? cellSize : draggedSize * cellSize;
        float left = gridLeft + previewCol * cellSize;
        float top = gridTop + previewRow * cellSize;
        float inset = 1.5f * density;
        tmpRect.set(left + inset, top + inset, left + w - inset, top + h - inset);
        previewPaint.setColor(valid ? COLOR_PREVIEW_OK : COLOR_PREVIEW_BAD);
        canvas.drawRoundRect(tmpRect, 6f * density, 6f * density, previewPaint);
    }

    private void drawLastHitHighlight(Canvas canvas) {
        float left = gridLeft + lastHitCol * cellSize;
        float top = gridTop + lastHitRow * cellSize;
        float inset = 1.5f * density;
        tmpRect.set(left + inset, top + inset,
                left + cellSize - inset, top + cellSize - inset);
        canvas.drawRoundRect(tmpRect, 4f * density, 4f * density, lastHitPaint);
    }

    private void drawLabels(Canvas canvas) {
        for (int c = 0; c < SIZE; c++) {
            String letter = String.valueOf((char) ('A' + c));
            float x = gridLeft + c * cellSize + cellSize / 2f;
            float y = gridTop - 5 * density;
            canvas.drawText(letter, x, y, labelPaint);
        }
        for (int r = 0; r < SIZE; r++) {
            String num = String.valueOf(r + 1);
            float x = gridLeft - 10 * density;
            float y = gridTop + r * cellSize + cellSize / 2f + labelPaint.getTextSize() / 3f;
            canvas.drawText(num, x, y, labelPaint);
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
            case DragEvent.ACTION_DRAG_STARTED -> {
                return true;
            }
            case DragEvent.ACTION_DRAG_LOCATION -> {
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
            }
            case DragEvent.ACTION_DROP -> {
                int[] drop = cellFromPoint(event.getX(), event.getY());
                if (drop != null && dropListener != null) {
                    dropListener.onShipDrop(draggedSize, drop[0], drop[1], draggedOrientation);
                }
                previewRow = -1;
                previewCol = -1;
                invalidate();
                return true;
            }
            case DragEvent.ACTION_DRAG_ENDED -> {
                previewRow = -1;
                previewCol = -1;
                invalidate();
                return true;
            }
            default -> {
                return true;
            }
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
