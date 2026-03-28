package org.example;

import java.time.LocalDateTime;

public class Submission {
    private int           id;
    private int           assignmentId;
    private String        studentName;
    private String        content;
    private LocalDateTime submittedAt;

    public Submission() {}
    public Submission(int assignmentId, String studentName, String content) {
        this.assignmentId = assignmentId;
        this.studentName  = studentName;
        this.content      = content;
    }

    public int           getId()                          { return id; }
    public void          setId(int id)                    { this.id = id; }
    public int           getAssignmentId()                { return assignmentId; }
    public void          setAssignmentId(int a)           { this.assignmentId = a; }
    public String        getStudentName()                 { return studentName; }
    public void          setStudentName(String s)         { this.studentName = s; }
    public String        getContent()                     { return content; }
    public void          setContent(String c)             { this.content = c; }
    public LocalDateTime getSubmittedAt()                 { return submittedAt; }
    public void          setSubmittedAt(LocalDateTime t)  { this.submittedAt = t; }
}
