package vn.vku.kanban.server;

import java.io.IOException;
import vn.vku.kanban.server.session.BroadcastHub;
import vn.vku.kanban.server.store.Store;

public class Main {
    public static void main(String[] args) {
        System.out.println("[Server] Hello. (TCP will listen in next step)");
        var store = new Store();
        var hub = new BroadcastHub();
        var router = new Router(store, hub);
        TcpServer server = new TcpServer(9000, router, hub);
        try {
            server.start();
          } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
