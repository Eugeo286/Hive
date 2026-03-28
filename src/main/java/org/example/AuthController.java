package org.example;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private JavaMailSender mailSender;

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body("Email is required.");
        }

        // 1. Generate a random 6-digit code
        String code = String.format("%06d", new Random().nextInt(999999));

        // 2. Save code to database (Expires in 15 minutes)
        // Uses ON DUPLICATE KEY to overwrite any old codes they requested before
        String sql = "INSERT INTO password_resets (email, code, expires_at) " +
                "VALUES (?, ?, DATE_ADD(NOW(), INTERVAL 15 MINUTE)) " +
                "ON DUPLICATE KEY UPDATE code = ?, expires_at = DATE_ADD(NOW(), INTERVAL 15 MINUTE)";

        try (Connection c = DBUtil.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1, email);
            s.setString(2, code);
            s.setString(3, code);
            s.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Database connection error.");
        }

        // 3. Send the email via JavaMailSender
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("The Hive - Password Reset Code");
            message.setText("Hello,\n\nYour password reset code is: " + code +
                    "\n\nThis code will expire in 15 minutes.\n\n- The Hive Team");
            mailSender.send(message);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Failed to send email. Check SMTP credentials.");
        }

        return ResponseEntity.ok("Verification code sent to email.");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String code = body.get("code");
        String newPassword = body.get("newPassword");

        if (email == null || code == null || newPassword == null) {
            return ResponseEntity.badRequest().body("Please fill in all fields.");
        }

        // 1. Verify the code exists and hasn't expired
        String verifySql = "SELECT * FROM password_resets WHERE email = ? AND code = ? AND expires_at > NOW()";
        boolean isValid = false;

        try (Connection c = DBUtil.getConnection(); PreparedStatement s = c.prepareStatement(verifySql)) {
            s.setString(1, email);
            s.setString(2, code);
            ResultSet rs = s.executeQuery();
            if (rs.next()) {
                isValid = true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Database error.");
        }

        if (!isValid) {
            return ResponseEntity.status(400).body("Invalid or expired reset code.");
        }

        // 2. Hash the new password and update the user's account
        String hashedPass = BCrypt.hashpw(newPassword, BCrypt.gensalt());
        String updateSql = "UPDATE users SET password = ? WHERE email = ?";

        try (Connection c = DBUtil.getConnection(); PreparedStatement s = c.prepareStatement(updateSql)) {
            s.setString(1, hashedPass);
            s.setString(2, email);
            s.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error updating password.");
        }

        // 3. Clean up the used code so it can't be used again
        String deleteSql = "DELETE FROM password_resets WHERE email = ?";
        try (Connection c = DBUtil.getConnection(); PreparedStatement s = c.prepareStatement(deleteSql)) {
            s.setString(1, email);
            s.executeUpdate();
        } catch (SQLException e) {
            // Ignore minor cleanup errors
        }

        return ResponseEntity.ok("Password reset successfully. You can now log in.");
    }
}