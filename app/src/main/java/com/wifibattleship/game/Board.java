package com.wifibattleship.game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Board {

    public enum CellState { EMPTY, SHIP, HIT, MISS, SUNK }

    public final int size;
    public final CellState[][] grid;
    private final List<Ship> ships = new ArrayList<>();

    public Board(int size) {
        this.size = size;
        this.grid = new CellState[size][size];
        for (CellState[] row : grid) Arrays.fill(row, CellState.EMPTY);
    }

    public boolean placeShip(Ship ship) {
        if (!isValidPlacement(ship)) return false;
        ships.add(ship);
        for (int[] cell : ship.getCells()) {
            grid[cell[0]][cell[1]] = CellState.SHIP;
        }
        return true;
    }

    public CellState receiveAttack(int row, int col) {
        switch (grid[row][col]) {
            case SHIP: {
                Ship hit = null;
                for (Ship s : ships) {
                    if (s.occupies(row, col)) { hit = s; break; }
                }
                hit.hit(row, col);
                if (hit.isSunk()) {
                    for (int[] cell : hit.getCells()) grid[cell[0]][cell[1]] = CellState.SUNK;
                    return CellState.SUNK;
                } else {
                    grid[row][col] = CellState.HIT;
                    return CellState.HIT;
                }
            }
            case EMPTY:
                grid[row][col] = CellState.MISS;
                return CellState.MISS;
            default:
                return grid[row][col];
        }
    }

    public boolean allSunk() {
        if (ships.isEmpty()) return false;
        for (Ship s : ships) { if (!s.isSunk()) return false; }
        return true;
    }

    public void reset() {
        ships.clear();
        for (CellState[] row : grid) Arrays.fill(row, CellState.EMPTY);
    }

    private boolean isValidPlacement(Ship ship) {
        for (int[] cell : ship.getCells()) {
            int r = cell[0], c = cell[1];
            if (r < 0 || r >= size || c < 0 || c >= size || grid[r][c] != CellState.EMPTY) return false;
        }
        return true;
    }
}
