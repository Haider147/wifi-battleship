package app.wifibattleship.game;

import java.util.ArrayList;
import java.util.List;

public class Ship {

    private final int size;
    private final Orientation orientation;
    private final int row;
    private final int col;
    private int hitCount;

    public Ship(int size, Orientation orientation, int row, int col) {
        if (size <= 0) {
            throw new IllegalArgumentException("size must be > 0");
        }
        this.size = size;
        this.orientation = orientation;
        this.row = row;
        this.col = col;
        this.hitCount = 0;
    }

    public int getSize() {
        return size;
    }

    public Orientation getOrientation() {
        return orientation;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public List<int[]> positions() {
        List<int[]> cells = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            if (orientation == Orientation.HORIZONTAL) {
                cells.add(new int[]{row, col + i});
            } else {
                cells.add(new int[]{row + i, col});
            }
        }
        return cells;
    }

    public boolean contains(int r, int c) {
        for (int[] p : positions()) {
            if (p[0] == r && p[1] == c) {
                return true;
            }
        }
        return false;
    }

    public void registerHit() {
        hitCount = Math.min(hitCount + 1, size);
    }

    public boolean isSunk() {
        return hitCount >= size;
    }
}
