package vn.iotstar.connection;

import java.sql.Connection;
import java.sql.DriverManager;
import vn.iotstar.config.AppConfig;

public class DBConnection {
    public Connection getConnection() throws Exception {
        Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        return DriverManager.getConnection(AppConfig.get("db.url", "DB_URL"),
                AppConfig.get("db.user", "DB_USER"), AppConfig.get("db.password", "DB_PASSWORD"));
    }
}
