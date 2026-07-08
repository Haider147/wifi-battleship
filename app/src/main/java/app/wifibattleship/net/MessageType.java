package app.wifibattleship.net;

public enum MessageType {
    READY("READY"),
    ATTACK("ATTACK"),
    RESULT("RESULT"),
    FIRST_TURN("FIRST_TURN"),
    GAMEOVER("GAMEOVER"),
    BYE("BYE");

    private final String code;

    MessageType(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static MessageType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (MessageType t : values()) {
            if (t.code.equals(code)) {
                return t;
            }
        }
        return null;
    }
}
