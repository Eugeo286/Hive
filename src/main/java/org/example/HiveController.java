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
    // BROADCASTS
    // ══════════════════════════════════════════════════════

    @GetMapping("/broadcast/latest")
    public ResponseEntity<Map<String, String>> getLatestAnnouncement() {
        Map<String, String> announcement = contentDAO.getLatestAnnouncement();
        return ResponseEntity.ok(announcement);
    }

    // ══════════════════════════════════════════════════════
    // QUESTIONS & ANSWERS
    // ══════════════════════════════════════════════════════

    @GetMapping("/questions")
    public List<Question> getAllQuestions() { return contentDAO.getAllQuestions(); }

    @PostMapping("/questions")
    public ResponseEntity<String> addQuestion(@RequestBody Question q) {
        contentDAO.saveQuestion(q);
        return ResponseEntity.ok("Posted.");
    }

    @DeleteMapping("/questions/{id}")
    public ResponseEntity<String> deleteQuestion(@PathVariable int id) {
        contentDAO.deleteQuestion(id);
        return ResponseEntity.ok("Deleted.");
    }

    @GetMapping("/questions/urgent")
    public List<Question> getUrgent() { return contentDAO.getUnansweredOver24Hours(); }

    @PostMapping("/questions/{id}/answers")
    public ResponseEntity<String> postAnswer(@PathVariable int id, @RequestBody Map<String, String> body) {
        String text = body.get("text");
        String authorName = body.get("authorName");
        contentDAO.saveAnswer(id, new Answer(text, authorName));
        return ResponseEntity.ok("Answer posted.");
    }

    // ══════════════════════════════════════════════════════
    // RESOURCES & UPLOADS
    // ══════════════════════════════════════════════════════

    @GetMapping("/resources")
    public List<Resource> getAllResources() {
        return contentDAO.getResourcesByCourse(""); // Empty string matches all
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

    @DeleteMapping("/resources/{id}")
    public ResponseEntity<String> deleteResource(@PathVariable int id) {
        contentDAO.deleteResourceWithFile(id, UPLOAD_DIR);
        return ResponseEntity.ok("Deleted.");
    }

    // ══════════════════════════════════════════════════════
    // ASSIGNMENTS & SUBMISSIONS
    // ══════════════════════════════════════════════════════

    @GetMapping("/assignments/all")
    public List<Assignment> getAllAssignments() {
        return contentDAO.getAllAssignments();
    }

    @PostMapping("/assignments")
    public ResponseEntity<String> postAssignment(@RequestBody Assignment a) {
        contentDAO.saveAssignment(a);
        return ResponseEntity.ok("Assignment posted.");
    }

    @PostMapping("/assignments/{id}/submit")
    public ResponseEntity<String> submitAssignment(@PathVariable int id, @RequestBody Submission s) {
        s.setAssignmentId(id);
        contentDAO.saveSubmission(id, s);
        return ResponseEntity.ok("Submitted.");
    }

    // ══════════════════════════════════════════════════════
    // BOOKS (Open Library Proxy)
    // ══════════════════════════════════════════════════════

    @GetMapping("/books/{query}")
    public ResponseEntity<String> searchBooks(@PathVariable String query) {
        try {
            String url = "https://openlibrary.org/search.json?q="
                    + query.replace(" ", "+") + "&limit=6&fields=title,author_name,key";
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req   = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            return ResponseEntity.ok(resp.body());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("{\"error\":\"Offline\"}");
        }
    }

    // ══════════════════════════════════════════════════════
    // VIDEO SESSIONS
    // ══════════════════════════════════════════════════════

    @GetMapping("/sessions")
    public List<VideoSession> getSessions() {
        return contentDAO.getAllVideoSessions();
    }

    @PostMapping("/sessions")
    public ResponseEntity<String> scheduleSession(@RequestBody VideoSession vs) {
        String link = vs.getMeetingLink();
        if (link != null && !link.startsWith("http")) vs.setMeetingLink("https://" + link);
        contentDAO.saveVideoSession(vs);
        return ResponseEntity.ok("Scheduled.");
    }

    @DeleteMapping("/sessions/{id}")
    public ResponseEntity<String> deleteSession(@PathVariable int id) {
        contentDAO.deleteVideoSession(id);
        return ResponseEntity.ok("Cancelled.");
    }

    // ══════════════════════════════════════════════════════
    // ANALYTICS & GAMIFICATION
    // ══════════════════════════════════════════════════════

    @GetMapping("/leaderboard")
    public List<Student> leaderboard() { return userDAO.getLeaderboard(10); }

    @GetMapping("/stats/subjects")
    public Map<String,Long> subjectStats() { return contentDAO.getSubjectStats(); }

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
    // COURSE ENDPOINTS
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

    @PostMapping("/admin/courses")
    public ResponseEntity<String> createCourse(@RequestBody Course course) {
        contentDAO.createCourse(course);
        return ResponseEntity.ok("Course officially registered in The Hive.");
    }

    // ══════════════════════════════════════════════════════
    // CHAT ENDPOINTS
    // ══════════════════════════════════════════════════════

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
    // AI Q&A ENDPOINTS
    // ══════════════════════════════════════════════════════

    @PostMapping("/questions/{id}/ask-ai")
    public ResponseEntity<Map<String, String>> requestAiAnswer(@PathVariable int id, @RequestBody Map<String, String> payload) {
        String questionText = payload.get("questionText");

        // --- AI GENERATION LOGIC ---
        // In a production app, you would make an HTTP call to the OpenAI or Gemini API here.
        // For now, we will generate a simulated context-aware response so you can build the UI.

        String simulatedAiResponse = "Based on my knowledge base, regarding your question about '"
                + questionText
                + "': This is a common topic in this course. Ensure you check the course syllabus and review the core fundamentals. If you need a more specific code or math breakdown, please reply!";

        // Save it to the database so it persists
        contentDAO.saveAiAnswer(id, simulatedAiResponse);

        Map<String, String> response = new HashMap<>();
        response.put("aiAnswer", simulatedAiResponse);
        return ResponseEntity.ok(response);
    }
}