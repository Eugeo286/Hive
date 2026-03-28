package org.example;

public class Teacher extends User {
    private String department;

    public Teacher() {}
    public Teacher(String name, String email, String department) {
        super(name, email);
        this.department = department;
        this.role = "teacher"; // <-- ADD THIS
    }

    public String getDepartment()              { return department; }
    public void   setDepartment(String d)      { this.department = d; }

    public void answerEscalatedQuestion(Question q, String text, ContentDAO cDAO, UserDAO uDAO) {
        Answer ans = new Answer(text, this.getName());
        this.addPoints(5);
        cDAO.saveAnswer(q.getId(), ans);
        cDAO.markQuestionAnswered(q.getId());
        uDAO.updatePoints(this);
    }
}
