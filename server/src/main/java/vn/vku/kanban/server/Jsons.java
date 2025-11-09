package vn.vku.kanban.server;
import com.google.gson.*;

public final class Jsons {
    public static final Gson G = new GsonBuilder().serializeNulls().create();
    public static JsonObject obj() { return new JsonObject(); }
    public static JsonObject ok(String corr, JsonObject data){ var o=obj(); o.addProperty("status","ok"); if(corr!=null)o.addProperty("corr",corr); o.add("data",data); return o; }
    public static JsonObject err(String corr, String code, String msg){ var o=obj(); o.addProperty("status","error"); if(corr!=null)o.addProperty("corr",corr); var e=obj(); e.addProperty("code",code); e.addProperty("message",msg); o.add("error",e); return o; }
}
