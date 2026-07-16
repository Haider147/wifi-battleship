package app.wifibattleship.net;

public interface MessageSender {

    void send(Message message);

    void sendBlocking(Message message);
}
