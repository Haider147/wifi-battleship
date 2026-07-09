package app.wifibattleship.net;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

public class Message {

    private static final String TAG = "WbsMessage";
    private static final String FIELD_TYPE = "type";
    private static final String FIELD_X = "x";
    private static final String FIELD_Y = "y";
    private static final String FIELD_RESULT = "result";
    private static final String FIELD_ROLE = "role";
    private static final String FIELD_WINNER = "winner";

    private final MessageType type;
    private int x = -1;
    private int y = -1;
    private String result;
    private String role;
    private String winner;

    private Message(MessageType type) {
        this.type = type;
    }

    public static Message ready() {
        return new Message(MessageType.READY);
    }

    public static Message attack(int x, int y) {
        if (x < 0 || x >= 8 || y < 0 || y >= 8) {
            throw new IllegalArgumentException("coords out of board: " + x + "," + y);
        }
        Message m = new Message(MessageType.ATTACK);
        m.x = x;
        m.y = y;
        return m;
    }

    public static Message result(int x, int y, String result) {
        if (x < 0 || x >= 8 || y < 0 || y >= 8) {
            throw new IllegalArgumentException("coords out of board: " + x + "," + y);
        }
        Message m = new Message(MessageType.RESULT);
        m.x = x;
        m.y = y;
        m.result = result;
        return m;
    }

    public static Message firstTurn(String role) {
        Message m = new Message(MessageType.FIRST_TURN);
        m.role = role;
        return m;
    }

    public static Message gameOver(String winner) {
        Message m = new Message(MessageType.GAMEOVER);
        m.winner = winner;
        return m;
    }

    public static Message bye() {
        return new Message(MessageType.BYE);
    }

    public static Message fromJson(String json) throws JSONException {
        JSONObject o = new JSONObject(json);
        MessageType type = MessageType.fromCode(o.optString(FIELD_TYPE));
        if (type == null) {
            throw new JSONException("Unknown message type: " + json);
        }
        Message m = new Message(type);
        if (o.has(FIELD_X)) m.x = o.optInt(FIELD_X, -1);
        if (o.has(FIELD_Y)) m.y = o.optInt(FIELD_Y, -1);
        if (o.has(FIELD_RESULT)) m.result = o.optString(FIELD_RESULT, null);
        if (o.has(FIELD_ROLE)) m.role = o.optString(FIELD_ROLE, null);
        if (o.has(FIELD_WINNER)) m.winner = o.optString(FIELD_WINNER, null);
        if (m.x != -1 && (m.x < 0 || m.x >= 8)) {
            throw new JSONException("x out of range: " + m.x);
        }
        if (m.y != -1 && (m.y < 0 || m.y >= 8)) {
            throw new JSONException("y out of range: " + m.y);
        }
        return m;
    }

    public String toJson() {
        JSONObject o = new JSONObject();
        try {
            o.put(FIELD_TYPE, type.code());
            if (x >= 0) o.put(FIELD_X, x);
            if (y >= 0) o.put(FIELD_Y, y);
            if (result != null) o.put(FIELD_RESULT, result);
            if (role != null) o.put(FIELD_ROLE, role);
            if (winner != null) o.put(FIELD_WINNER, winner);
        } catch (JSONException e) {
            Log.e(TAG, "toJson error", e);
        }
        return o.toString();
    }

    public MessageType getType() {
        return type;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public String getResult() {
        return result;
    }

    public String getRole() {
        return role;
    }

    public String getWinner() {
        return winner;
    }

    @Override
    public String toString() {
        return toJson();
    }
}
