package app.wifibattleship.game;

import java.util.Random;

import app.wifibattleship.net.Message;
import app.wifibattleship.net.MessageListener;
import app.wifibattleship.net.MessageSender;

public class GameController implements MessageListener {

    public interface Listener {
        void onPhaseChanged(GamePhase phase);

        void onTurnChanged(boolean myTurn);

        void onOpponentReady();

        void onMyBoardChanged();

        void onEnemyBoardChanged();

        void onIncomingAttack(int x, int y);

        void onAttackResult(int x, int y, AttackResult result);

        void onGameOver(boolean iWon);

        void onDisconnected(boolean voluntaryExit);
    }

    private final Board myBoard = new Board();
    private final Board enemyBoard = new Board();
    private final Random random = new Random();

    private Role myRole;
    private GamePhase phase = GamePhase.PLACEMENT;
    private boolean myTurn = false;
    private boolean localReady = false;
    private boolean opponentReady = false;
    private int enemySunkCount = 0;

    private MessageSender sender;
    private volatile Listener listener;

    public void setRole(Role role) {
        if (role == null) {
            throw new IllegalArgumentException("role must not be null");
        }
        this.myRole = role;
    }

    public void setSender(MessageSender sender) {
        this.sender = sender;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void clearListener(Listener expected) {
        if (this.listener == expected) {
            this.listener = null;
        }
    }

    public Board getMyBoard() {
        return myBoard;
    }

    public Board getEnemyBoard() {
        return enemyBoard;
    }

    public boolean isMyTurn() {
        return myTurn;
    }

    public void setLocalReady() {
        if (localReady) {
            return;
        }
        localReady = true;
        send(Message.ready());
        maybeStartGame();
    }

    public void localAttack(int x, int y) {
        if (phase != GamePhase.PLAYING || !myTurn) {
            return;
        }
        if (enemyBoard.wasAlreadyAttacked(x, y)) {
            return;
        }
        myTurn = false;
        notifyTurnChanged();
        send(Message.attack(x, y));
    }

    public void leave() {
        if (sender != null) {
            sender.sendBlocking(Message.bye());
        }
    }

    @Override
    public void onMessageReceived(Message msg) {
        if (msg == null) {
            return;
        }
        switch (msg.getType()) {
            case READY -> {
                opponentReady = true;
                notifyOpponentReady();
                maybeStartGame();
            }
            case FIRST_TURN -> handleFirstTurn(msg.getRole());
            case ATTACK -> handleIncomingAttack(msg.getX(), msg.getY());
            case RESULT -> handleAttackResult(msg.getX(), msg.getY(), msg.getResult(), msg.getCells());
            case GAMEOVER -> handleGameOver(msg.getWinner());
            case BYE -> notifyDisconnected(true);
            default -> {}
        }
    }

    @Override
    public void onDisconnected() {
        notifyDisconnected(false);
    }

    private void maybeStartGame() {
        if (phase != GamePhase.PLACEMENT || myRole == null) {
            return;
        }
        if (!localReady || !opponentReady) {
            return;
        }
        if (myRole == Role.HOST) {
            Role starter = random.nextBoolean() ? Role.HOST : Role.CLIENT;
            send(Message.firstTurn(starter.name()));
            applyFirstTurn(starter);
        }
    }

    private void handleFirstTurn(String role) {
        if (phase != GamePhase.PLACEMENT || myRole != Role.CLIENT) {
            return;
        }
        Role starter = parseRole(role);
        if (starter == null) {
            starter = Role.HOST;
        }
        applyFirstTurn(starter);
    }

    private void applyFirstTurn(Role starter) {
        if (phase != GamePhase.PLACEMENT) {
            return;
        }
        myTurn = (starter == myRole);
        phase = GamePhase.PLAYING;
        notifyPhaseChanged();
        notifyTurnChanged();
    }

    private void handleIncomingAttack(int x, int y) {
        if (phase != GamePhase.PLAYING) {
            return;
        }
        AttackResult result = myBoard.receiveAttack(x, y);
        if (result == AttackResult.SUNK) {
            send(Message.result(x, y, result.name(), myBoard.getShipPositionsAt(x, y)));
        } else {
            send(Message.result(x, y, result.name()));
        }
        notifyIncomingAttack(x, y);
        notifyMyBoardChanged();

        if (myBoard.allSunk()) {
            phase = GamePhase.ENDED;
            notifyPhaseChanged();
            Role winner = (myRole == Role.HOST) ? Role.CLIENT : Role.HOST;
            send(Message.gameOver(winner.name()));
            notifyGameOver(false);
        } else {
            myTurn = true;
            notifyTurnChanged();
        }
    }

    private void handleAttackResult(int x, int y, String resultStr, java.util.List<int[]> cells) {
        if (phase != GamePhase.PLAYING) {
            return;
        }
        AttackResult result = parseResult(resultStr);
        if (result == null) {
            result = AttackResult.WATER;
        }
        if (enemyBoard.wasAlreadyAttacked(x, y)) {
            return;
        }
        enemyBoard.markShotResult(x, y, result);
        if (result == AttackResult.SUNK && cells != null) {
            for (int[] p : cells) {
                enemyBoard.markShotResult(p[0], p[1], AttackResult.SUNK);
            }
        }
        notifyAttackResult(x, y, result);
        notifyEnemyBoardChanged();

        if (result == AttackResult.SUNK) {
            enemySunkCount++;
            if (enemySunkCount >= GameConfig.TOTAL_SHIPS) {
                phase = GamePhase.ENDED;
                notifyPhaseChanged();
                notifyGameOver(true);
            }
        }
    }

    private void handleGameOver(String winnerStr) {
        if (phase == GamePhase.ENDED) {
            return;
        }
        phase = GamePhase.ENDED;
        notifyPhaseChanged();
        Role winner = parseRole(winnerStr);
        boolean iWon = (winner == myRole);
        notifyGameOver(iWon);
    }

    private void send(Message msg) {
        if (sender != null) {
            sender.send(msg);
        }
    }

    private Role parseRole(String s) {
        if (s == null) {
            return null;
        }
        try {
            return Role.valueOf(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private AttackResult parseResult(String s) {
        if (s == null) {
            return null;
        }
        try {
            return AttackResult.valueOf(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private void notifyPhaseChanged() {
        Listener l = listener;
        if (l != null) {
            l.onPhaseChanged(phase);
        }
    }

    private void notifyTurnChanged() {
        Listener l = listener;
        if (l != null) {
            l.onTurnChanged(myTurn);
        }
    }

    private void notifyOpponentReady() {
        Listener l = listener;
        if (l != null) {
            l.onOpponentReady();
        }
    }

    private void notifyMyBoardChanged() {
        Listener l = listener;
        if (l != null) {
            l.onMyBoardChanged();
        }
    }

    private void notifyEnemyBoardChanged() {
        Listener l = listener;
        if (l != null) {
            l.onEnemyBoardChanged();
        }
    }

    private void notifyIncomingAttack(int x, int y) {
        Listener l = listener;
        if (l != null) {
            l.onIncomingAttack(x, y);
        }
    }

    private void notifyAttackResult(int x, int y, AttackResult result) {
        Listener l = listener;
        if (l != null) {
            l.onAttackResult(x, y, result);
        }
    }

    private void notifyGameOver(boolean iWon) {
        Listener l = listener;
        if (l != null) {
            l.onGameOver(iWon);
        }
    }

    private void notifyDisconnected(boolean voluntaryExit) {
        if (phase == GamePhase.ENDED) {
            return;
        }
        phase = GamePhase.ENDED;
        notifyPhaseChanged();
        Listener l = listener;
        if (l != null) {
            l.onDisconnected(voluntaryExit);
        }
    }
}
