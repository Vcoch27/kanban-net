package vn.vku.kanban.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;
import vn.vku.kanban.server.session.BroadcastHub;
import vn.vku.kanban.server.store.Store;

public class TcpServer {
    private final int port;
    private volatile boolean running = true;
    private final ExecutorService pool = Executors.newCachedThreadPool();
    private final Router router;
    private final BroadcastHub hub;

    public TcpServer(int port, Router router, BroadcastHub hub){
        this.port = port; this.router = router; this.hub = hub;
    }

    public void start() throws IOException {
        try (ServerSocket server = new ServerSocket(port)) {
            log("LISTEN", "TCP " + port);
            while (running) {
                Socket sock = server.accept();
                log("ACCEPT", sock.getRemoteSocketAddress().toString());
                pool.submit(new ClientHandler(sock, router, hub));
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
