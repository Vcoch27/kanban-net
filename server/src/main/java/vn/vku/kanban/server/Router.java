package vn.vku.kanban.server;

import com.google.gson.*;
import vn.vku.kanban.server.model.Board;
import vn.vku.kanban.server.session.BroadcastHub;
import vn.vku.kanban.server.session.ClientSession;
import vn.vku.kanban.server.store.Store;

public class Router {
    private final Store store;
    private final BroadcastHub hub;

    public Router(Store store, BroadcastHub hub){ this.store=store; this.hub=hub; }

    public String handle(ClientSession sess, String jsonStr){
        var j = Jsons.G.fromJson(jsonStr, JsonObject.class);
        var type = j.has("type") ? j.get("type").getAsString() : "";
        var corr = j.has("corr") ? j.get("corr").getAsString() : null;
        var data = j.has("data") && j.get("data").isJsonObject() ? j.get("data").getAsJsonObject() : new JsonObject();

        try {
            return switch (type) {
                case "PING" -> Jsons.ok(corr, Jsons.obj()).toString();
                case "CREATE_BOARD" -> onCreateBoard(corr, data);
                case "SUBSCRIBE_BOARD" -> onSubscribeBoard(sess, corr, data);
                default -> Jsons.err(corr,"E_INVALID","unknown type").toString();
            };
        } catch (Exception e){
            return Jsons.err(corr,"E_INTERNAL", e.getMessage()).toString();
        }
    }

    private String onCreateBoard(String corr, JsonObject data){
        var name = data.has("name")? data.get("name").getAsString() : "Untitled";
        Board b = store.createBoard(name);

        var boardJson = Jsons.G.toJsonTree(b).getAsJsonObject();
        // Response
        var respData = Jsons.obj(); respData.add("board", boardJson);
        var resp = Jsons.ok(corr, respData).toString();
        // Event broadcast
        var ev = new JsonObject(); ev.addProperty("type","EV_BOARD_CREATED");
        var evData = Jsons.obj(); evData.add("board", boardJson); ev.add("data", evData);
        var evStr = ev.toString();
        hub.broadcastToBoard(b.id, evStr); // ai đã subscribe board này sẽ nhận

        return resp;
    }

    private String onSubscribeBoard(ClientSession sess, String corr, JsonObject data){
        long boardId = data.get("board_id").getAsLong();
        sess.subscribeBoard(boardId);

        // snapshot tối thiểu: chỉ board (Lists/Cards sẽ thêm sau)
        var board = store.getBoard(boardId);
        var snap = Jsons.obj();
        if (board != null) snap.add("board", Jsons.G.toJsonTree(board).getAsJsonObject());

        var resp = Jsons.ok(corr, snap).toString();
        return resp;
    }
}
