package org.example;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBUtil {
    private DBUtil() {}

    public static Connection getConnection() throws SQLException {
        // 1. Try to get the automated Railway connection string
        String railwayUrl = System.getenv("MYSQL_URL");

        if (railwayUrl != null) {
            // Railway gives us "mysql://...", but JDBC needs "jdbc:mysql://..."
            String jdbcUrl = railwayUrl.replace("mysql://", "jdbc:mysql://");
            return DriverManager.getConnection(jdbcUrl);
        }

        // 2. Fallback for your local laptop (XAMPP)
        String localUrl = "jdbc:mysql://localhost:3306/hive";
        String localUser = "root";
        String localPass = "";
        return DriverManager.getConnection(localUrl, localUser, localPass);
    }
}