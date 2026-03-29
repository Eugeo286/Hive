package org.example;

import java.time.LocalDateTime;

public class Question {
    private int           id;
    private String        title;
    private String        subject;
    private String        difficulty;
    private String        authorName;
    private boolean       answered;
    private LocalDateTime createdAt;

    // 🔴 ADDED: This holds the text of the answer so the frontend can display it!
    private String        answerText;

    public Question() {}
    public Question(String title, String subject, String difficulty, String authorName) {
        this.title      = title;
        this.subject    = subject;
        this.difficulty = difficulty;
        this.authorName = authorName;
        this.answered   = false;
    }

    public int           getId()                          { return id; }
    public void          setId(int id)                    { this.id = id; }
    public String        getTitle()                       { return title; }
    public void          setTitle(String t)               { this.title = t; }
    public String        getSubject()                     { return subject; }
    public void          setSubject(String s)             { this.subject = s; }
    public String        getDifficulty()                  { return difficulty; }
    public void          setDifficulty(String d)          { this.difficulty = d; }
    public String        getAuthorName()                  { return authorName; }
    public void          setAuthorName(String a)          { this.authorName = a; }
    public boolean       isAnswered()                     { return answered; }
    public void          setAnswered(boolean a)           { this.answered = a; }
    public LocalDateTime getCreatedAt()                   { return createdAt; }
    public void          setCreatedAt(LocalDateTime t)    { this.createdAt = t; }

    // 🔴 ADDED: Getters and Setters for the answer text
    public String        getAnswerText()                  { return answerText; }
    public void          setAnswerText(String text)       { this.answerText = text; }
}