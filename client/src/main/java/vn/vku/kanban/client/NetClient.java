package vn.vku.kanban.client;

import com.google.gson.JsonObject;
import com.google.gson.Gson;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;

public class NetClient implements Closeable {
    private final String host;
    private final int port;
    private final Consumer<String> logger;
    private final Gson gson = new Gson();

    private Socket sock;
    private DataInputStream in;
    private DataOutputStream out;
    private Thread reader;
    private volatile boolean running = false;

    // queue nhận response dành cho sendAndRecv()
    private final BlockingQueue<String> respQueue = new ArrayBlockingQueue<>(8);

    public NetClient(String host, int port, Consumer<String> logger) {
        this.host = host;
        this.port = port;
        this.logger = logger;
    }

    public void connect() throws IOException {
        sock = new Socket(host, port);
        in = new DataInputStream(new BufferedInputStream(sock.getInputStream()));
        out = new DataOutputStream(new BufferedOutputStream(sock.getOutputStream()));
        startReader(); // <-- BẮT ĐẦU NGHE NỀN
    }

    private void startReader() {
        running = true;
        reader = new Thread(() -> {
            try {
                while (running) {
                    int len = in.readInt();                      // chặn đọc frame
                    if (len <= 0 || len > (1<<20)) continue;
                    byte[] buf = new byte[len];
                    in.readFully(buf);
                    String msg = new String(buf, StandardCharsets.UTF_8);

                    // Phân loại thô: nếu là RESP (có "status") -> đẩy về respQueue
                    if (msg.startsWith("{\"status\"")) {
                        respQueue.offer(msg);
                    } else {
                        // Coi như EVENT server-push
                        logger.accept("[EVENT] " + msg);
                    }
                }
            } catch (Exception e) {
                if (running) logger.accept("[ERR/reader] " + e.getMessage());
            }
        }, "net-reader");
        reader.setDaemon(true);
        reader.start();
    }

    public String sendAndRecv(JsonObject json) throws IOException, InterruptedException {
        String s = gson.toJson(json);
        byte[] data = s.getBytes(StandardCharsets.UTF_8);
        synchronized (out) {
            out.writeInt(data.length);
            out.write(data);
            out.flush();
        }

        // chờ response đầu tiên từ queue (timeout 5s để tránh treo)
        String resp = respQueue.poll(5, java.util.concurrent.TimeUnit.SECONDS);
        if (resp == null) throw new IOException("Timeout waiting response");
        return resp;
    }

    @Override
    public void close() throws IOException {
        running = false;
        try { if (sock != null) sock.close(); } catch (Exception ignored) {}
        try { if (reader != null) reader.interrupt(); } catch (Exception ignored) {}
        try { if (in != null) in.close(); } catch (Exception ignored) {}
        try { if (out != null) out.close(); } catch (Exception ignored) {}
    }
}
