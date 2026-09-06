import java.sql.*;
import vn.iotstar.connection.DBConnection;
import vn.iotstar.config.JPAConfig;
import vn.iotstar.dao.impl.CategoryDaoImpl;
public class CheckDatabase {
    public static void main(String[] args) throws Exception {
        try (Connection cn = new DBConnection().getConnection()) {
            System.out.println("JDBC OK: " + cn.getMetaData().getDatabaseProductVersion());
            try (Statement st = cn.createStatement()) {
                for (String sql : new String[]{"SELECT DB_NAME() AS database_name", "SELECT COLUMN_NAME, DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='dbo' AND TABLE_NAME IN ('User','Category')", "SELECT COUNT(*) FROM dbo.[User]", "SELECT COUNT(*) FROM dbo.Category"}) {
                    try (ResultSet rs = st.executeQuery(sql)) {
                        while (rs.next()) { for (int i=1; i<=rs.getMetaData().getColumnCount(); i++) System.out.print(rs.getString(i) + " "); System.out.println(); }
                    }
                }
            }
        }
        try { System.out.println("JPA OK: categories=" + new CategoryDaoImpl().findAll().size()); }
        finally { JPAConfig.close(); }
    }
}
