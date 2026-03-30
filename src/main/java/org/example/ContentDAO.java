package org.example;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.HashMap;
import java.util.Map;

public class ContentDAO {

    // ══════════════════════════════════════════════════════
    // QUESTIONS
    // ══════════════════════════════════════════════════════

    public void saveQuestion(Question q) {
        // Added 'department' to the INSERT
        String sql = "INSERT INTO questions (title, subject, author_name, course_id, department, is_answered, created_at) VALUES (?,?,?,?,?,false,NOW())";
        try (Connection c = DBUtil.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1, q.getTitle());
            s.setString(2, q.getSubject());
            s.setString(3, q.getAuthorName());
            s.setInt(4, q.getCourseId());
            s.setString(5, q.getDepartment()); // Store the chosen department
            s.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public List<Question> getAllQuestions() {
        List<Question> questions = new ArrayList<>();
        String sql = "SELECT q.*, (SELECT text FROM answers a WHERE a.question_id = q.id ORDER BY created_at DESC LIMIT 1) AS fetched_answer FROM questions q ORDER BY q.is_answered ASC, q.created_at DESC";

        try (Connection c = DBUtil.getConnection();
             PreparedStatement s = c.prepareStatement(sql);
             ResultSet rs = s.executeQuery()) {

            while (rs.next()) {
                Question q = new Question(rs.getString("title"), rs.getString("subject"), "Medium", rs.getString("author_name"));
                q.setId(rs.getInt("id"));
                q.setAnswered(rs.getBoolean("is_answered"));
                Timestamp ts = rs.getTimestamp("created_at");
                if (ts != null) q.setCreatedAt(ts.toLocalDateTime());
                q.setAnswerText(rs.getString("fetched_answer"));
                q.setAiAnswer(rs.getString("ai_answer")); // Added to ensure AI answers show
                questions.add(q);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return questions;
    }

    public List<Question> getQuestionsByDepartment(String dept) {
        List<Question> questions = new ArrayList<>();
        // Filter questions where the department matches the user's department
        String sql = "SELECT * FROM questions WHERE department = ? ORDER BY created_at DESC";
        try (Connection c = DBUtil.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1, dept);
            ResultSet rs = s.executeQuery();
            while (rs.next()) {
                // ... (your existing mapping logic)
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return questions;
    }

    public List<Question> getUnansweredOver24Hours() {
        List<Question> list = new ArrayList<>();
        String sql = "SELECT * FROM questions WHERE is_answered=false AND created_at < NOW() - INTERVAL 24 HOUR ORDER BY created_at ASC";
        try (Connection c = DBUtil.getConnection(); Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) list.add(mapQuestion(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public void markQuestionAnswered(int id) {
        String sql = "UPDATE questions SET is_answered=true WHERE id=?";
        try (Connection c = DBUtil.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, id); s.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void deleteQuestion(int id) {
        String sql = "DELETE FROM questions WHERE id=?";
        try (Connection c = DBUtil.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, id); s.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public List<Question> searchQuestions(String term) {
        List<Question> list = new ArrayList<>();
        String sql = "SELECT * FROM questions WHERE LOWER(subject) LIKE LOWER(?) OR LOWER(title) LIKE LOWER(?) ORDER BY created_at DESC";
        try (Connection c = DBUtil.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            String p = "%" + term.trim() + "%";
            s.setString(1, p); s.setString(2, p);
            ResultSet rs = s.executeQuery();
            while (rs.next()) list.add(mapQuestion(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    // ══════════════════════════════════════════════════════
    // ANSWERS
    // ══════════════════════════════════════════════════════

    public void saveAnswer(int questionId, Answer a) {
        String sql = "INSERT INTO answers (question_id,text,author_name,verified) VALUES (?,?,?,false)";
        try (Connection c = DBUtil.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, questionId);
            s.setString(2, a.getText());
            s.setString(3, a.getAuthorName());
            s.executeUpdate();
            markQuestionAnswered(questionId);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public List<Map<String,Object>> getAnswersForQuestion(int qId) {
        List<Map<String,Object>> list = new ArrayList<>();
        String sql = "SELECT * FROM answers WHERE question_id=? ORDER BY created_at ASC";
        try (Connection c = DBUtil.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, qId);
            ResultSet rs = s.executeQuery();
            while (rs.next()) {
                Map<String,Object> m = new LinkedHashMap<>();
                m.put("id",         rs.getInt("id"));
                m.put("text",       rs.getString("text"));
                m.put("authorName", rs.getString("author_name"));
                m.put("verified",   rs.getBoolean("verified"));
                list.add(m);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public void verifyAnswer(int answerId) {
        String sql = "UPDATE answers SET verified=true WHERE id=?";
        try (Connection c = DBUtil.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, answerId); s.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // ══════════════════════════════════════════════════════
    // NOTIFICATIONS
    // ══════════════════════════════════════════════════════

    // 🟢 NEW: Added Notification Methods
    public void addNotification(int userId, String message) {
        String sql = "INSERT INTO notifications (user_id, message) VALUES (?, ?)";
        try (Connection c = DBUtil.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, userId);
            s.setString(2, message);
            s.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public List<Map<String, Object>> getNotifications(int userId) {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT * FROM notifications WHERE user_id = ? ORDER BY created_at DESC LIMIT 5";
        try (Connection c = DBUtil.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, userId);
            ResultSet rs = s.executeQuery();
            while (rs.next()) {
                Map<String, Object> m = new HashMap<>();
                m.put("message", rs.getString("message"));
                m.put("time", rs.getTimestamp("created_at"));
                list.add(m);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    // ══════════════════════════════════════════════════════
    // RESOURCES
    // ══════════════════════════════════════════════════════

    public void saveResource(Resource r) {
        String sql = "INSERT INTO resources (title,link,course,posted_by,resource_type,original_file_name) VALUES (?,?,?,?,?,?)";
        try (Connection c = DBUtil.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1, r.getTitle());
            s.setString(2, r.getLink());
            s.setString(3, r.getCourse());
            s.setString(4, r.getPostedBy());
            s.setString(5, r.getResourceType());
            s.setString(6, r.getOriginalFileName());
            s.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public List<Resource> getResourcesByCourse(String course) {
        List<Resource> list = new ArrayList<>();
        String sql = "SELECT * FROM resources WHERE LOWER(course) LIKE LOWER(?) ORDER BY id DESC";
        try (Connection c = DBUtil.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1, "%" + course.trim() + "%");
            ResultSet rs = s.executeQuery();
            while (rs.next()) list.add(mapResource(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public void deleteResourceWithFile(int id, String uploadDir) {
        String fetchSql = "SELECT link FROM resources WHERE id=?";
        try (Connection c = DBUtil.getConnection(); PreparedStatement s = c.prepareStatement(fetchSql)) {
            s.setInt(1, id);
            ResultSet rs = s.executeQuery();
            if (rs.next()) {
                String link = rs.getString("link");
                if (link != null && link.startsWith("/uploads/")) {
                    java.io.File f = new java.io.File(uploadDir + link.substring("/uploads/".length()));
                    if (f.exists()) f.delete();
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        String sql = "DELETE FROM resources WHERE id=?";
        try (Connection c = DBUtil.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, id); s.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // ══════════════════════════════════════════════════════
    // ASSIGNMENTS
    // ══════════════════════════════════════════════════════

    public void saveAssignment(Assignment a) {
        String sql = "INSERT INTO assignments (title,description,course,teacher_name,deadline,answer_text) VALUES (?,?,?,?,?,?)";
        try (Connection c = DBUtil.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1, a.getTitle());
            s.setString(2, a.getDescription());
            s.setString(3, a.getCourse());
            s.setString(4, a.getTeacherName());
            s.setObject(5, a.getDeadline());
            s.setString(6, a.getAnswerText());
            s.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public List<Assignment> getAssignmentsByCourse(String course) {
        List<Assignment> list = new ArrayList<>();
        String sql = "SELECT id,title,description,course,teacher_name,deadline FROM assignments WHERE LOWER(course) LIKE LOWER(?) ORDER BY deadline ASC";
        try (Connection c = DBUtil.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1, "%" + course.trim() + "%");
            ResultSet rs = s.executeQuery();
            while (rs.next()) list.add(mapAssignment(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public List<Assignment> getAllAssignments() {
        List<Assignment> list = new ArrayList<>();
        String sql = "SELECT id,title,description,course,teacher_name,deadline FROM assignments ORDER BY deadline ASC";
        try (Connection c = DBUtil.getConnection(); Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) list.add(mapAssignment(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public String getAnswerIfDeadlinePassed(int id) {
        String sql = "SELECT answer_text, deadline FROM assignments WHERE id=?";
        try (Connection c = DBUtil.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, id);
            ResultSet rs = s.executeQuery();
            if (rs.next()) {
                Timestamp dl = rs.getTimestamp("deadline");
                if (dl != null && LocalDateTime.now().isAfter(dl.toLocalDateTime()))
                    return rs.getString("answer_text");
                return "\uD83D\uDD12 Answers unlock after deadline: " + (dl != null ? dl.toLocalDateTime() : "N/A");
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return "Assignment not found.";
    }

    // ══════════════════════════════════════════════════════
    // SUBMISSIONS
    // ══════════════════════════════════════════════════════

    public String saveSubmission(int assignmentId, Submission sub) {
        String checkSql = "SELECT deadline FROM assignments WHERE id=?";
        try (Connection c = DBUtil.getConnection(); PreparedStatement s = c.prepareStatement(checkSql)) {
            s.setInt(1, assignmentId);
            ResultSet rs = s.executeQuery();
            if (rs.next()) {
                Timestamp dl = rs.getTimestamp("deadline");
                if (dl != null && LocalDateTime.now().isAfter(dl.toLocalDateTime()))
                    return "DEADLINE_PASSED";
            }
        } catch (SQLException e) { e.printStackTrace(); return "ERROR"; }

        String sql = "INSERT INTO submissions (assignment_id,student_name,content,submitted_at) VALUES (?,?,?,NOW())";
        try (Connection c = DBUtil.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, assignmentId);
            s.setString(2, sub.getStudentName());
            s.setString(3, sub.getContent());
            s.executeUpdate();
            return "OK";
        } catch (SQLException e) { e.printStackTrace(); return "ERROR"; }
    }

    public List<Submission> getSubmissions(int assignmentId) {
        List<Submission> list = new ArrayList<>();
        String sql = "SELECT * FROM submissions WHERE assignment_id=? ORDER BY submitted_at DESC";
        try (Connection c = DBUtil.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, assignmentId);
            ResultSet rs = s.executeQuery();
            while (rs.next()) {
                Submission sub = new Submission(rs.getInt("assignment_id"), rs.getString("student_name"), rs.getString("content"));
                sub.setId(rs.getInt("id"));
                Timestamp ts = rs.getTimestamp("submitted_at");
                if (ts != null) sub.setSubmittedAt(ts.toLocalDateTime());
                list.add(sub);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    // ══════════════════════════════════════════════════════
    // VIDEO SESSIONS
    // ══════════════════════════════════════════════════════

    public void saveVideoSession(VideoSession vs) {
        String sql = "INSERT INTO video_sessions (topic,subject,host,scheduled_time,meeting_link) VALUES (?,?,?,?,?)";
        try (Connection c = DBUtil.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1, vs.getTopic());
            s.setString(2, vs.getSubject());
            s.setString(3, vs.getHost());
            s.setString(4, vs.getScheduledTime());
            s.setString(5, vs.getMeetingLink());
            s.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public List<VideoSession> getAllVideoSessions() {
        List<VideoSession> list = new ArrayList<>();
        String sql = "SELECT * FROM video_sessions ORDER BY scheduled_time ASC";
        try (Connection c = DBUtil.getConnection(); Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) {
                VideoSession vs = new VideoSession(rs.getString("topic"), rs.getString("subject"),
                        rs.getString("host"), rs.getString("scheduled_time"), rs.getString("meeting_link"));
                vs.setId(rs.getInt("id"));
                list.add(vs);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public void deleteVideoSession(int id) {
        String sql = "DELETE FROM video_sessions WHERE id=?";
        try (Connection c = DBUtil.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, id); s.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // ══════════════════════════════════════════════════════
    // ANALYTICS
    // ══════════════════════════════════════════════════════

    public Map<String,Long> getSubjectStats() {
        Map<String,Long> map = new LinkedHashMap<>();
        String sql = "SELECT subject, COUNT(*) as cnt FROM questions GROUP BY subject ORDER BY cnt DESC";
        try (Connection c = DBUtil.getConnection(); Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) map.put(rs.getString("subject"), rs.getLong("cnt"));
        } catch (SQLException e) { e.printStackTrace(); }
        return map;
    }

    public List<Map<String,Object>> getEngagementStats() {
        List<Map<String,Object>> list = new ArrayList<>();
        String sql = "SELECT q.subject, COUNT(a.id) as answers FROM questions q LEFT JOIN answers a ON q.id=a.question_id GROUP BY q.subject ORDER BY answers DESC";
        try (Connection c = DBUtil.getConnection(); Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) {
                Map<String,Object> m = new LinkedHashMap<>();
                m.put("subject",    rs.getString("subject"));
                m.put("engagement", rs.getLong("answers"));
                list.add(m);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public List<Map<String,Object>> getAcademyInsights() {
        return getEngagementStats();
    }

    // ══════════════════════════════════════════════════════
    // PRIVATE MAPPERS
    // ══════════════════════════════════════════════════════

    private Question mapQuestion(ResultSet rs) throws SQLException {
        Question q = new Question(rs.getString("title"), rs.getString("subject"),
                rs.getString("difficulty"), rs.getString("author_name"));
        q.setId(rs.getInt("id"));
        q.setAnswered(rs.getBoolean("is_answered"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) q.setCreatedAt(ts.toLocalDateTime());
        return q;
    }

    private Resource mapResource(ResultSet rs) throws SQLException {
        Resource r = new Resource(rs.getString("title"), rs.getString("link"),
                rs.getString("course"), rs.getString("posted_by"), rs.getString("resource_type"));
        r.setId(rs.getInt("id"));
        r.setOriginalFileName(rs.getString("original_file_name"));
        return r;
    }

    private Assignment mapAssignment(ResultSet rs) throws SQLException {
        Assignment a = new Assignment();
        a.setId(rs.getInt("id"));
        a.setTitle(rs.getString("title"));
        a.setDescription(rs.getString("description"));
        a.setCourse(rs.getString("course"));
        a.setTeacherName(rs.getString("teacher_name"));
        Timestamp ts = rs.getTimestamp("deadline");
        if (ts != null) a.setDeadline(ts.toLocalDateTime());
        return a;
    }

    public List<Map<String, Object>> viewAllQuestions() {
        List<Map<String, Object>> questions = new ArrayList<>();
        String sql = "SELECT * FROM questions";
        try (Connection c = DBUtil.getConnection(); Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("id", rs.getInt("id"));
                row.put("question", rs.getString("question"));
                row.put("optionA", rs.getString("optionA"));
                row.put("optionB", rs.getString("optionB"));
                row.put("optionC", rs.getString("optionC"));
                row.put("optionD", rs.getString("optionD"));
                row.put("answer", rs.getString("answer"));
                questions.add(row);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return questions;
    }

    public void viewAnswersForQuestion(int questionId) {
        String sql = "SELECT * FROM answers WHERE question_id = ?";
        try (Connection c = DBUtil.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, questionId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                System.out.println("Answer ID: " + rs.getInt("id"));
                System.out.println("Answer: " + rs.getString("answer"));
                System.out.println("Posted By: " + rs.getString("username"));
                System.out.println("----------------------");
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void deleteResource(int resourceId) {
        String sql = "DELETE FROM resources WHERE id = ?";
        try (Connection c = DBUtil.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, resourceId);
            int rows = ps.executeUpdate();
            if (rows > 0) System.out.println("Resource deleted successfully.");
            else System.out.println("Resource not found.");
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void saveSession(VideoSession v) {
        String sql = "INSERT INTO video_sessions (topic, subject, host, scheduled_time, meeting_link) VALUES (?,?,?,?,?)";
        try (Connection c = DBUtil.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1, v.getTopic());
            s.setString(2, v.getSubject());
            s.setString(3, v.getHost());
            s.setString(4, v.getScheduledTime());
            s.setString(5, v.getMeetingLink());
            s.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public List<VideoSession> getAllSessions() {
        List<VideoSession> list = new ArrayList<>();
        String sql = "SELECT * FROM video_sessions";
        try (Connection c = DBUtil.getConnection(); Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) {
                VideoSession v = new VideoSession(rs.getString("topic"), rs.getString("subject"),
                        rs.getString("host"), rs.getString("scheduled_time"), rs.getString("meeting_link"));
                v.setId(rs.getInt("id"));
                list.add(v);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public void deleteSession(int id) {
        String sql = "DELETE FROM video_sessions WHERE id = ?";
        try (Connection c = DBUtil.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, id); s.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public Map<String, String> getLatestAnnouncement() {
        Map<String, String> announcement = new HashMap<>();
        String sql = "SELECT message, posted_by FROM announcements ORDER BY created_at DESC LIMIT 1";
        try (Connection c = DBUtil.getConnection(); Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            if (rs.next()) {
                announcement.put("message", rs.getString("message"));
                announcement.put("postedBy", rs.getString("posted_by"));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return announcement;
    }

    public void deleteOldQuestions() {
        String sql = "DELETE FROM questions WHERE created_at < NOW() - INTERVAL 30 DAY";
        try (Connection c = DBUtil.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            int deletedCount = s.executeUpdate();
            if (deletedCount > 0) System.out.println("🧹 Robot Janitor: Deleted " + deletedCount + " old questions!");
        } catch (SQLException e) { System.out.println("Error during scheduled cleanup: " + e.getMessage()); }
    }

    // ══════════════════════════════════════════════════════
    // COURSE ENROLLMENT
    // ══════════════════════════════════════════════════════

    public List<Course> getAllCourses() {
        List<Course> list = new ArrayList<>();
        String sql = "SELECT * FROM courses ORDER BY title ASC";
        try (Connection c = DBUtil.getConnection(); Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) {
                Course course = new Course(rs.getString("course_code"), rs.getString("title"), rs.getString("description"));
                course.setId(rs.getInt("id"));
                list.add(course);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public List<Course> getCoursesForUser(int userId) {
        List<Course> list = new ArrayList<>();
        String sql = "SELECT c.* FROM courses c JOIN enrollments e ON c.id = e.course_id WHERE e.user_id = ?";
        try (Connection c = DBUtil.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, userId);
            ResultSet rs = s.executeQuery();
            while (rs.next()) {
                Course course = new Course(rs.getString("course_code"), rs.getString("title"), rs.getString("description"));
                course.setId(rs.getInt("id"));
                list.add(course);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public void enrollUserInCourse(int userId, int courseId, String role) {
        String sql = "INSERT IGNORE INTO enrollments (user_id, course_id, role) VALUES (?, ?, ?)";
        try (Connection c = DBUtil.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, userId);
            s.setInt(2, courseId);
            s.setString(3, role);
            s.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void createCourse(Course c) {
        String sql = "INSERT INTO courses (course_code, title, description) VALUES (?, ?, ?)";
        try (Connection conn = DBUtil.getConnection(); PreparedStatement s = conn.prepareStatement(sql)) {
            s.setString(1, c.getCourseCode());
            s.setString(2, c.getTitle());
            s.setString(3, c.getDescription());
            s.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // ══════════════════════════════════════════════════════
    // CHAT ENGINE
    // ══════════════════════════════════════════════════════

    public void saveChatMessage(Message msg) {
        // FIXED: Added 'sent_at' and 'NOW()' so the database knows when the chat was sent!
        String sql = "INSERT INTO messages (sender_id, course_id, content, sent_at) VALUES (?, ?, ?, NOW())";
        try (Connection c = DBUtil.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, msg.getSenderId());
            s.setInt(2, msg.getCourseId());
            s.setString(3, msg.getContent());
            s.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public List<Message> getMessagesForCourse(int courseId) {
        List<Message> list = new ArrayList<>();
        String sql = "SELECT m.*, u.name AS sender_name FROM messages m JOIN users u ON m.sender_id = u.id WHERE m.course_id = ? ORDER BY m.sent_at ASC";
        try (Connection c = DBUtil.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, courseId);
            ResultSet rs = s.executeQuery();
            while (rs.next()) {
                Message msg = new Message(rs.getInt("sender_id"), rs.getInt("course_id"), rs.getString("content"));
                msg.setId(rs.getInt("id"));
                msg.setSenderName(rs.getString("sender_name"));
                Timestamp ts = rs.getTimestamp("sent_at");
                if (ts != null) msg.setSentAt(ts.toLocalDateTime());
                list.add(msg);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    // ══════════════════════════════════════════════════════
    // AI ENGINE
    // ══════════════════════════════════════════════════════

    public void saveAiAnswer(int questionId, String aiAnswerText) {
        String sql = "UPDATE questions SET ai_answer = ? WHERE id = ?";
        try (Connection c = DBUtil.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1, aiAnswerText);
            s.setInt(2, questionId);
            s.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // ══════════════════════════════════════════════════════
// ADMIN: DEPARTMENT & COURSE MANAGEMENT
// ══════════════════════════════════════════════════════

    public void addDepartment(String name) {
        String sql = "INSERT INTO departments (name) VALUES (?)";
        try (Connection c = DBUtil.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1, name);
            s.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public List<Map<String, Object>> getAllDepartments() {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT * FROM departments ORDER BY name ASC";
        try (Connection c = DBUtil.getConnection(); Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, Object> m = new HashMap<>();
                m.put("id", rs.getInt("id"));
                m.put("name", rs.getString("name"));
                list.add(m);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public void addCourseToDept(int deptId, String code, String title) {
        String sql = "INSERT INTO courses (dept_id, course_code, title) VALUES (?, ?, ?)";
        try (Connection c = DBUtil.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, deptId);
            s.setString(2, code);
            s.setString(3, title);
            s.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }
    // ══════════════════════════════════════════════════════
// ADVANCED CHAT LOGIC (FIX FOR CONTROLLER ERRORS)
// ══════════════════════════════════════════════════════

    public void setTyping(int courseId, int userId, String userName) {
        String sql = "REPLACE INTO typing_status (course_id, user_id, user_name, last_typed) VALUES (?, ?, ?, NOW())";
        try (Connection c = DBUtil.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, courseId);
            s.setInt(2, userId);
            s.setString(3, userName);
            s.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void removeTyping(int courseId, int userId) {
        String sql = "DELETE FROM typing_status WHERE course_id = ? AND user_id = ?";
        try (Connection c = DBUtil.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, courseId);
            s.setInt(2, userId);
            s.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public List<String> getActiveTypers(int courseId) {
        List<String> typers = new ArrayList<>();
        // Only get people who typed in the last 6 seconds
        String sql = "SELECT user_name FROM typing_status WHERE course_id = ? AND last_typed > NOW() - INTERVAL 6 SECOND";
        try (Connection c = DBUtil.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, courseId);
            ResultSet rs = s.executeQuery();
            while (rs.next()) { typers.add(rs.getString("user_name")); }
        } catch (SQLException e) { e.printStackTrace(); }
        return typers;
    }

    public void markAsRead(int courseId, int userId) {
        // Marks messages in this course NOT sent by you as read
        String sql = "UPDATE messages SET is_read = true WHERE course_id = ? AND sender_id != ?";
        try (Connection c = DBUtil.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, courseId);
            s.setInt(2, userId);
            s.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }
}