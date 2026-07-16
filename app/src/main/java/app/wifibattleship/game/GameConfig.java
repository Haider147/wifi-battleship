package app.wifibattleship.game;

public final class GameConfig {

    public static final int BOARD_SIZE = 8;
    public static final int[] SHIP_SIZES = {4, 3, 2};
    public static final int TOTAL_SHIPS = SHIP_SIZES.length;
    public static final int SERVICE_PORT = 50556;
    public static final String SERVICE_TYPE = "_wifibattleship._tcp";
    public static final String SERVICE_PREFIX = "wifibattleship-";
    public static final String TXT_KEY_PORT = "port";

    private GameConfig() {
    }
}
