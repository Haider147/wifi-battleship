package com.wifibattleship.game;

import java.util.Arrays;
import java.util.List;

public class GameConfig {

    public static class ShipConfig {
        private final String name;
        private final int size;
        private int count;

        public ShipConfig(String name, int size, int count) {
            this.name = name;
            this.size = size;
            this.count = count;
        }

        public String getName() { return name; }
        public int getSize() { return size; }
        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }
    }

    private int boardSize;
    private List<ShipConfig> ships;

    public GameConfig() {
        this.boardSize = 10;
        this.ships = defaultShips();
    }

    public GameConfig(int boardSize, List<ShipConfig> ships) {
        this.boardSize = boardSize;
        this.ships = ships;
    }

    public static List<ShipConfig> defaultShips() {
        return Arrays.asList(
            new ShipConfig("Portaaviones", 5, 1),
            new ShipConfig("Acorazado", 4, 1),
            new ShipConfig("Crucero", 3, 1),
            new ShipConfig("Submarino", 3, 1),
            new ShipConfig("Destructor", 2, 1)
        );
    }

    public int getBoardSize() { return boardSize; }
    public void setBoardSize(int boardSize) { this.boardSize = boardSize; }
    public List<ShipConfig> getShips() { return ships; }
    public void setShips(List<ShipConfig> ships) { this.ships = ships; }
}
