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
    private final ServerSocket hostServerSocket;

    private volatile BufferedWriter writer;
    private Thread readerThread;
    private volatile boolean started;

    public GameConnection(Socket socket, MessageListener listener) {
        this(socket, listener, null);
    }

    public GameConnection(Socket socket, MessageListener listener, ServerSocket hostServerSocket) {
        if (socket == null || !socket.isConnected()) {
            throw new IllegalArgumentException("socket is not connected");
        }
        this.socket = socket;
        this.listener = listener;
        this.hostServerSocket = hostServerSocket;
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
        if (started) {
            return;
        }
        started = true;
        BufferedWriter w;
        try {
            w = new BufferedWriter(new OutputStreamWriter(
                    socket.getOutputStream(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            notifyDisconnected();
            return;
        }
        writer = w;
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
                final Message msg;
                try {
                    msg = Message.fromJson(line);
                } catch (Exception e) {
                    main.post(this::notifyDisconnected);
                    return;
                }
                final MessageListener l = listener;
                if (l != null) {
                    main.post(() -> {
                        MessageListener current = listener;
                        if (current != null) {
                            current.onMessageReceived(msg);
                        }
                    });
                }
            }
        } catch (IOException e) {
        } finally {
            notifyDisconnected();
        }
    }

    @Override
    public void send(Message message) {
        if (closed.get() || message == null) {
            return;
        }
        final BufferedWriter w = writer;
        if (w == null) {
            return;
        }
        final String line = message.toJson();
        synchronized (this) {
            if (closed.get()) {
                return;
            }
            try {
                w.write(line);
                w.write('\n');
                w.flush();
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
        if (hostServerSocket != null && !hostServerSocket.isClosed()) {
            try {
                hostServerSocket.close();
            } catch (IOException ignored) {
            }
        }
        if (readerThread != null) {
            readerThread.interrupt();
        }
    }

    private void notifyDisconnected() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        try {
            if (!socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
        if (hostServerSocket != null && !hostServerSocket.isClosed()) {
            try {
                hostServerSocket.close();
            } catch (IOException ignored) {
            }
        }
        main.post(() -> {
            MessageListener current = listener;
            if (current != null) {
                current.onDisconnected();
            }
        });
    }

    public static void acceptAsHost(final ServerSocket serverSocket,
                                    final ConnectCallback callback) {
        Thread t = new Thread(() -> {
            try {
                serverSocket.setSoTimeout(0);
                Socket s = serverSocket.accept();
                mainPost(() -> callback.onConnected(new GameConnection(s, null, serverSocket)));
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
                mainPost(() -> callback.onConnected(new GameConnection(s, null, null)));
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
