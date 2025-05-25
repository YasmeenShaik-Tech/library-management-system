
package sample.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    public static Connection getConnection() throws SQLException {
        String url = "jdbc:postgresql://localhost:5432/library_db";
        String user = "library_user";
        String password = "12345";
        return DriverManager.getConnection(url, user, password);
    }
}
