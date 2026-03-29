package org.example;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.*;
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
        User u = "teacher".equalsIgnoreCase(role)
                ? new Teacher(body.get("name"), body.get("email"), "Staff")
                : new Student(body.get("name"), body.get("email"));
        boolean saved = userDAO.saveUser(u, body.get("password"), role);
        if (!saved) return ResponseEntity.status(409).body("Email already in use.");
        return ResponseEntity.ok("Registered successfully.");
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String,Object>> login(@RequestBody Map<String,String> body) {
        User u = userDAO.login(body.get("email"), body.get("password"));
        if (u == null) return ResponseEntity.status(401).build();

        Map<String,Object> res = new LinkedHashMap<>();
        res.put("id",     u.getId()); // Crucial for enrollment logic
        res.put("name",   u.getName());
        res.put("email",  u.getEmail());
        res.put("points", u.getPoints());

        String role = "student";
        if (u instanceof Admin) { role = "admin"; }
        else if (u instanceof Teacher) { role = "teacher"; }

        res.put("role", role);
        return ResponseEntity.ok(res);
    }

    // ══════════════════════════════════════════════════════
    // QUESTIONS & ANSWERS (FIXED DUPLICATES)
    // ══════════════════════════════════════════════════════

    // Merged: Fetches either all or filtered by department
    @GetMapping("/questions")
    public List<Question> getQuestions(@RequestParam(required = false) String department) {
        if (department != null && !department.isEmpty()) {
            return contentDAO.getQuestionsByDepartment(department);
        }
        return contentDAO.getAllQuestions();
    }

    // New specific endpoint for department filtering
    @GetMapping("/questions/department")
    public List<Question> getQuestionsByDept(@RequestParam String name) {
        return contentDAO.getQuestionsByDepartment(name);
    }

    // Merged: One single POST mapping for questions
    @PostMapping("/questions")
    public ResponseEntity<String> addQuestion(@RequestBody Question q) {
        contentDAO.saveQuestion(q);
        return ResponseEntity.ok("Question deployed to the Hive department: " + q.getDepartment());
    }

    @DeleteMapping("/questions/{id}")
    public ResponseEntity<String> deleteQuestion(@PathVariable int id) {
        contentDAO.deleteQuestion(id);
        return ResponseEntity.ok("Deleted.");
    }

    @PostMapping("/questions/{id}/answers")
    public ResponseEntity<String> postAnswer(@PathVariable int id, @RequestBody Map<String, String> body) {
        String text = body.get("text");
        String authorName = body.get("authorName");
        contentDAO.saveAnswer(id, new Answer(text, authorName));
        return ResponseEntity.ok("Answer posted.");
    }

    // ══════════════════════════════════════════════════════
    // NOTIFICATIONS (NEW)
    // ══════════════════════════════════════════════════════

    @GetMapping("/notifications/{userId}")
    public List<Map<String, Object>> getNotifications(@PathVariable int userId) {
        return contentDAO.getNotifications(userId);
    }

    // ══════════════════════════════════════════════════════
    // RESOURCES & UPLOADS
    // ══════════════════════════════════════════════════════

    @GetMapping("/resources")
    public List<Resource> getAllResources() {
        return contentDAO.getResourcesByCourse("");
    }

    @GetMapping("/resources/{course}")
    public List<Resource> getResources(@PathVariable String course) {
        return contentDAO.getResourcesByCourse(course);
    }

    @PostMapping("/resources/upload")
    public ResponseEntity<Map<String,Object>> uploadFile(
            @RequestParam("file")     MultipartFile file,
            @RequestParam("title")    String title,
            @RequestParam("course")   String course,
            @RequestParam("postedBy") String postedBy) {

        Map<String,Object> result = new LinkedHashMap<>();
        try {
            File dir = new File(UPLOAD_DIR);
            if (!dir.exists()) dir.mkdirs();

            String originalName = file.getOriginalFilename();
            String safeName     = System.currentTimeMillis() + "_" + originalName;
            Files.write(Paths.get(UPLOAD_DIR + safeName), file.getBytes());

            Resource r = new Resource(title, "/uploads/" + safeName, course, postedBy, detectType(originalName));
            r.setOriginalFileName(originalName);
            contentDAO.saveResource(r);

            result.put("message", "Uploaded successfully.");
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            result.put("error", "Upload failed");
            return ResponseEntity.status(500).body(result);
        }
    }

    // ══════════════════════════════════════════════════════
    // ADMIN: ARCHITECT ENDPOINTS
    // ══════════════════════════════════════════════════════

    @GetMapping("/departments")
    public List<Map<String, Object>> getDepts() {
        return contentDAO.getAllDepartments();
    }

    @PostMapping("/admin/departments")
    public ResponseEntity<String> createDept(@RequestBody Map<String, String> payload) {
        contentDAO.addDepartment(payload.get("name"));
        return ResponseEntity.ok("Department created.");
    }

    @PostMapping("/admin/courses")
    public ResponseEntity<String> createCourse(@RequestBody Map<String, Object> payload) {
        int deptId = Integer.parseInt(payload.get("deptId").toString());
        String code = payload.get("courseCode").toString();
        String title = payload.get("title").toString();
        contentDAO.addCourseToDept(deptId, code, title);
        return ResponseEntity.ok("Module deployed.");
    }

    // ══════════════════════════════════════════════════════
    // CHAT & ENROLLMENT
    // ══════════════════════════════════════════════════════

    @GetMapping("/courses")
    public List<Course> getAllCourses() {
        return contentDAO.getAllCourses();
    }

    @GetMapping("/users/{userId}/courses")
    public List<Course> getUserCourses(@PathVariable int userId) {
        return contentDAO.getCoursesForUser(userId);
    }

    @PostMapping("/courses/{courseId}/enroll")
    public ResponseEntity<String> enrollInCourse(@PathVariable int courseId, @RequestBody Map<String, Object> payload) {
        int userId = Integer.parseInt(payload.get("userId").toString());
        String role = payload.get("role").toString();
        contentDAO.enrollUserInCourse(userId, courseId, role);
        return ResponseEntity.ok("Enrolled successfully!");
    }

    @GetMapping("/chat/course/{courseId}")
    public List<Message> getCourseChat(@PathVariable int courseId) {
        return contentDAO.getMessagesForCourse(courseId);
    }

    @PostMapping("/chat/send")
    public ResponseEntity<String> sendChatMessage(@RequestBody Map<String, Object> payload) {
        int senderId = Integer.parseInt(payload.get("senderId").toString());
        int courseId = Integer.parseInt(payload.get("courseId").toString());
        String content = payload.get("content").toString();

        Message msg = new Message(senderId, courseId, content);
        contentDAO.saveChatMessage(msg);
        return ResponseEntity.ok("Message sent");
    }

    // ══════════════════════════════════════════════════════
    // AI Q&A
    // ══════════════════════════════════════════════════════

    @PostMapping("/questions/{id}/ask-ai")
    public ResponseEntity<Map<String, String>> requestAiAnswer(@PathVariable int id, @RequestBody Map<String, String> payload) {
        String questionText = payload.get("questionText");
        String simulatedAiResponse = "Regarding '" + questionText + "': This is a core topic in your department. Check the suggested library resources for a deep dive!";

        contentDAO.saveAiAnswer(id, simulatedAiResponse);
        Map<String, String> response = new HashMap<>();
        response.put("aiAnswer", simulatedAiResponse);
        return ResponseEntity.ok(response);
    }

    // --- Utility ---
    private String detectType(String name) {
        if (name == null) return "file";
        String f = name.toLowerCase();
        if (f.endsWith(".pdf")) return "pdf";
        if (f.endsWith(".ppt") || f.endsWith(".pptx")) return "slides";
        if (f.endsWith(".png") || f.endsWith(".jpg")) return "image";
        if (f.endsWith(".mp4")) return "video";
        return "file";
    }

    // ══════════════════════════════════════════════════════
// CHAT ADVANCED FEATURES (Typing & Read Receipts)
// ══════════════════════════════════════════════════════

    @PostMapping("/chat/course/{courseId}/typing")
    public ResponseEntity<String> updateTypingStatus(@PathVariable int courseId, @RequestBody Map<String, Object> payload) {
        int userId = Integer.parseInt(payload.get("userId").toString());
        String userName = payload.get("userName").toString();
        boolean isTyping = (boolean) payload.get("status");

        if (isTyping) {
            contentDAO.setTyping(courseId, userId, userName);
        } else {
            contentDAO.removeTyping(courseId, userId);
        }
        return ResponseEntity.ok("Status updated");
    }

    @GetMapping("/chat/course/{courseId}/typing-status")
    public List<String> getTypingStatus(@PathVariable int courseId) {
        // Returns a list of names who typed in the last 5 seconds
        return contentDAO.getActiveTypers(courseId);
    }

    @PostMapping("/chat/course/{courseId}/read")
    public ResponseEntity<String> markMessagesRead(@PathVariable int courseId, @RequestBody Map<String, Integer> payload) {
        int userId = payload.get("userId");
        contentDAO.markAsRead(courseId, userId);
        return ResponseEntity.ok("Marked as read");
    }
}