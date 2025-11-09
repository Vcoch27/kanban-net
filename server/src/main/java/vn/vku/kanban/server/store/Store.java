package vn.vku.kanban.server.store;

import vn.vku.kanban.server.model.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class Store {
    private final AtomicLong boardSeq = new AtomicLong(1);
    private final AtomicLong listSeq = new AtomicLong(1);
    private final AtomicLong cardSeq = new AtomicLong(1);

    private final Map<Long, Board> boards = new ConcurrentHashMap<>();
    private final Map<Long, ListItem> lists = new ConcurrentHashMap<>();
    private final Map<Long, CardItem> cards = new ConcurrentHashMap<>();

    public synchronized Board createBoard(String name) {
        var b = new Board();
        b.id = boardSeq.getAndIncrement();
        b.name = name == null ? "Untitled" : name;
        b.version = 1;
        b.updated_at = System.currentTimeMillis();
        boards.put(b.id, b);
        return b;
    }

    public synchronized ListItem createList(long boardId, String name, int pos) {
        var l = new ListItem();
        l.id = listSeq.getAndIncrement();
        l.board_id = boardId;
        l.name = name;
        l.position = pos;
        l.version = 1;
        l.updated_at = System.currentTimeMillis();
        lists.put(l.id, l);
        return l;
    }

    public synchronized CardItem createCard(long boardId, long listId, String title, String desc, int pos) {
        var c = new CardItem();
        c.id = cardSeq.getAndIncrement();
        c.board_id = boardId;
        c.list_id = listId;
        c.title = title;
        c.desc = desc;
        c.position = pos;
        c.version = 1;
        c.updated_at = System.currentTimeMillis();
        cards.put(c.id, c);
        return c;
    }

    public synchronized CardItem moveCard(long cardId, long toListId, int toPos) {
        var c = cards.get(cardId);
        if (c == null) return null;
        c.list_id = toListId;
        c.position = toPos;
        c.version++;
        c.updated_at = System.currentTimeMillis();
        return c;
    }

    public Collection<ListItem> getLists(long boardId) {
        return lists.values().stream().filter(l -> l.board_id == boardId).toList();
    }
    public Collection<CardItem> getCards(long boardId) {
        return cards.values().stream().filter(c -> c.board_id == boardId).toList();
    }
    public Board getBoard(long id){ return boards.get(id); }
}
