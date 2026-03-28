package org.example;

public abstract class User {
    protected int    id;
    protected String name;
    protected String email;
    protected int    points;
    protected String role; // <-- ADD THIS

    public User() {}
    public User(String name, String email) {
        this.name   = name;
        this.email  = email;
        this.points = 0;
    }

    public int    getId()           { return id; }
    public String getName()         { return name; }
    public String getEmail()        { return email; }
    public int    getPoints()       { return points; }
    public String getRole()         { return role; } // <-- ADD THIS

    public void   setId(int id)     { this.id = id; }
    public void   setName(String n) { this.name = n; }
    public void   setEmail(String e){ this.email = e; }
    public void   setPoints(int p)  { this.points = p; }
    public void   setRole(String r) { this.role = r; } // <-- ADD THIS
    public void   addPoints(int p)  { this.points += p; }
}