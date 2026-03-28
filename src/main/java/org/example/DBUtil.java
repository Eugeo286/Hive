package org.example;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBUtil {
    // Railway Credentials (Internal)
    private static final String CLOUD_URL  = "jdbc:mysql://mysql.railway.internal:3306/railway";
    private static final String CLOUD_USER = "root";
    private static final String CLOUD_PASS = "yLUlkXKDNpPRgvDkEGVOzqdQOpQcSqQQ";

    // Your Local XAMPP Credentials (Fallback)
    private static final String LOCAL_URL  = "jdbc:mysql://localhost:3306/hive";
    private static final String LOCAL_USER = "root";
    private static final String LOCAL_PASS = "";

    private DBUtil() {}

    public static Connection getConnection() throws SQLException {
        // This check looks to see if the code is running on Railway
        if (System.getenv("RAILWAY_ENVIRONMENT") != null || System.getenv("MYSQLHOST") != null) {
            try {
                return DriverManager.getConnection(CLOUD_URL, CLOUD_USER, CLOUD_PASS);
            } catch (SQLException e) {
                // If the internal cloud link fails, we try the public one or log the error
                System.err.println("Cloud connection failed: " + e.getMessage());
            }
        }

        // If not on Railway, or if cloud connection failed, use local settings
        return DriverManager.getConnection(LOCAL_URL, LOCAL_USER, LOCAL_PASS);
    }
}