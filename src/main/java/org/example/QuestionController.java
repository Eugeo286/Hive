package org.example;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/questions")
// Single unified CORS config — do NOT also set spring.web.cors in properties
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {
        RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
        RequestMethod.DELETE, RequestMethod.OPTIONS
})
public class QuestionController {

    private final ContentDAO contentDAO = new ContentDAO();
    private final UserDAO    userDAO    = new UserDAO();
    private static final String UPLOAD_DIR = "uploads/";

    // ── AUTH ──────────────────────────────────────────────────────────────

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody Map<String, String> body) {
        String name  = body.get("name");
        String email = body.get("email");
        String pass  = body.get("password");
        String role  = body.get("role");
        if (name == null || email == null || pass == null || role == null)
            return ResponseEntity.badRequest().body("Missing fields.");
        User u = "teacher".equalsIgnoreCase(role)
                ? new Teacher(name, email, "Staff")
                : new Student(name, email);
        boolean saved = userDAO.saveUser(u, pass, role);
        if (!saved) return ResponseEntity.status(409).body("Email already in use.");
        return ResponseEntity.ok("Registered successfully.");
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        User user = userDAO.login(body.get("email"), body.get("password"));
        if (user == null) return ResponseEntity.status(401).build();
        Map<String, Object> res = new HashMap<>();
        res.put("name",   user.getName());
        res.put("email",  user.getEmail());
        res.put("points", user.getPoints());
        res.put("role",   user instanceof Teacher ? "teacher" : "student");
        return ResponseEntity.ok(res);
    }

    // ── QUESTIONS ─────────────────────────────────────────────────────────
    // IMPORTANT: specific paths (/urgent, /search) MUST be declared BEFORE
    // the parameterised path (/{id}) — Spring matches top-down and
    // "urgent" would otherwise be treated as an id value.

    @GetMapping("/questions")
    public ResponseEntity<List<Question>> getAllQuestions() {
        return ResponseEntity.ok(contentDAO.getAllQuestions());
    }

    @PostMapping("/questions")
    public ResponseEntity<String> addQuestion(@RequestBody Question q) {
        if (q.getTitle() == null || q.getTitle().isBlank())
            return ResponseEntity.badRequest().body("Title is required.");
        if (q.getSubject() == null || q.getSubject().isBlank())
            return ResponseEntity.badRequest().body("Subject is required.");
        contentDAO.saveQuestion(q);
        return ResponseEntity.ok("Question posted.");
    }

    // MUST come before /{id} mapping
    @GetMapping("/questions/urgent")
    public ResponseEntity<List<Question>> getUrgentQuestions() {
        return ResponseEntity.ok(contentDAO.getUnansweredOver24Hours());
    }

    // MUST come before /{id} mapping
    @GetMapping("/questions/search")
    public ResponseEntity<List<Question>> searchQuestions(@RequestParam String subject) {
        List<Question> all = contentDAO.getAllQuestions();
        List<Question> filtered = all.stream()
                .filter(q -> q.getSubject() != null &&
                        q.getSubject().toLowerCase().contains(subject.toLowerCase()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(filtered);
    }

    @GetMapping("/questions/{id}")
    public ResponseEntity<Question> getQuestion(@PathVariable int id) {
        // Returns a single question by ID — used for validation
        List<Question> all = contentDAO.getAllQuestions();
        return all.stream().filter(q -> q.getId() == id)
                .findFirst()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/questions/{id}")
    public ResponseEntity<String> deleteQuestion(@PathVariable int id) {
        contentDAO.deleteQuestion(id);
        return ResponseEntity.ok("Deleted.");
    }

    // ── ANSWERS ───────────────────────────────────────────────────────────

    @GetMapping("/questions/{id}/answers")
    public ResponseEntity<List<Map<String, Object>>> getAnswers(@PathVariable int id) {
        return ResponseEntity.ok(contentDAO.getAnswersForQuestion(id));
    }

    @PostMapping("/questions/{id}/answers")
    public ResponseEntity<String> postAnswer(@PathVariable int id,
                                             @RequestBody Map<String, String> body) {
        String text       = body.get("text");
        String authorName = body.get("authorName");
        if (text == null || text.isBlank())
            return ResponseEntity.badRequest().body("Answer text is required.");
        contentDAO.saveAnswer(id, new Answer(text, authorName));
        return ResponseEntity.ok("Answer posted.");
    }

    @PostMapping("/answers/verify/{id}")
    public ResponseEntity<String> verifyAnswer(@PathVariable int id) {
        contentDAO.verifyAnswer(id);
        return ResponseEntity.ok("Verified.");
    }

    // ── RESOURCES ─────────────────────────────────────────────────────────

    // Specific paths before parameterised ones
    @PostMapping("/resources/upload")
    public ResponseEntity<Map<String, Object>> uploadResource(
            @RequestParam("file")     MultipartFile file,
            @RequestParam("title")    String title,
            @RequestParam("course")   String course,
            @RequestParam("postedBy") String postedBy) {

        Map<String, Object> result = new HashMap<>();
        if (file.isEmpty()) {
            result.put("error", "No file selected.");
            return ResponseEntity.badRequest().body(result);
        }
        if (file.getSize() > 50 * 1024 * 1024) {
            result.put("error", "File too large. Maximum 50 MB.");
            return ResponseEntity.badRequest().body(result);
        }
        try {
            File dir = new File(UPLOAD_DIR);
            if (!dir.exists()) dir.mkdirs();
            String originalName = file.getOriginalFilename();
            String safeName     = System.currentTimeMillis() + "_" + originalName;
            Path   savePath     = Paths.get(UPLOAD_DIR + safeName);
            Files.write(savePath, file.getBytes());
            String fileUrl      = "/uploads/" + safeName;
            String resourceType = detectType(originalName);
            Resource r = new Resource(title, fileUrl, course, postedBy, resourceType);
            r.setOriginalFileName(originalName);
            contentDAO.saveResource(r);
            result.put("message",      "Uploaded successfully.");
            result.put("url",          fileUrl);
            result.put("fileName",     originalName);
            result.put("resourceType", resourceType);
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            e.printStackTrace();
            result.put("error", "Upload failed: " + e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }

    @GetMapping("/resources/{course}")
    public ResponseEntity<List<Resource>> getResources(@PathVariable String course) {
        return ResponseEntity.ok(contentDAO.getResourcesByCourse(course));
    }

    @PostMapping("/resources")
    public ResponseEntity<String> addResource(@RequestBody Resource r) {
        contentDAO.saveResource(r);
        return ResponseEntity.ok("Resource posted.");
    }

    @DeleteMapping("/resources/{id}")
    public ResponseEntity<String> deleteResource(@PathVariable int id) {
        contentDAO.deleteResourceWithFile(id, UPLOAD_DIR);
        return ResponseEntity.ok("Deleted.");
    }

    // ── BOOKS ─────────────────────────────────────────────────────────────

    @GetMapping("/books/{course}")
    public ResponseEntity<String> getBookSuggestions(@PathVariable String course) {
        try {
            String query = course.replace(" ", "+");
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://openlibrary.org/search.json?q="
                            + query + "&limit=5&fields=title,author_name,key"))
                    .GET().build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            return ResponseEntity.ok(resp.body());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("{\"docs\":[]}");
        }
    }

    // ── ASSIGNMENTS ───────────────────────────────────────────────────────
    // Specific paths declared BEFORE /{id} paths to avoid routing collisions

    @GetMapping("/assignments/all")
    public ResponseEntity<List<Assignment>> getAllAssignments() {
        return ResponseEntity.ok(contentDAO.getAllAssignments());
    }

    @GetMapping("/assignments/course/{course}")
    public ResponseEntity<List<Assignment>> getAssignmentsByCourse(@PathVariable String course) {
        return ResponseEntity.ok(contentDAO.getAssignmentsByCourse(course));
    }

    @PostMapping("/assignments")
    public ResponseEntity<String> postAssignment(@RequestBody Assignment a) {
        if (a.getTitle() == null || a.getTitle().isBlank())
            return ResponseEntity.badRequest().body("Title is required.");
        contentDAO.saveAssignment(a);
        return ResponseEntity.ok("Assignment posted.");
    }

    @GetMapping("/assignments/{id}/answers")
    public ResponseEntity<String> getAssignmentAnswers(@PathVariable int id) {
        return ResponseEntity.ok(contentDAO.getAnswerIfDeadlinePassed(id));
    }

    // ── SUBMISSIONS ───────────────────────────────────────────────────────

    @PostMapping("/assignments/{id}/submit")
    public ResponseEntity<String> submitAssignment(@PathVariable int id,
                                                   @RequestBody Submission s) {
        s.setAssignmentId(id);
        contentDAO.saveSubmission(id, s);
        return ResponseEntity.ok("Submitted.");
    }

    @GetMapping("/assignments/{id}/submissions")
    public ResponseEntity<List<Submission>> getSubmissions(@PathVariable int id) {
        return ResponseEntity.ok(contentDAO.getSubmissions(id));
    }

    // ── VIDEO SESSIONS ────────────────────────────────────────────────────

    @GetMapping("/sessions")
    public ResponseEntity<List<VideoSession>> getAllSessions() {
        return ResponseEntity.ok(contentDAO.getAllVideoSessions());
    }

    @PostMapping("/sessions")
    public ResponseEntity<String> scheduleSession(@RequestBody VideoSession vs) {
        contentDAO.saveVideoSession(vs);
        return ResponseEntity.ok("Scheduled.");
    }

    @DeleteMapping("/sessions/{id}")
    public ResponseEntity<String> deleteSession(@PathVariable int id) {
        contentDAO.deleteVideoSession(id);
        return ResponseEntity.ok("Cancelled.");
    }

    // ── ANALYTICS ─────────────────────────────────────────────────────────

    @GetMapping("/leaderboard")
    public ResponseEntity<List<Student>> getLeaderboard() {
        return ResponseEntity.ok(userDAO.getLeaderboard(10));
    }

    @GetMapping("/stats/subjects")
    public ResponseEntity<Map<String, Long>> getSubjectBreakdown() {
        Map<String, Long> result = contentDAO.getAllQuestions().stream()
                .filter(q -> q.getSubject() != null)   // null-safe
                .collect(Collectors.groupingBy(Question::getSubject, Collectors.counting()));
        return ResponseEntity.ok(result);
    }

    @GetMapping("/stats/engagement")
    public ResponseEntity<List<Map<String, Object>>> getEngagementStats() {
        return ResponseEntity.ok(contentDAO.getAcademyInsights());
    }

    @GetMapping("/subjects")
    public ResponseEntity<List<String>> getSubjects() {
        List<String> subjects = contentDAO.getAllQuestions().stream()
                .map(Question::getSubject)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        return ResponseEntity.ok(subjects);
    }

    // ── PRIVATE HELPERS ───────────────────────────────────────────────────

    private String detectType(String filename) {
        if (filename == null) return "file";
        String f = filename.toLowerCase();
        if (f.endsWith(".pdf"))                                  return "pdf";
        if (f.endsWith(".ppt")  || f.endsWith(".pptx"))         return "slides";
        if (f.endsWith(".doc")  || f.endsWith(".docx"))         return "document";
        if (f.endsWith(".xls")  || f.endsWith(".xlsx"))         return "spreadsheet";
        if (f.endsWith(".mp4")  || f.endsWith(".mov"))          return "video";
        if (f.endsWith(".png")  || f.endsWith(".jpg") || f.endsWith(".jpeg")) return "image";
        if (f.endsWith(".zip")  || f.endsWith(".rar"))          return "archive";
        if (f.endsWith(".txt")  || f.endsWith(".md"))           return "text";
        return "file";
    }
}
