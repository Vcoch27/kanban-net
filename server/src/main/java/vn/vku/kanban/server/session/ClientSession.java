package vn.vku.kanban.server.session;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class ClientSession {
    public final Consumer<String> sendJson; // hàm gửi JSON string ra socket
    private final Set<Long> subscribedBoardIds = ConcurrentHashMap.newKeySet();

    public ClientSession(Consumer<String> sendJson){ this.sendJson = sendJson; }

    public void subscribeBoard(long boardId){ subscribedBoardIds.add(boardId); }
    public boolean isSubscribed(long boardId){ return subscribedBoardIds.contains(boardId); }
}
