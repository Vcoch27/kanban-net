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
    private int testStep = 0;
    private final java.util.List<com.google.gson.JsonObject> testMessages = new java.util.ArrayList<>();
    private Button btnSend;

    private void buildTestMessages() {
        // 1. CREATE_BOARD
        var msg1 = new JsonObject();
        msg1.addProperty("type", "CREATE_BOARD");
        var data1 = new JsonObject();
        data1.addProperty("name", "Sprint 1");
        msg1.add("data", data1);
        testMessages.add(msg1);

        // 2. CREATE_LIST
        var msg2 = new JsonObject();
        msg2.addProperty("type", "CREATE_LIST");
        var data2 = new JsonObject();
        data2.addProperty("board_id", 1);
        data2.addProperty("name", "To Do");
        data2.addProperty("position", 0);
        msg2.add("data", data2);
        testMessages.add(msg2);

        // 3. SUBSCRIBE_BOARD (cho Client A)
        var msg3 = new JsonObject();
        msg3.addProperty("type", "SUBSCRIBE_BOARD");
        var data3 = new JsonObject();
        data3.addProperty("board_id", 1);
        msg3.add("data", data3);
        testMessages.add(msg3);

        // 4. CREATE_CARD
        var msg4 = new JsonObject();
        msg4.addProperty("type", "CREATE_CARD");
        var data4 = new JsonObject();
        data4.addProperty("board_id", 1);
        data4.addProperty("list_id", 1);
        data4.addProperty("title", "Task A");
        data4.addProperty("desc", "Demo task");
        data4.addProperty("position", 0);
        msg4.add("data", data4);
        testMessages.add(msg4);

        // 5. MOVE_CARD
        var msg5 = new JsonObject();
        msg5.addProperty("type", "MOVE_CARD");
        var data5 = new JsonObject();
        data5.addProperty("card_id", 1);
        data5.addProperty("to_list_id", 1);
        data5.addProperty("to_position", 1);
        data5.addProperty("base_version", 1); // Giả sử version ban đầu là 1
        msg5.add("data", data5);
        testMessages.add(msg5);
    }


    @Override
    public void start(Stage stage) {
        buildTestMessages();
        hostField = new TextField("127.0.0.1");
        portField = new TextField("9000");
        hostField.setPrefColumnCount(12);
        portField.setPrefColumnCount(6);

        Button btnConnect = new Button("Connect");
        btnSend = new Button("Send Test (Step 1)");
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
            if (net == null || testStep >= testMessages.size()) return;

            JsonObject msg = testMessages.get(testStep);
            append("[SEND] " + msg.toString());

            new Thread(() -> {
                try {
                    String resp = net.sendAndRecv(msg);
                    append("[RESP] " + resp);
                } catch (Exception ex) {
                    append("[ERR] " + ex.getMessage());
                }
            }).start();

            testStep++;
            if (testStep < testMessages.size()) {
                String nextType = testMessages.get(testStep).get("type").getAsString();
                btnSend.setText("Send Test (Step " + (testStep + 1) + ": " + nextType + ")");
            } else {
                btnSend.setText("All Tests Done");
                btnSend.setDisable(true);
            }
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
