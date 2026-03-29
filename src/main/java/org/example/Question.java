package org.example;

import java.time.LocalDateTime;

public class Question {
    private int id;
    private String title;
    private String subject;
    private String difficulty;
    private String authorName;
    private boolean answered;
    private LocalDateTime createdAt;
    private String answerText;

    // 🟢 ADD THESE NEW FIELDS
    private int courseId;
    private String department;
    private String aiAnswer;

    // Existing Constructor (Update it or add a new one)
    public Question(String title, String subject, String difficulty, String authorName) {
        this.title = title;
        this.subject = subject;
        this.difficulty = difficulty;
        this.authorName = authorName;
    }

    // 🟢 ADD THESE GETTERS AND SETTERS
    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public int getCourseId() {
        return courseId;
    }

    public void setCourseId(int courseId) {
        this.courseId = courseId;
    }

    public String getAiAnswer() {
        return aiAnswer;
    }

    public void setAiAnswer(String aiAnswer) {
        this.aiAnswer = aiAnswer;
    }

    // ... (Keep all your existing getters/setters for title, subject, id, etc.) ...

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitle() { return title; }
    public String getSubject() { return subject; }
    public String getAuthorName() { return authorName; }
    public boolean isAnswered() { return answered; }
    public void setAnswered(boolean answered) { this.answered = answered; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setAnswerText(String answerText) { this.answerText = answerText; }
}