package vn.vku.kanban.server.store;

import vn.vku.kanban.server.model.Board;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class Store {
    private final AtomicLong boardSeq = new AtomicLong(1);
    private final Map<Long, Board> boards = new ConcurrentHashMap<>();

    public synchronized Board createBoard(String name){
        var b = new Board();
        b.id = boardSeq.getAndIncrement();
        b.name = name==null?"Untitled":name;
        b.version = 1;
        b.updated_at = System.currentTimeMillis();
        boards.put(b.id, b);
        return b;
    }

    public Board getBoard(long id){ return boards.get(id); }
    public Collection<Board> allBoards(){ return boards.values(); }
}
