package app.wifibattleship.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Board {

    public static final int SIZE = 8;

    private final Cell[][] grid = new Cell[SIZE][SIZE];
    private final List<Ship> ships = new ArrayList<>();

    public Board() {
        reset();
    }

    public void reset() {
        ships.clear();
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                grid[r][c] = Cell.WATER;
            }
        }
    }

    public Cell getCell(int r, int c) {
        return grid[r][c];
    }

    public List<Ship> getShips() {
        return Collections.unmodifiableList(ships);
    }

    public boolean isValidPlacement(int row, int col, int size, Orientation orientation) {
        if (size <= 0) {
            return false;
        }
        for (int i = 0; i < size; i++) {
            int r = orientation == Orientation.HORIZONTAL ? row : row + i;
            int c = orientation == Orientation.HORIZONTAL ? col + i : col;
            if (r < 0 || r >= SIZE || c < 0 || c >= SIZE) {
                return false;
            }
            if (grid[r][c] != Cell.WATER) {
                return false;
            }
        }
        return true;
    }

    public boolean placeShip(int size, Orientation orientation, int row, int col) {
        if (!isValidPlacement(row, col, size, orientation)) {
            return false;
        }
        Ship ship = new Ship(size, orientation, row, col);
        for (int[] p : ship.positions()) {
            grid[p[0]][p[1]] = Cell.SHIP;
        }
        ships.add(ship);
        return true;
    }

    public void removeShipAt(int r, int c) {
        Ship target = null;
        for (Ship s : ships) {
            if (s.contains(r, c)) {
                target = s;
                break;
            }
        }
        if (target == null) {
            return;
        }
        for (int[] p : target.positions()) {
            grid[p[0]][p[1]] = Cell.WATER;
        }
        ships.remove(target);
    }

    public int getShipSizeAt(int r, int c) {
        for (Ship s : ships) {
            if (s.contains(r, c)) {
                return s.getSize();
            }
        }
        return 0;
    }

    public AttackResult receiveAttack(int r, int c) {
        if (r < 0 || r >= SIZE || c < 0 || c >= SIZE) {
            return AttackResult.WATER;
        }
        Cell cell = grid[r][c];
        if (cell == Cell.SHIP) {
            grid[r][c] = Cell.HIT;
            Ship target = findShip(r, c);
            if (target != null) {
                target.registerHit();
                if (target.isSunk()) {
                    for (int[] p : target.positions()) {
                        grid[p[0]][p[1]] = Cell.SUNK;
                    }
                    return AttackResult.SUNK;
                }
            }
            return AttackResult.HIT;
        } else if (cell == Cell.WATER) {
            grid[r][c] = Cell.MISS;
            return AttackResult.WATER;
        } else {
            return AttackResult.WATER;
        }
    }

    public boolean allSunk() {
        if (ships.isEmpty()) {
            return false;
        }
        for (Ship s : ships) {
            if (!s.isSunk()) {
                return false;
            }
        }
        return true;
    }

    public void markShotResult(int r, int c, AttackResult result) {
        if (r < 0 || r >= SIZE || c < 0 || c >= SIZE) {
            return;
        }
        switch (result) {
            case WATER -> grid[r][c] = Cell.MISS;
            case HIT -> grid[r][c] = Cell.HIT;
            case SUNK -> grid[r][c] = Cell.SUNK;
        }
    }

    public boolean wasAlreadyAttacked(int r, int c) {
        if (r < 0 || r >= SIZE || c < 0 || c >= SIZE) {
            return false;
        }
        Cell cell = grid[r][c];
        return cell == Cell.MISS || cell == Cell.HIT || cell == Cell.SUNK;
    }

    public List<int[]> getShipPositionsAt(int r, int c) {
        Ship s = findShip(r, c);
        if (s == null) {
            return Collections.emptyList();
        }
        return s.positions();
    }

    private Ship findShip(int r, int c) {
        for (Ship s : ships) {
            if (s.contains(r, c)) {
                return s;
            }
        }
        return null;
    }
}
