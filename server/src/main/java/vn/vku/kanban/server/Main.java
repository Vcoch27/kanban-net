package vn.vku.kanban.server;

import java.io.IOException;
import vn.vku.kanban.server.db.DB;
import vn.vku.kanban.server.session.BroadcastHub;
import vn.vku.kanban.server.store.Store;

public class Main {
    public static void main(String[] args) {
        System.out.println("[Server] Hello. Starting...");

        try {
            DB.connect(); // Khởi tạo kết nối DB
        } catch (Exception e) {
            System.err.println("[FATAL] Database connection failed. Server cannot start.");
            e.printStackTrace();
            return; // Dừng server nếu không kết nối được DB
        }

        var store = new Store();
        var hub = new BroadcastHub();
        var router = new Router(store, hub);
        var server = new TcpServer(9000, router, hub);

        try {
            server.start();
        } catch (IOException e) {
            System.err.println("[FATAL] Failed to start TCP server.");
            e.printStackTrace();
        } finally {
            DB.close(); // Đóng kết nối khi server dừng
            System.out.println("[Server] Shutdown complete.");
        }
    }
}
