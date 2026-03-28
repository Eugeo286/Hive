package org.example;

import java.util.ArrayList;

public class Student extends User {
    private ArrayList<String> strongSubjects = new ArrayList<>();

    public Student() {}
    public Student(String name, String email) {
        super(name, email);
        this.role = "student"; // <-- ADD THIS
    }

    public void addStrongSubject(String s) { strongSubjects.add(s); }
    public ArrayList<String> getStrongSubjects() { return strongSubjects; }

    public void answerQuestion(Question q, String text, UserDAO uDAO, ContentDAO cDAO) {
        Answer ans = new Answer(text, this.getName());
        this.addPoints(10);
        cDAO.saveAnswer(q.getId(), ans);
        uDAO.updatePoints(this);
    }
}
