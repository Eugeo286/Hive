package org.example;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.sql.*;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    // ══════════════════════════════════════════════════════
    // 1. USER & ROLE MANAGEMENT
    // ══════════════════════════════════════════════════════
    @GetMapping("/users")
    public List<Map<String, Object>> getAllUsers() {
        List<Map<String, Object>> users = new ArrayList<>();
        String sql = "SELECT id, username, email, role, points FROM users ORDER BY role ASC, points DESC";
        try (Connection c = DBUtil.getConnection(); Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("id", rs.getInt("id"));
                map.put("username", rs.getString("username"));
                map.put("email", rs.getString("email"));
                map.put("role", rs.getString("role"));
                map.put("points", rs.getInt("points"));
                users.add(map);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return users;
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<String> updateUserRole(@PathVariable int id, @RequestBody Map<String, String> body) {
        String newRole = body.get("role");
        String sql = "UPDATE users SET role = ? WHERE id = ?";
        try (Connection c = DBUtil.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1, newRole);
            s.setInt(2, id);
            s.executeUpdate();
            return ResponseEntity.ok("Role updated to " + newRole);
        } catch (SQLException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error updating role.");
        }
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable int id) {
        String sql = "DELETE FROM users WHERE id=?";
        try (Connection c = DBUtil.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, id);
            s.executeUpdate();
            return ResponseEntity.ok("User permanently deleted.");
        } catch (SQLException e) { return ResponseEntity.status(500).body("Error deleting user."); }
    }

    // ══════════════════════════════════════════════════════
    // 2. COMMUNITY MODERATION (Q&A)
    // ══════════════════════════════════════════════════════
    @DeleteMapping("/questions/{id}")
    public ResponseEntity<String> forceDeleteQuestion(@PathVariable int id) {
        String sql = "DELETE FROM questions WHERE id=?";
        try (Connection c = DBUtil.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, id);
            s.executeUpdate();
            return ResponseEntity.ok("Post wiped from server.");
        } catch (SQLException e) { return ResponseEntity.status(500).body("Error deleting post."); }
    }

    // ══════════════════════════════════════════════════════
    // 3. GLOBAL BROADCASTS (ANNOUNCEMENTS)
    // ══════════════════════════════════════════════════════
    @PostMapping("/broadcast")
    public ResponseEntity<String> postAnnouncement(@RequestBody Map<String, String> body) {
        String sql = "INSERT INTO announcements (message, posted_by) VALUES (?, ?)";
        try (Connection c = DBUtil.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1, body.get("message"));
            s.setString(2, body.get("postedBy"));
            s.executeUpdate();
            return ResponseEntity.ok("Broadcast sent server-wide.");
        } catch (SQLException e) { return ResponseEntity.status(500).body("Broadcast failed."); }
    }

    @GetMapping("/broadcast/latest")
    public ResponseEntity<Map<String, String>> getLatestAnnouncement() {
        String sql = "SELECT message, posted_by, created_at FROM announcements ORDER BY created_at DESC LIMIT 1";
        try (Connection c = DBUtil.getConnection(); Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            if (rs.next()) {
                Map<String, String> map = new HashMap<>();
                map.put("message", rs.getString("message"));
                map.put("postedBy", rs.getString("posted_by"));
                return ResponseEntity.ok(map);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return ResponseEntity.ok(new HashMap<>()); // Return empty if no announcements
    }

    @DeleteMapping("/broadcast/clear")
    public ResponseEntity<String> clearBroadcasts() {
        String sql = "TRUNCATE TABLE announcements";
        try (Connection c = DBUtil.getConnection(); Statement s = c.createStatement()) {
            s.executeUpdate(sql);
            return ResponseEntity.ok("All broadcasts cleared.");
        } catch (SQLException e) {
            return ResponseEntity.status(500).body("Failed to clear.");
        }
    }
}