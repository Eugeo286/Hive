package org.example;

public class Answer {
    private int     id;
    private String  text;
    private String  authorName;
    private boolean verified;

    public Answer() {}
    public Answer(String text, String authorName) {
        this.text       = text;
        this.authorName = authorName;
        this.verified   = false;
    }

    public int     getId()                    { return id; }
    public void    setId(int id)              { this.id = id; }
    public String  getText()                  { return text; }
    public void    setText(String t)          { this.text = t; }
    public String  getAuthorName()            { return authorName; }
    public void    setAuthorName(String a)    { this.authorName = a; }
    public boolean isVerified()               { return verified; }
    public void    setVerified(boolean v)     { this.verified = v; }
}
