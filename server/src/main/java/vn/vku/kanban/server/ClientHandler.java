package vn.vku.kanban.server;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import vn.vku.kanban.server.session.ClientSession;
import vn.vku.kanban.server.session.BroadcastHub;

public class ClientHandler implements Runnable {
    private final Socket sock;
    private final Router router;
    private final BroadcastHub hub;

    public ClientHandler(Socket sock, Router router, BroadcastHub hub) {
        this.sock = sock; this.router = router; this.hub = hub;
    }

    @Override
    public void run() {
        try (sock;
             DataInputStream in = new DataInputStream(new BufferedInputStream(sock.getInputStream()));
             DataOutputStream out = new DataOutputStream(new BufferedOutputStream(sock.getOutputStream()))) {

            ClientSession session = new ClientSession(respStr -> {
                try {
                    byte[] bytes = respStr.getBytes(StandardCharsets.UTF_8);
                    synchronized (out) {
                        out.writeInt(bytes.length);
                        out.write(bytes);
                        out.flush();
                    }
                } catch (IOException ignored) {}
            });
            hub.add(session);

            while (true) {
                int len;
                try { len = in.readInt(); }
                catch (EOFException eof) { TcpServer.log("CLOSE", sock.getRemoteSocketAddress().toString()); break; }

                if (len <= 0 || len > (1<<20)) break;
                byte[] buf = new byte[len];
                in.readFully(buf);
                String json = new String(buf, StandardCharsets.UTF_8);
                TcpServer.log("RECV", json);

                String resp = router.handle(session, json);
                byte[] outBytes = resp.getBytes(StandardCharsets.UTF_8);
                out.writeInt(outBytes.length);
                out.write(outBytes);
                out.flush();
                TcpServer.log("SEND", resp);
            }

            hub.remove(session);
        } catch (IOException e) {
            TcpServer.log("ERR", "IO " + e.getMessage());
        }
    }
}
