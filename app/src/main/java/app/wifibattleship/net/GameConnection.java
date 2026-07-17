package app.wifibattleship.net;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
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
    private volatile long lastReceivedMs;
    private Thread heartbeatThread;
    private static final long PING_INTERVAL_MS = 2000;
    private static final long TIMEOUT_MS = 6000;
    private final ExecutorService sendExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "wbs-writer");
        t.setDaemon(true);
        return t;
    });

    public GameConnection(Socket socket, @Nullable MessageListener listener,
                          @Nullable ServerSocket hostServerSocket) {
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
        lastReceivedMs = System.currentTimeMillis();
        readerThread = new Thread(this::readerLoop, "wbs-reader");
        readerThread.setDaemon(true);
        readerThread.start();
        heartbeatThread = new Thread(this::heartbeatLoop, "wbs-heartbeat");
        heartbeatThread.setDaemon(true);
        heartbeatThread.start();
    }

    @SuppressWarnings("BusyWait")
    private void heartbeatLoop() {
        while (!closed.get()) {
            try {
                Thread.sleep(PING_INTERVAL_MS);
            } catch (InterruptedException e) {
                return;
            }
            if (closed.get()) {
                return;
            }
            if (System.currentTimeMillis() - lastReceivedMs > TIMEOUT_MS) {
                Log.w("wbs-heartbeat", "no data received in " + TIMEOUT_MS + "ms, closing");
                notifyDisconnected();
                return;
            }
            send(Message.ping());
        }
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
                    Log.w("wbs-reader", "skip malformed line", e);
                    continue;
                }
                lastReceivedMs = System.currentTimeMillis();
                if (msg.getType() == MessageType.PING) {
                    send(Message.pong());
                    continue;
                }
                if (msg.getType() == MessageType.PONG) {
                    continue;
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
        } catch (IOException ignored) {
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
        try {
            sendExecutor.execute(() -> {
                try {
                    w.write(line);
                    w.write('\n');
                    w.flush();
                } catch (IOException e) {
                    notifyDisconnected();
                }
            });
        } catch (RejectedExecutionException ignored) {
        }
    }

    @Override
    public void sendBlocking(Message message) {
        if (closed.get() || message == null) {
            return;
        }
        final BufferedWriter w = writer;
        if (w == null) {
            return;
        }
        try {
            synchronized (w) {
                w.write(message.toJson());
                w.write('\n');
                w.flush();
            }
        } catch (IOException ignored) {
        }
    }

    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        try {
            sendExecutor.execute(this::closeSockets);
        } catch (RejectedExecutionException e) {
            closeSockets();
        }
        sendExecutor.shutdown();
        if (readerThread != null) {
            readerThread.interrupt();
        }
        if (heartbeatThread != null) {
            heartbeatThread.interrupt();
        }
    }

    private void closeSockets() {
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
    }

    private void notifyDisconnected() {
        boolean wasOpen = closed.compareAndSet(false, true);
        if (wasOpen) {
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
            sendExecutor.shutdown();
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
            Socket s = new Socket();
            try {
                s.connect(new InetSocketAddress(host, port), 5000);
            } catch (IOException e) {
                try {
                    s.close();
                } catch (IOException ignored) {
                }
                mainPost(() -> callback.onFailed(e.getMessage()));
                return;
            }
            mainPost(() -> callback.onConnected(new GameConnection(s, null, null)));
        }, "wbs-client-connect");
        t.setDaemon(true);
        t.start();
    }

    private static void mainPost(Runnable r) {
        new Handler(Looper.getMainLooper()).post(r);
    }
}
