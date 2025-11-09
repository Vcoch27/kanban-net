package vn.vku.kanban.server.model;

public class CardItem {
    public long id;
    public long board_id;
    public long list_id;
    public String title;
    public String desc;
    public int position;
    public long version;
    public long updated_at;
}
