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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@RestController
@RequestMapping("/api/hive")
@CrossOrigin(origins = "*")
public class HiveController {

    private final ContentDAO contentDAO = new ContentDAO();
    private final UserDAO    userDAO    = new UserDAO();
    private static final String UPLOAD_DIR = "uploads/";

    // Memory storage for the Global Broadcast Banner
    private static String currentGlobalBroadcast = "";

    // ══════════════════════════════════════════════════════
    // AUTHENTICATION & AUTO-ENROLLMENT
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

        if (saved) {
            User newUser = userDAO.login(body.get("email"), body.get("password"));
            if (newUser != null) {
                // 1. Auto-enroll into the Global Lounge (Course ID 1)
                contentDAO.enrollUserInCourse(newUser.getId(), 1, role);

                // 2. Automated Welcome Bot Message
                String welcomeMsg = "System: Welcome to The Hive, " + newUser.getName() + "! Feel free to introduce yourself to the campus.";
                Message botMsg = new Message(0, 1, welcomeMsg); // Sender ID 0 represents the System
                contentDAO.saveChatMessage(botMsg);
            }
            return ResponseEntity.ok("Registered and auto-enrolled.");
        }
        return ResponseEntity.status(409).body("Email taken.");
    }

    // ══════════════════════════════════════════════════════
    // ADMIN FUNCTIONS (BROADCAST, PURGE, WIPE MODERATION)
    // ══════════════════════════════════════════════════════
    @PostMapping("/admin/broadcast")
    public ResponseEntity<String> setBroadcast(@RequestBody Map<String, String> body) {
        currentGlobalBroadcast = body.get("message");
        return ResponseEntity.ok("Broadcast live.");
    }

    @DeleteMapping("/admin/broadcast/clear")
    public ResponseEntity<String> clearBroadcast() {
        currentGlobalBroadcast = "";
        return ResponseEntity.ok("Broadcast stopped.");
    }

    @GetMapping("/admin/broadcast/latest")
    public ResponseEntity<Map<String, String>> getBroadcast() {
        Map<String, String> res = new HashMap<>();
        if (currentGlobalBroadcast != null && !currentGlobalBroadcast.isEmpty()) {
            res.put("message", currentGlobalBroadcast);
        }
        return ResponseEntity.ok(res);
    }

    @DeleteMapping("/admin/users/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable int id) {
        try (java.sql.Connection c = DBUtil.getConnection();
             java.sql.PreparedStatement s = c.prepareStatement("DELETE FROM users WHERE id = ?")) {
            s.setInt(1, id);
            s.executeUpdate();
            return ResponseEntity.ok("User Purged.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error purging user.");
        }
    }

    @DeleteMapping("/questions/{id}")
    public ResponseEntity<String> deleteQuestion(@PathVariable int id) {
        contentDAO.deleteQuestion(id);
        return ResponseEntity.ok("Question wiped.");
    }

    @DeleteMapping("/resources/{id}")
    public ResponseEntity<String> deleteResource(@PathVariable int id) {
        // UPGRADED: Now actually deletes the physical file from the server hard drive!
        contentDAO.deleteResourceWithFile(id, UPLOAD_DIR);
        return ResponseEntity.ok("Resource and file permanently deleted.");
    }

    // ══════════════════════════════════════════════════════
    // ADMIN COURSE & SUBJECT CREATION
    // ══════════════════════════════════════════════════════
    @PostMapping("/admin/departments")
    public ResponseEntity<String> createDept(@RequestBody Map<String, String> payload) {
        contentDAO.addDepartment(payload.get("name"));
        return ResponseEntity.ok("Master Course created.");
    }

    @PostMapping("/admin/courses")
    public ResponseEntity<String> createCourse(@RequestBody Map<String, Object> payload) {
        int deptId = Integer.parseInt(payload.get("deptId").toString());
        String code = payload.get("courseCode").toString();
        String title = payload.get("title").toString();

        String sql = "INSERT INTO courses (dept_id, course_code, title) VALUES (?, ?, ?)";
        try (java.sql.Connection c = DBUtil.getConnection();
             java.sql.PreparedStatement s = c.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {

            s.setInt(1, deptId); s.setString(2, code); s.setString(3, title);
            s.executeUpdate();

            // If the Admin selected a teacher, auto-enroll them immediately
            if (payload.containsKey("teacherId") && payload.get("teacherId") != null && !payload.get("teacherId").toString().isEmpty()) {
                java.sql.ResultSet rs = s.getGeneratedKeys();
                if (rs.next()) {
                    int newCourseId = rs.getInt(1);
                    int teacherId = Integer.parseInt(payload.get("teacherId").toString());
                    contentDAO.enrollUserInCourse(teacherId, newCourseId, "teacher");
                }
            }
            return ResponseEntity.ok("Subject deployed and Teacher assigned.");
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Failed to deploy.");
        }
    }

    // ══════════════════════════════════════════════════════
    // COURSES & SUBJECTS (ENROLLMENT)
    // ══════════════════════════════════════════════════════
    @GetMapping("/departments")
    public List<Map<String, Object>> getDepts() { return contentDAO.getAllDepartments(); }

    @GetMapping("/courses")
    public List<Course> getAllCourses() { return contentDAO.getAllCourses(); }

    @GetMapping("/users/{userId}/courses")
    public List<Course> getUserCourses(@PathVariable int userId) { return contentDAO.getCoursesForUser(userId); }

    @PostMapping("/courses/{courseId}/enroll")
    public ResponseEntity<String> enroll(@PathVariable int courseId, @RequestBody Map<String, Object> payload) {
        contentDAO.enrollUserInCourse((int)payload.get("userId"), courseId, (String)payload.get("role"));
        return ResponseEntity.ok("Enrolled");
    }

    @DeleteMapping("/courses/{courseId}/unenroll/{userId}")
    public ResponseEntity<String> unenroll(@PathVariable int courseId, @PathVariable int userId) {
        String sql = "DELETE FROM enrollments WHERE user_id = ? AND course_id = ?";
        try (java.sql.Connection c = DBUtil.getConnection(); java.sql.PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, userId);
            s.setInt(2, courseId);
            s.executeUpdate();
            return ResponseEntity.ok("Unenrolled successfully.");
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error unenrolling.");
        }
    }

    // ══════════════════════════════════════════════════════
    // QUESTIONS & REAL AI (GEMINI API)
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

    @PostMapping("/answers/{id}/verify")
    public ResponseEntity<String> verifyPeerAnswer(@PathVariable int id) {
        // Officially verify peer answer
        try (java.sql.Connection c = DBUtil.getConnection();
             java.sql.PreparedStatement s = c.prepareStatement("UPDATE answers SET is_verified = TRUE WHERE id = ?")) {
            s.setInt(1, id);
            s.executeUpdate();
        } catch(Exception e) {}
        return ResponseEntity.ok("Answer officially verified.");
    }

    @PostMapping("/questions/{id}/ask-ai")
    public ResponseEntity<Map<String, String>> requestAiAnswer(@PathVariable int id, @RequestBody Map<String, String> payload) {
        String questionText = payload.get("questionText");
        String apiKey = System.getenv("GEMINI_API_KEY"); // Set this in Railway Variables!
        String aiAnswer;

        if (apiKey == null || apiKey.isEmpty()) {
            aiAnswer = "I am the Hive AI. To utilize my full intelligence, please configure the GEMINI_API_KEY in your Railway server variables. For now, please rely on your peers and faculty for answers.";
        } else {
            try {
                RestTemplate restTemplate = new RestTemplate();
                String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey;

                String requestBody = "{\"contents\": [{\"parts\":[{\"text\": \"You are an AI tutor for a university portal. Answer this student's question clearly and educationally: " + questionText.replace("\"", "\\\"") + "\"}]}]}";

                HttpHeaders headers = new HttpHeaders();
                headers.set("Content-Type", "application/json");
                HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

                ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

                List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.getBody().get("candidates");
                Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                aiAnswer = (String) parts.get(0).get("text");

            } catch (Exception e) {
                aiAnswer = "I experienced a network disruption connecting to Google servers. Please try asking again later.";
            }
        }

        contentDAO.saveAiAnswer(id, aiAnswer);
        return ResponseEntity.ok(Map.of("aiAnswer", aiAnswer));
    }

    // ══════════════════════════════════════════════════════
    // GENERAL FETCHERS, ASSIGNMENTS & FILE UPLOAD
    // ══════════════════════════════════════════════════════
    @GetMapping("/notifications/{userId}")
    public ResponseEntity<List<Map<String,String>>> getNotifications(@PathVariable int userId) {
        List<Map<String,String>> notifs = new ArrayList<>();
        notifs.add(Map.of("message", "Welcome to your dashboard! Keep an eye here for future alerts and assignment updates."));
        return ResponseEntity.ok(notifs);
    }

    @GetMapping("/assignments/all")
    public List<Assignment> getAssignments() { return contentDAO.getAllAssignments(); }

    @PostMapping("/assignments")
    public ResponseEntity<String> saveAssignment(@RequestBody Assignment a) {
        contentDAO.saveAssignment(a);
        return ResponseEntity.ok("Assignment posted.");
    }

    @PostMapping("/assignments/{id}/submit")
    public ResponseEntity<String> submitAssignment(@PathVariable int id, @RequestBody Submission s) {
        s.setAssignmentId(id);
        contentDAO.saveSubmission(id, s);
        return ResponseEntity.ok("Homework submitted successfully.");
    }

    @GetMapping("/sessions")
    public List<VideoSession> getSessions() { return contentDAO.getAllVideoSessions(); }

    @PostMapping("/sessions")
    public ResponseEntity<String> saveSession(@RequestBody VideoSession vs) {
        contentDAO.saveVideoSession(vs);
        return ResponseEntity.ok("Scheduled.");
    }

    @GetMapping("/resources")
    public List<Resource> getResources() { return contentDAO.getResourcesByCourse(""); }

    @PostMapping("/resources/upload")
    public ResponseEntity<Map<String,Object>> uploadFile(
            @RequestParam("file")     MultipartFile file,
            @RequestParam("title")    String title,
            @RequestParam("course")   String course,
            @RequestParam("postedBy") String postedBy) {

        try {
            File dir = new File(UPLOAD_DIR);
            if (!dir.exists()) dir.mkdirs();

            String originalName = file.getOriginalFilename();
            String safeName     = System.currentTimeMillis() + "_" + originalName;
            Files.write(Paths.get(UPLOAD_DIR + safeName), file.getBytes());

            Resource r = new Resource(title, "/uploads/" + safeName, course, postedBy, "file");
            r.setOriginalFileName(originalName);
            contentDAO.saveResource(r);

            return ResponseEntity.ok(Map.of("message", "Uploaded successfully."));
        } catch (IOException e) {
            return ResponseEntity.status(500).body(Map.of("error", "Upload failed"));
        }
    }

    // ══════════════════════════════════════════════════════
    // CHAT ENGINE (RESTORED TYPING INDICATORS)
    // ══════════════════════════════════════════════════════
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
    public List<String> getTyping(@PathVariable int courseId) {
        return contentDAO.getActiveTypers(courseId);
    }

    @PostMapping("/chat/course/{courseId}/read")
    public ResponseEntity<String> markRead(@PathVariable int courseId, @RequestBody Map<String, Integer> p) {
        contentDAO.markAsRead(courseId, p.get("userId"));
        return ResponseEntity.ok("Read.");
    }
}