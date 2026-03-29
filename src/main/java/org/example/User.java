package org.example;

import java.util.ArrayList;
import java.util.List;

public abstract class User {
    protected int id;
    protected String name;
    protected String email;
    protected int points;
    protected String role;
    protected List<Course> enrolledCourses = new ArrayList<>();

    public User(String name, String email) {
        this.name = name;
        this.email = email;
        this.points = 0;
    }

    // 🟢 FIX FOR getName()
    public String getName() {
        return name;
    }

    // 🟢 FIX FOR addPoints()
    public void addPoints(int p) {
        this.points += p;
    }

    // Standard Getters/Setters for other fields
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public List<Course> getEnrolledCourses() { return enrolledCourses; }
    public void setEnrolledCourses(List<Course> enrolledCourses) { this.enrolledCourses = enrolledCourses; }
}