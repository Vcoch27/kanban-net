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
                case "CREATE_LIST" -> onCreateList(corr, data);
                case "CREATE_CARD" -> onCreateCard(corr, data);
                case "MOVE_CARD" -> onMoveCard(corr, data);
                default -> Jsons.err(corr,"E_INVALID","unknown type").toString();
            };
        } catch (Exception e){
            return Jsons.err(corr,"E_INTERNAL", e.getMessage()).toString();
        }
    }

    private String onCreateBoard(String corr, JsonObject data){
        var name = data.has("name")? data.get("name").getAsString() : "Untitled";
        Board b = store.createBoard(name);

        try {
            vn.vku.kanban.server.db.DB.insertBoard(name);
        } catch (Exception e) {
            TcpServer.log("DB_ERR", "onCreateBoard failed: " + e.getMessage());
        }

        var boardJson = Jsons.G.toJsonTree(b).getAsJsonObject();
        var respData = Jsons.obj(); respData.add("board", boardJson);
        var resp = Jsons.ok(corr, respData).toString();

        var ev = new JsonObject();
        ev.addProperty("type","EV_BOARD_CREATED");
        var evData = Jsons.obj();
        evData.add("board", boardJson);
        ev.add("data", evData);
        hub.broadcastToBoard(b.id, ev.toString());

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
    private String onCreateList(String corr, JsonObject data) {
        long boardId = data.get("board_id").getAsLong();
        String name = data.get("name").getAsString();
        int pos = data.has("position") ? data.get("position").getAsInt() : 0;
        var l = store.createList(boardId, name, pos);

        var listJson = Jsons.G.toJsonTree(l).getAsJsonObject();
        var respData = Jsons.obj(); respData.add("list", listJson);
        var resp = Jsons.ok(corr, respData).toString();

        var ev = new JsonObject(); ev.addProperty("type", "EV_LIST_CREATED");
        var evData = Jsons.obj(); evData.add("list", listJson); ev.add("data", evData);
        hub.broadcastToBoard(boardId, ev.toString());
        return resp;
    }

    private String onCreateCard(String corr, JsonObject data) {
        long boardId = data.get("board_id").getAsLong();
        long listId = data.get("list_id").getAsLong();
        String title = data.get("title").getAsString();
        String desc = data.has("desc") ? data.get("desc").getAsString() : "";
        int pos = data.has("position") ? data.get("position").getAsInt() : 0;
        var c = store.createCard(boardId, listId, title, desc, pos);

        var cardJson = Jsons.G.toJsonTree(c).getAsJsonObject();
        var respData = Jsons.obj(); respData.add("card", cardJson);
        var resp = Jsons.ok(corr, respData).toString();

        var ev = new JsonObject(); ev.addProperty("type", "EV_CARD_CREATED");
        var evData = Jsons.obj(); evData.add("card", cardJson); ev.add("data", evData);
        hub.broadcastToBoard(boardId, ev.toString());
        return resp;
    }

    private String onMoveCard(String corr, JsonObject data) {
        long cardId = data.get("card_id").getAsLong();
        long toListId = data.get("to_list_id").getAsLong();
        int pos = data.has("to_position") ? data.get("to_position").getAsInt() : 0;
        var c = store.moveCard(cardId, toListId, pos);
        if (c == null) return Jsons.err(corr, "E_NOT_FOUND", "card not found").toString();

        var cardJson = Jsons.G.toJsonTree(c).getAsJsonObject();
        var respData = Jsons.obj(); respData.add("card", cardJson);
        var resp = Jsons.ok(corr, respData).toString();

        var ev = new JsonObject(); ev.addProperty("type", "EV_CARD_MOVED");
        var evData = Jsons.obj(); evData.add("card", cardJson); ev.add("data", evData);
        hub.broadcastToBoard(c.board_id, ev.toString());
        return resp;
    }

}
