package org.example;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@RestController
@RequestMapping("/api/hive")
@CrossOrigin(origins = "*")
public class HiveController {

    private final ContentDAO contentDAO = new ContentDAO();
    private final UserDAO    userDAO    = new UserDAO();
    private static final String UPLOAD_DIR = "uploads/";

    // ══════════════════════════════════════════════════════
    // AUTHENTICATION
    // ══════════════════════════════════════════════════════
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody Map<String,String> body) {
        String role = body.get("role");
        User u = "teacher".equalsIgnoreCase(role) ? new Teacher(body.get("name"), body.get("email"), "Staff") : new Student(body.get("name"), body.get("email"));
        boolean saved = userDAO.saveUser(u, body.get("password"), role);
        return saved ? ResponseEntity.ok("Registered.") : ResponseEntity.status(409).body("Error.");
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String,Object>> login(@RequestBody Map<String,String> body) {
        User u = userDAO.login(body.get("email"), body.get("password"));
        if (u == null) return ResponseEntity.status(401).build();
        Map<String,Object> res = new LinkedHashMap<>();
        res.put("id", u.getId()); res.put("name", u.getName());
        res.put("role", (u instanceof Admin ? "admin" : (u instanceof Teacher ? "teacher" : "student")));
        res.put("points", u.getPoints());
        return ResponseEntity.ok(res);
    }

    // ══════════════════════════════════════════════════════
    // QUESTIONS & AI
    // ══════════════════════════════════════════════════════
    @GetMapping("/questions")
    public List<Question> getQuestions(@RequestParam(required = false) String department) {
        return (department != null) ? contentDAO.getQuestionsByDepartment(department) : contentDAO.getAllQuestions();
    }

    @PostMapping("/questions")
    public ResponseEntity<String> saveQuestion(@RequestBody Question q) {
        contentDAO.saveQuestion(q);
        return ResponseEntity.ok("Saved.");
    }

    @PostMapping("/questions/{id}/ask-ai")
    public ResponseEntity<Map<String, String>> requestAiAnswer(@PathVariable int id, @RequestBody Map<String, String> payload) {
        String text = payload.get("questionText").toLowerCase();
        String answer;
        if (text.contains("java") || text.contains("code")) {
            answer = "The Hivemind suggests: Check your loops and object instantiation. Always ensure your class names match your file names.";
        } else if (text.contains("math")) {
            answer = "For this mathematical query, isolate the variable and check your quadratic signs. Review the formula in Resources.";
        } else {
            answer = "I've analyzed '" + text + "'. This matches core concepts in your department. Review the current module's slides.";
        }
        contentDAO.saveAiAnswer(id, answer);
        return ResponseEntity.ok(Map.of("aiAnswer", answer));
    }

    // ══════════════════════════════════════════════════════
    // ADVANCED CHAT & DEPARTMENTS
    // ══════════════════════════════════════════════════════
    @GetMapping("/departments")
    public List<Map<String, Object>> getDepts() { return contentDAO.getAllDepartments(); }

    @GetMapping("/chat/course/{courseId}")
    public List<Message> getChat(@PathVariable int courseId) { return contentDAO.getMessagesForCourse(courseId); }

    @PostMapping("/chat/send")
    public ResponseEntity<String> sendChat(@RequestBody Map<String, Object> payload) {
        Message msg = new Message(Integer.parseInt(payload.get("senderId").toString()), Integer.parseInt(payload.get("courseId").toString()), payload.get("content").toString());
        contentDAO.saveChatMessage(msg);
        return ResponseEntity.ok("Sent.");
    }

    @PostMapping("/chat/course/{courseId}/typing")
    public ResponseEntity<String> updateTyping(@PathVariable int courseId, @RequestBody Map<String, Object> p) {
        contentDAO.setTyping(courseId, (int)p.get("userId"), (String)p.get("userName"));
        return ResponseEntity.ok("Updated.");
    }

    @GetMapping("/chat/course/{courseId}/typing-status")
    public List<String> getTyping(@PathVariable int courseId) { return contentDAO.getActiveTypers(courseId); }

    @PostMapping("/chat/course/{courseId}/read")
    public ResponseEntity<String> markRead(@PathVariable int courseId, @RequestBody Map<String, Integer> p) {
        contentDAO.markAsRead(courseId, p.get("userId"));
        return ResponseEntity.ok("Read.");
    }

    // ══════════════════════════════════════════════════════
    // SESSIONS & ASSIGNMENTS
    // ══════════════════════════════════════════════════════
    @GetMapping("/assignments/all")
    public List<Assignment> getAllAssignments() { return contentDAO.getAllAssignments(); }

    @GetMapping("/sessions")
    public List<VideoSession> getSessions() { return contentDAO.getAllVideoSessions(); }

    @GetMapping("/users/{userId}/courses")
    public List<Course> getUserCourses(@PathVariable int userId) { return contentDAO.getCoursesForUser(userId); }
}