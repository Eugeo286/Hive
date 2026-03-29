package org.example;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
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

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody Map<String,String> body) {
        String role = body.get("role");
        User u = "teacher".equalsIgnoreCase(role) ? new Teacher(body.get("name"), body.get("email"), "Staff") : new Student(body.get("name"), body.get("email"));
        boolean saved = userDAO.saveUser(u, body.get("password"), role);
        return saved ? ResponseEntity.ok("Registered.") : ResponseEntity.status(409).body("Email taken.");
    }

    // ══════════════════════════════════════════════════════
    // QUESTIONS & REAL AI
    // ══════════════════════════════════════════════════════
    @GetMapping("/questions")
    public List<Question> getAllQuestions() { return contentDAO.getAllQuestions(); }

    @PostMapping("/questions")
    public ResponseEntity<String> saveQuestion(@RequestBody Question q) {
        contentDAO.saveQuestion(q);
        return ResponseEntity.ok("Saved.");
    }

    @PostMapping("/questions/{id}/answers")
    public ResponseEntity<String> postAnswer(@PathVariable int id, @RequestBody Map<String, String> body) {
        contentDAO.saveAnswer(id, new Answer(body.get("text"), body.get("authorName")));
        return ResponseEntity.ok("Answered.");
    }

    @PostMapping("/questions/{id}/ask-ai")
    public ResponseEntity<Map<String, String>> requestAiAnswer(@PathVariable int id, @RequestBody Map<String, String> payload) {
        String questionText = payload.get("questionText");
        String apiKey = System.getenv("GEMINI_API_KEY");
        String aiAnswer;

        if (apiKey == null || apiKey.isEmpty()) {
            // Fallback if no API key is set in Railway
            aiAnswer = "I am the Hive AI. To give you advanced answers, please add GEMINI_API_KEY to your Railway variables. For now: Please review your course notes for '" + questionText + "'.";
        } else {
            // REAL GEMINI API CALL
            try {
                RestTemplate restTemplate = new RestTemplate();
                String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey;

                String requestBody = "{\"contents\": [{\"parts\":[{\"text\": \"You are an AI tutor for a university portal called The Hive. Answer this student's question clearly, concisely, and educationally: " + questionText.replace("\"", "\\\"") + "\"}]}]}";

                HttpHeaders headers = new HttpHeaders();
                headers.set("Content-Type", "application/json");
                HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

                ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

                // Parse the deeply nested Gemini JSON response
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.getBody().get("candidates");
                Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                aiAnswer = (String) parts.get(0).get("text");

            } catch (Exception e) {
                aiAnswer = "I encountered an error connecting to my neural network. Please try asking again later.";
            }
        }

        contentDAO.saveAiAnswer(id, aiAnswer);
        return ResponseEntity.ok(Map.of("aiAnswer", aiAnswer));
    }

    // ══════════════════════════════════════════════════════
    // COURSES & ENROLLMENT
    // ══════════════════════════════════════════════════════
    @GetMapping("/courses")
    public List<Course> getAllCourses() { return contentDAO.getAllCourses(); }

    @GetMapping("/users/{userId}/courses")
    public List<Course> getUserCourses(@PathVariable int userId) { return contentDAO.getCoursesForUser(userId); }

    @PostMapping("/courses/{courseId}/enroll")
    public ResponseEntity<String> enroll(@PathVariable int courseId, @RequestBody Map<String, Object> payload) {
        contentDAO.enrollUserInCourse((int)payload.get("userId"), courseId, (String)payload.get("role"));
        return ResponseEntity.ok("Enrolled");
    }

    // ══════════════════════════════════════════════════════
    // DATA FETCHERS (Assignments, Sessions, Chat)
    // ══════════════════════════════════════════════════════
    @GetMapping("/assignments/all")
    public List<Assignment> getAssignments() { return contentDAO.getAllAssignments(); }

    @GetMapping("/sessions")
    public List<VideoSession> getSessions() { return contentDAO.getAllVideoSessions(); }

    @GetMapping("/chat/course/{courseId}")
    public List<Message> getChat(@PathVariable int courseId) { return contentDAO.getMessagesForCourse(courseId); }

    @PostMapping("/chat/send")
    public ResponseEntity<String> sendChat(@RequestBody Map<String, Object> payload) {
        Message msg = new Message(Integer.parseInt(payload.get("senderId").toString()), Integer.parseInt(payload.get("courseId").toString()), payload.get("content").toString());
        contentDAO.saveChatMessage(msg);
        return ResponseEntity.ok("Sent.");
    }
}