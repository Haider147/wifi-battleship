package app.wifibattleship.net;

import android.os.Handler;
import android.os.Looper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

public class GameConnection implements MessageSender {

    public interface ConnectCallback {
        void onConnected(GameConnection connection);

        void onFailed(String reason);
    }

    private final Socket socket;
    private volatile MessageListener listener;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final AtomicBoolean closed = new AtomicBoolean(false);

    private BufferedWriter writer;
    private Thread readerThread;

    public GameConnection(Socket socket, MessageListener listener) {
        if (socket == null || !socket.isConnected()) {
            throw new IllegalArgumentException("socket is not connected");
        }
        this.socket = socket;
        this.listener = listener;
        try {
            this.socket.setKeepAlive(true);
            this.socket.setTcpNoDelay(true);
        } catch (IOException ignored) {
        }
    }

    public void setListener(MessageListener listener) {
        this.listener = listener;
    }

    public void start() {
        try {
            writer = new BufferedWriter(new OutputStreamWriter(
                    socket.getOutputStream(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            notifyDisconnected();
            return;
        }
        readerThread = new Thread(this::readerLoop, "wbs-reader");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    private void readerLoop() {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new InputStreamReader(
                    socket.getInputStream(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            notifyDisconnected();
            return;
        }
        try {
            String line;
            while (!closed.get()) {
                line = reader.readLine();
                if (line == null) {
                    break;
                }
                if (line.isEmpty()) {
                    continue;
                }
                try {
                    final Message msg = Message.fromJson(line);
                    main.post(() -> {
                        if (listener != null) {
                            listener.onMessageReceived(msg);
                        }
                    });
                } catch (Exception e) {
                    main.post(this::notifyDisconnected);
                    return;
                }
            }
        } catch (IOException e) {
            // socket closed or broken
        } finally {
            notifyDisconnected();
        }
    }

    @Override
    public void send(Message message) {
        if (closed.get() || message == null) {
            return;
        }
        final String line = message.toJson();
        synchronized (this) {
            if (writer == null) {
                return;
            }
            try {
                writer.write(line);
                writer.write('\n');
                writer.flush();
            } catch (IOException e) {
                notifyDisconnected();
            }
        }
    }

    public boolean isClosed() {
        return closed.get();
    }

    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        try {
            if (!socket.isClosed()) {
                socket.shutdownInput();
                socket.shutdownOutput();
                socket.close();
            }
        } catch (IOException ignored) {
        }
        if (readerThread != null) {
            readerThread.interrupt();
        }
    }

    private void notifyDisconnected() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        main.post(() -> {
            if (listener != null) {
                listener.onDisconnected();
            }
        });
    }

    public static void acceptAsHost(final ServerSocket serverSocket,
                                    final ConnectCallback callback) {
        Thread t = new Thread(() -> {
            try {
                serverSocket.setSoTimeout(0);
                Socket s = serverSocket.accept();
                mainPost(() -> callback.onConnected(new GameConnection(s, null)));
            } catch (IOException e) {
                mainPost(() -> callback.onFailed(e.getMessage()));
            }
        }, "wbs-host-accept");
        t.setDaemon(true);
        t.start();
    }

    public static void connectAsClient(final String host,
                                       final int port,
                                       final ConnectCallback callback) {
        Thread t = new Thread(() -> {
            try {
                Socket s = new Socket();
                s.connect(new InetSocketAddress(host, port), 5000);
                mainPost(() -> callback.onConnected(new GameConnection(s, null)));
            } catch (IOException e) {
                mainPost(() -> callback.onFailed(e.getMessage()));
            }
        }, "wbs-client-connect");
        t.setDaemon(true);
        t.start();
    }

    private static void mainPost(Runnable r) {
        new Handler(Looper.getMainLooper()).post(r);
    }
}
