package vn.vku.kanban.client;

import com.google.gson.JsonObject;
import com.google.gson.Gson;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class App extends Application {
    private final Gson gson = new Gson();
    private NetClient net;
    private TextArea log;
    private TextField hostField, portField;

    @Override
    public void start(Stage stage) {
        hostField = new TextField("127.0.0.1");
        portField = new TextField("9000");
        hostField.setPrefColumnCount(12);
        portField.setPrefColumnCount(6);

        Button btnConnect = new Button("Connect");
        Button btnSend = new Button("Send Test");
        btnSend.setDisable(true);

        log = new TextArea();
        log.setEditable(false);
        log.setPrefRowCount(18);

        btnConnect.setOnAction(e -> {
            String host = hostField.getText().trim();
            int port = Integer.parseInt(portField.getText().trim());
            append("[UI] Connecting to " + host + ":" + port + " ...");
            btnConnect.setDisable(true);

            new Thread(() -> {
                try {
                    net = new NetClient(host, port, this::append);
                    net.connect();
                    Platform.runLater(() -> btnSend.setDisable(false));
                    append("[NET] Connected.");
                } catch (Exception ex) {
                    append("[ERR] " + ex.getMessage());
                    Platform.runLater(() -> btnConnect.setDisable(false));
                }
            }).start();
        });

        btnSend.setOnAction(e -> {
            if (net == null) return;
            JsonObject msg = new JsonObject();
            msg.addProperty("type", "PING");
            msg.addProperty("msg", "hello");
            new Thread(() -> {
                try {
                    String resp = net.sendAndRecv(msg);
                    append("[RESP] " + resp);
                } catch (Exception ex) {
                    append("[ERR] " + ex.getMessage());
                }
            }).start();
        });

        HBox top = new HBox(8, new Label("Host:"), hostField, new Label("Port:"), portField, btnConnect, btnSend);
        top.setPadding(new Insets(8));
        VBox root = new VBox(8, top, log);
        root.setPadding(new Insets(8));
        stage.setScene(new Scene(root, 760, 420));
        stage.setTitle("Kanban Client (LAN) — Test");
        stage.show();
    }

    private void append(String s) {
        Platform.runLater(() -> {
            log.appendText(s);
            log.appendText("\n");
        });
    }

    @Override
    public void stop() throws Exception {
        if (net != null) net.close();
        super.stop();
    }

    public static void main(String[] args) { launch(args); }
}
