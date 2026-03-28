package org.example;

import org.mindrot.jbcrypt.BCrypt;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    public boolean saveUser(User u, String plainPassword, String role) {
        String hashed = BCrypt.hashpw(plainPassword, BCrypt.gensalt());
        String sql = "INSERT INTO users (username, email, password, points, role) VALUES (?,?,?,?,?)";
        try (Connection c = DBUtil.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1, u.getName());
            s.setString(2, u.getEmail());
            s.setString(3, hashed);
            s.setInt(4, 0);
            s.setString(5, role);
            s.executeUpdate();
            return true;
        } catch (SQLIntegrityConstraintViolationException e) {
            return false;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Database error during registration", e);
        }
    }

    public User login(String email, String plainPassword) {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (Connection c = DBUtil.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1, email);
            ResultSet rs = s.executeQuery();
            if (rs.next()) {
                if (!BCrypt.checkpw(plainPassword, rs.getString("password"))) return null;

                String role = rs.getString("role");

                if ("admin".equalsIgnoreCase(role)) {
                    Admin a = new Admin(rs.getString("username"), rs.getString("email"));
                    a.setId(rs.getInt("id"));
                    a.setPoints(rs.getInt("points"));
                    return a;
                } else if ("teacher".equalsIgnoreCase(role)) {
                    Teacher t = new Teacher(rs.getString("username"), rs.getString("email"), "Staff");
                    t.setId(rs.getInt("id"));
                    t.setPoints(rs.getInt("points"));
                    return t;
                } else {
                    Student st = new Student(rs.getString("username"), rs.getString("email"));
                    st.setId(rs.getInt("id"));
                    st.setPoints(rs.getInt("points"));
                    return st;
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    public void updatePoints(User u) {
        String sql = "UPDATE users SET points = ? WHERE email = ?";
        try (Connection c = DBUtil.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, u.getPoints());
            s.setString(2, u.getEmail());
            s.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public List<Student> getLeaderboard(int limit) {
        List<Student> list = new ArrayList<>();
        String sql = "SELECT * FROM users WHERE role='student' ORDER BY points DESC LIMIT ?";
        try (Connection c = DBUtil.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, limit);
            ResultSet rs = s.executeQuery();
            while (rs.next()) {
                Student st = new Student(rs.getString("username"), rs.getString("email"));
                st.setId(rs.getInt("id"));
                st.setPoints(rs.getInt("points"));
                list.add(st);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }
}