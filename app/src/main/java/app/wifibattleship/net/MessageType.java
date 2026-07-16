package app.wifibattleship.net;

import java.util.HashMap;
import java.util.Map;

public enum MessageType {
    READY("READY"),
    ATTACK("ATTACK"),
    RESULT("RESULT"),
    FIRST_TURN("FIRST_TURN"),
    GAMEOVER("GAMEOVER"),
    BYE("BYE"),
    PING("PING"),
    PONG("PONG");

    private final String code;

    MessageType(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    private static final Map<String, MessageType> BY_CODE = new HashMap<>();
    static {
        for (MessageType t : values()) {
            BY_CODE.put(t.code, t);
        }
    }

    public static MessageType fromCode(String code) {
        if (code == null) {
            return null;
        }
        return BY_CODE.get(code);
    }
}
