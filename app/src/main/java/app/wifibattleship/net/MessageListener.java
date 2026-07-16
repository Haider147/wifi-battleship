package app.wifibattleship.net;

public interface MessageListener {

    void onMessageReceived(Message message);

    void onDisconnected(boolean peerTimedOut);
}
