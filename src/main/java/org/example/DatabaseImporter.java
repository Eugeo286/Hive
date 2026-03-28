package org.example;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException; // <--- The missing piece!
import java.sql.Statement;

public class DatabaseImporter {
    public static void main(String[] args) {
        String url = "jdbc:mysql://gondola.proxy.rlwy.net:52921/railway";
        String user = "root";
        String pass = "uKIpTBuqRQKaySoaaWRvbiLrcbaQZjJz";
        String filePath = "C:\\Users\\HP\\quizzes.sql";

        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            System.out.println("Connected to Railway! Importing tables...");

            String sql = new String(Files.readAllBytes(Paths.get(filePath)));
            String[] queries = sql.split(";");
            Statement stmt = conn.createStatement();

            for (String query : queries) {
                if (!query.trim().isEmpty()) {
                    try {
                        stmt.execute(query);
                    } catch (SQLException e) {
                        // Error 1050 means the table is already there
                        if (e.getErrorCode() == 1050) {
                            System.out.println("Skipped a table (already exists).");
                        } else {
                            System.err.println("Note: " + e.getMessage());
                        }
                    }
                }
            }

            System.out.println("✅ Finished! Your data is now in the cloud.");
        } catch (Exception e) {
            System.err.println("❌ Critical Error: " + e.getMessage());
        }
    }
}