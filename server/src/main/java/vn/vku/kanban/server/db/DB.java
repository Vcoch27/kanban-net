package vn.vku.kanban.server.db;

import java.sql.*;
import java.util.Properties;
import java.io.InputStream;

public class DB {
    private static Connection conn;

    public static Connection connect() throws Exception {
        if (conn != null && !conn.isClosed()) return conn;

        try (InputStream in = DB.class.getClassLoader().getResourceAsStream("db.properties")) {
            Properties p = new Properties();
            p.load(in);
            String url = p.getProperty("db.url");
            String user = p.getProperty("db.user");
            String pass = p.getProperty("db.pass");
            conn = DriverManager.getConnection(url, user, pass);
            System.out.println("[DB] Connected: " + url);
            return conn;
        }
    }

    public static void insertBoard(String name) throws Exception {
        String sql = "INSERT INTO boards(name, updated_at) VALUES (?, ?)";
        try (PreparedStatement st = connect().prepareStatement(sql)) {
            st.setString(1, name);
            st.setLong(2, System.currentTimeMillis());
            st.executeUpdate();
        }
    }

    public static void close() {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
                System.out.println("[DB] Connection closed.");
            }
        } catch (SQLException e) {
            System.err.println("[DB_ERR] Failed to close connection: " + e.getMessage());
        }
    }
}
