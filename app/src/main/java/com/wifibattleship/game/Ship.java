package com.wifibattleship.game;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Ship {

    public enum Orientation { HORIZONTAL, VERTICAL }

    private final String name;
    private final int size;
    private final int startRow;
    private final int startCol;
    private Orientation orientation;
    private final Set<String> hitCells = new HashSet<>();

    public Ship(String name, int size, int startRow, int startCol, Orientation orientation) {
        this.name = name;
        this.size = size;
        this.startRow = startRow;
        this.startCol = startCol;
        this.orientation = orientation;
    }

    public List<int[]> getCells() {
        List<int[]> cells = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            if (orientation == Orientation.HORIZONTAL) {
                cells.add(new int[]{startRow, startCol + i});
            } else {
                cells.add(new int[]{startRow + i, startCol});
            }
        }
        return cells;
    }

    public boolean occupies(int row, int col) {
        for (int[] cell : getCells()) {
            if (cell[0] == row && cell[1] == col) return true;
        }
        return false;
    }

    public void hit(int row, int col) { hitCells.add(row + "," + col); }

    public boolean isSunk() { return hitCells.size() == size; }

    public String getName() { return name; }
    public int getSize() { return size; }
    public Orientation getOrientation() { return orientation; }
    public void setOrientation(Orientation orientation) { this.orientation = orientation; }
}
