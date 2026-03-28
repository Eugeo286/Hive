package org.example;

public class QuizQuestion {
    private int id;
    private int quizId;
    private String questionText;
    private String optA;
    private String optB;
    private String optC;
    private String optD;
    private String correctOpt;

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getQuizId() { return quizId; }
    public void setQuizId(int qid) { this.quizId = qid; }
    public String getQuestionText() { return questionText; }
    public void setQuestionText(String qt) { this.questionText = qt; }
    public String getOptA() { return optA; }
    public void setOptA(String a) { this.optA = a; }
    public String getOptB() { return optB; }
    public void setOptB(String b) { this.optB = b; }
    public String getOptC() { return optC; }
    public void setOptC(String c) { this.optC = c; }
    public String getOptD() { return optD; }
    public void setOptD(String d) { this.optD = d; }
    public String getCorrectOpt() { return correctOpt; }
    public void setCorrectOpt(String co) { this.correctOpt = co; }
}