package com.wifibattleship.ui.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.wifibattleship.game.GameConfig;
import com.wifibattleship.game.GameEngine;

public class GameViewModel extends ViewModel {

    private final MutableLiveData<String> gameOver = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isMyTurn = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> bothPlayersReady = new MutableLiveData<>(false);

    private GameEngine engine;

    public LiveData<String> getGameOver() { return gameOver; }
    public LiveData<Boolean> getIsMyTurn() { return isMyTurn; }
    public LiveData<Boolean> getBothPlayersReady() { return bothPlayersReady; }

    public void initGame(GameConfig config, boolean isHost) {
        engine = new GameEngine(config);
        isMyTurn.setValue(isHost);
    }

    public void onLocalAttack(int row, int col) {
        if (engine == null) return;
        engine.processLocalAttack(row, col);
        // TODO: send ATTACK message via ConnectionManager
        checkGameOver();
        isMyTurn.setValue(false);
    }

    public void onRemoteAttack(int row, int col) {
        if (engine == null) return;
        engine.processEnemyAttack(row, col);
        // TODO: send ATTACK_RESULT message via ConnectionManager
        checkGameOver();
        isMyTurn.setValue(true);
    }

    public void onRemoteReady() {
        bothPlayersReady.setValue(true);
    }

    public GameEngine getEngine() { return engine; }

    private void checkGameOver() {
        String winner = engine.isGameOver();
        if (winner != null) gameOver.setValue(winner);
    }
}
