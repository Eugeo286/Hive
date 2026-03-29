package org.example;

public class Course {
    private int id;
    private String courseCode;
    private String title;
    private String description;

    public Course() {}

    public Course(String courseCode, String title, String description) {
        this.courseCode = courseCode;
        this.title = title;
        this.description = description;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}