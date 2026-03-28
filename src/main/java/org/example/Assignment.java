package org.example;

import java.time.LocalDateTime;

public class Assignment {
    private int           id;
    private String        title;
    private String        description;
    private String        course;
    private String        teacherName;
    private LocalDateTime deadline;
    private String        answerText;

    public Assignment() {}
    public Assignment(String title, String description, String course,
                      String teacherName, LocalDateTime deadline, String answerText) {
        this.title       = title;
        this.description = description;
        this.course      = course;
        this.teacherName = teacherName;
        this.deadline    = deadline;
        this.answerText  = answerText;
    }

    public int           getId()                       { return id; }
    public void          setId(int id)                 { this.id = id; }
    public String        getTitle()                    { return title; }
    public void          setTitle(String t)            { this.title = t; }
    public String        getDescription()              { return description; }
    public void          setDescription(String d)      { this.description = d; }
    public String        getCourse()                   { return course; }
    public void          setCourse(String c)           { this.course = c; }
    public String        getTeacherName()              { return teacherName; }
    public void          setTeacherName(String t)      { this.teacherName = t; }
    public LocalDateTime getDeadline()                 { return deadline; }
    public void          setDeadline(LocalDateTime d)  { this.deadline = d; }
    public String        getAnswerText()               { return answerText; }
    public void          setAnswerText(String a)       { this.answerText = a; }
}
