package vn.vku.kanban.client;

import com.google.gson.JsonObject;
import com.google.gson.Gson;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public class NetClient implements Closeable {
    private final String host;
    private final int port;
    private final Consumer<String> logger;
    private final Gson gson = new Gson();

    private Socket sock;
    private DataInputStream in;
    private DataOutputStream out;

    public NetClient(String host, int port, Consumer<String> logger) {
        this.host = host;
        this.port = port;
        this.logger = logger;
    }

    public void connect() throws IOException {
        sock = new Socket(host, port);
        in = new DataInputStream(new BufferedInputStream(sock.getInputStream()));
        out = new DataOutputStream(new BufferedOutputStream(sock.getOutputStream()));
    }

    public String sendAndRecv(JsonObject json) throws IOException {
        String s = gson.toJson(json);
        byte[] data = s.getBytes(StandardCharsets.UTF_8);
        out.writeInt(data.length); // 4-byte length (big-endian)
        out.write(data);
        out.flush();
        logger.accept("[SEND] " + s);

        int len = in.readInt();
        if (len <= 0 || len > (1 << 20)) throw new IOException("Invalid resp len: " + len);
        byte[] buf = new byte[len];
        in.readFully(buf);
        String resp = new String(buf, StandardCharsets.UTF_8);
        return resp;
    }

    @Override
    public void close() throws IOException {
        try { if (in != null) in.close(); } catch (Exception ignored) {}
        try { if (out != null) out.close(); } catch (Exception ignored) {}
        try { if (sock != null) sock.close(); } catch (Exception ignored) {}
    }
}
