package org.example;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/quizzes")
@CrossOrigin(origins = "*")
public class QuizController {

    @PostMapping("/create")
    public ResponseEntity<String> createQuiz(@RequestBody Quiz quiz) {
        String sqlQuiz = "INSERT INTO quizzes (title, subject, duration_minutes, teacher_name) VALUES (?, ?, ?, ?)";
        String sqlQuestion = "INSERT INTO quiz_questions (quiz_id, question_text, opt_a, opt_b, opt_c, opt_d, correct_opt) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection c = DBUtil.getConnection()) {
            c.setAutoCommit(false); // Start transaction

            // 1. Save the Quiz
            try (PreparedStatement sQuiz = c.prepareStatement(sqlQuiz, Statement.RETURN_GENERATED_KEYS)) {
                sQuiz.setString(1, quiz.getTitle());
                sQuiz.setString(2, quiz.getSubject());
                sQuiz.setInt(3, quiz.getDurationMinutes());
                sQuiz.setString(4, quiz.getTeacherName());
                sQuiz.executeUpdate();

                ResultSet rs = sQuiz.getGeneratedKeys();
                if (rs.next()) {
                    int newQuizId = rs.getInt(1);

                    // 2. Save all Questions linked to this Quiz
                    if (quiz.getQuestions() != null) {
                        try (PreparedStatement sQ = c.prepareStatement(sqlQuestion)) {
                            for (QuizQuestion q : quiz.getQuestions()) {
                                sQ.setInt(1, newQuizId);
                                sQ.setString(2, q.getQuestionText());
                                sQ.setString(3, q.getOptA());
                                sQ.setString(4, q.getOptB());
                                sQ.setString(5, q.getOptC());
                                sQ.setString(6, q.getOptD());
                                sQ.setString(7, q.getCorrectOpt());
                                sQ.addBatch();
                            }
                            sQ.executeBatch();
                        }
                    }
                }
            }
            c.commit();
            return ResponseEntity.ok("Quiz Deployed Successfully!");
        } catch (SQLException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error saving quiz.");
        }
    }

    @GetMapping("/all")
    public ResponseEntity<List<Quiz>> getAllQuizzes() {
        List<Quiz> quizzes = new ArrayList<>();
        String sql = "SELECT * FROM quizzes ORDER BY created_at DESC";

        try (Connection c = DBUtil.getConnection(); Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) {
                Quiz q = new Quiz();
                q.setId(rs.getInt("id"));
                q.setTitle(rs.getString("title"));
                q.setSubject(rs.getString("subject"));
                q.setDurationMinutes(rs.getInt("duration_minutes"));
                q.setTeacherName(rs.getString("teacher_name"));
                quizzes.add(q);
            }
            return ResponseEntity.ok(quizzes);
        } catch (SQLException e) {
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/{id}/questions")
    public ResponseEntity<List<QuizQuestion>> getQuizQuestions(@PathVariable int id) {
        List<QuizQuestion> questions = new ArrayList<>();
        String sql = "SELECT * FROM quiz_questions WHERE quiz_id = ?";

        try (Connection c = DBUtil.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, id);
            ResultSet rs = s.executeQuery();
            while (rs.next()) {
                QuizQuestion q = new QuizQuestion();
                q.setId(rs.getInt("id"));
                q.setQuestionText(rs.getString("question_text"));
                q.setOptA(rs.getString("opt_a"));
                q.setOptB(rs.getString("opt_b"));
                q.setOptC(rs.getString("opt_c"));
                q.setOptD(rs.getString("opt_d"));
                // Note: In a strict production environment, you wouldn't send correctOpt to the frontend until submission.
                // We send it here so the frontend JavaScript can auto-grade instantly.
                q.setCorrectOpt(rs.getString("correct_opt"));
                questions.add(q);
            }
            return ResponseEntity.ok(questions);
        } catch (SQLException e) {
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping("/submit-attempt")
    public ResponseEntity<String> saveAttempt(@RequestBody Map<String, Object> body) {
        String sql = "INSERT INTO quiz_attempts (quiz_id, student_name, score, total) VALUES (?, ?, ?, ?)";
        try (Connection c = DBUtil.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, (Integer) body.get("quizId"));
            s.setString(2, (String) body.get("studentName"));
            s.setInt(3, (Integer) body.get("score"));
            s.setInt(4, (Integer) body.get("total"));
            s.executeUpdate();

            // Optionally add points to the user here using UserDAO
            return ResponseEntity.ok("Score recorded!");
        } catch (SQLException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error recording score.");
        }
    }
}