package vn.vku.kanban.server;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ClientHandler implements Runnable {
    private final Socket sock;

    public ClientHandler(Socket sock) { this.sock = sock; }

    @Override
    public void run() {
        try (sock;
             DataInputStream in = new DataInputStream(new BufferedInputStream(sock.getInputStream()));
             DataOutputStream out = new DataOutputStream(new BufferedOutputStream(sock.getOutputStream()))) {

            while (true) {
                // Đọc frame: 4 byte length (big-endian), rồi n byte JSON
                int len;
                try {
                    len = in.readInt(); // big-endian theo chuẩn DataInputStream
                } catch (EOFException eof) {
                    TcpServer.log("CLOSE", sock.getRemoteSocketAddress().toString());
                    break;
                }
                if (len <= 0 || len > (1 << 20)) { // giới hạn 1MB để an toàn
                    TcpServer.log("ERR", "Invalid frame length: " + len);
                    break;
                }
                byte[] buf = new byte[len];
                in.readFully(buf);
                String json = new String(buf, StandardCharsets.UTF_8);
                TcpServer.log("RECV", json);

                // Echo lại: gói phản hồi dạng {"ok":true,"echo":<json gốc>}
                String resp = "{\"ok\":true,\"echo\":" + json + "}";
                byte[] outBytes = resp.getBytes(StandardCharsets.UTF_8);
                out.writeInt(outBytes.length);
                out.write(outBytes);
                out.flush();
                TcpServer.log("SEND", resp);
            }

        } catch (IOException e) {
            TcpServer.log("ERR", "IO " + e.getMessage());
        }
    }
}
