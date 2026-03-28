package org.example;

import java.util.List;

public class Quiz {
    private int id;
    private String title;
    private String subject;
    private int durationMinutes;
    private String teacherName;
    private List<QuizQuestion> questions;

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String t) { this.title = t; }
    public String getSubject() { return subject; }
    public void setSubject(String s) { this.subject = s; }
    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int d) { this.durationMinutes = d; }
    public String getTeacherName() { return teacherName; }
    public void setTeacherName(String t) { this.teacherName = t; }
    public List<QuizQuestion> getQuestions() { return questions; }
    public void setQuestions(List<QuizQuestion> q) { this.questions = q; }
}