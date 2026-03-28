package org.example;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Standalone connection tester.
 * Run this class directly to verify that MySQL/XAMPP is running
 * and that the "hive" database is reachable before starting the app.
 */
public class DatabaseConnection {

    public static void main(String[] args) {
        try (Connection connection = DBUtil.getConnection()) {
            if (connection != null) {
                System.out.println("Success! Connected to the Hive database.");
            }
        } catch (SQLException e) {
            System.out.println("Connection failed! Make sure MySQL is running in XAMPP.");
            e.printStackTrace();
        }
    }
}
