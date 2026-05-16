package com.wifibattleship.game;

public class GameEngine {

    public final GameConfig config;
    public final Board localBoard;
    public final Board enemyBoard;
    public boolean isLocalTurn;

    public GameEngine(GameConfig config) {
        this.config = config;
        this.localBoard = new Board(config.getBoardSize());
        this.enemyBoard = new Board(config.getBoardSize());
    }

    public Board.CellState processLocalAttack(int row, int col) {
        return enemyBoard.receiveAttack(row, col);
    }

    public Board.CellState processEnemyAttack(int row, int col) {
        return localBoard.receiveAttack(row, col);
    }

    /** Returns "LOCAL", "REMOTE", or null if game continues. */
    public String isGameOver() {
        if (enemyBoard.allSunk()) return "LOCAL";
        if (localBoard.allSunk()) return "REMOTE";
        return null;
    }
}
