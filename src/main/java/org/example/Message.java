package org.example;

import java.time.LocalDateTime;

public class Message {
    private int id;
    private int senderId;
    private int courseId;
    private String content;
    private String senderName;
    private LocalDateTime sentAt;
    private boolean isRead;

    // Default constructor for JSON mapping
    public Message() {}

    // Constructor used by HiveController
    public Message(int senderId, int courseId, String content) {
        this.senderId = senderId;
        this.courseId = courseId;
        this.content = content;
        this.sentAt = LocalDateTime.now();
        this.isRead = false;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getSenderId() { return senderId; }
    public void setSenderId(int senderId) { this.senderId = senderId; }

    public int getCourseId() { return courseId; }
    public void setCourseId(int courseId) { this.courseId = courseId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
}