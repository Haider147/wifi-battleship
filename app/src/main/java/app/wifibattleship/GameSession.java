package app.wifibattleship;

import app.wifibattleship.game.GameController;
import app.wifibattleship.game.Role;
import app.wifibattleship.net.GameConnection;
import app.wifibattleship.net.WifiDirectHelper;

import java.net.ServerSocket;

public final class GameSession {

    private static GameSession instance;

    private Role role;
    private GameConnection connection;
    private GameController controller;
    private WifiDirectHelper wifiDirectHelper;
    private ServerSocket serverSocket;
    private String serviceName;

    private GameSession() {
    }

    public static GameSession get() {
        if (instance == null) {
            instance = new GameSession();
        }
        return instance;
    }

    public static void reset() {
        if (instance != null) {
            instance.clear();
            instance = null;
        }
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public GameConnection getConnection() {
        return connection;
    }

    public void setConnection(GameConnection connection) {
        this.connection = connection;
    }

    public GameController getController() {
        if (controller == null) {
            controller = new GameController();
            controller.setRole(role);
            if (connection != null) {
                controller.setSender(connection);
                connection.setListener(controller);
            }
        }
        return controller;
    }

    public WifiDirectHelper getWifiDirectHelper(android.content.Context context) {
        if (wifiDirectHelper == null) {
            wifiDirectHelper = new WifiDirectHelper(context.getApplicationContext());
        }
        return wifiDirectHelper;
    }

    public ServerSocket getServerSocket() {
        return serverSocket;
    }

    public void setServerSocket(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public void clear() {
        if (connection != null) {
            connection.close();
            connection = null;
        }
        if (wifiDirectHelper != null) {
            wifiDirectHelper.teardown();
            wifiDirectHelper = null;
        }
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (Exception ignored) {
            }
            serverSocket = null;
        }
        controller = null;
        connection = null;
        role = null;
        serviceName = null;
    }
}
