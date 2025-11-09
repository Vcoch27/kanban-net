package vn.vku.kanban.server.session;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class BroadcastHub {
    private final Set<ClientSession> sessions = new CopyOnWriteArraySet<>();
    public void add(ClientSession s){ sessions.add(s); }
    public void remove(ClientSession s){ sessions.remove(s); }
    public void broadcastToBoard(long boardId, String json){
        for (var s : sessions) if (s.isSubscribed(boardId)) s.sendJson.accept(json);
    }
}
