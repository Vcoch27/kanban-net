package vn.vku.kanban.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;

public class TcpServer {
    private final int port;
    private volatile boolean running = true;
    private final ExecutorService pool = Executors.newCachedThreadPool();

    public TcpServer(int port) { this.port = port; }

    public void start() throws IOException {
        try (ServerSocket server = new ServerSocket(port)) {
            log("LISTEN", "TCP " + port);
            while (running) {
                Socket sock = server.accept();
                log("ACCEPT", sock.getRemoteSocketAddress().toString());
                pool.submit(new ClientHandler(sock));
            }
        }
    }

    public void stop() {
        running = false;
        pool.shutdownNow();
        log("STOP", "server stopping");
    }

    static void log(String tag, String msg) {
        System.out.printf("[%s] %s%n", tag, msg);
    }
}
