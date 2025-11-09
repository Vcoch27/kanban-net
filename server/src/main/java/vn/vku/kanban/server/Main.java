package vn.vku.kanban.server;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        System.out.println("[Server] Hello. (TCP will listen in next step)");
        TcpServer server = new TcpServer(9000);
        try {
            server.start();
          } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
