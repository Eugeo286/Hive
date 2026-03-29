package org.example;

public class Admin extends User {
    private String clearanceLevel;

    public Admin() {
        super("Admin", "admin@thehive.com");
        this.clearanceLevel = "Superuser";
        this.role = "admin";
    }

    public Admin(String name, String email) {
        super(name, email);
        this.clearanceLevel = "Superuser";
        this.role = "admin";
    }

    public String getClearanceLevel() { return clearanceLevel; }
    public void setClearanceLevel(String clearanceLevel) { this.clearanceLevel = clearanceLevel; }
}