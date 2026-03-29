package org.example;

import java.time.LocalDateTime;

public class Message {
    private int id;
    private int senderId;
    private String senderName; // We will populate this via SQL JOIN for the UI
    private int courseId;
    private String content;
    private LocalDateTime sentAt;

    public Message() {}

    public Message(int senderId, int courseId, String content) {
        this.senderId = senderId;
        this.courseId = courseId;
        this.content = content;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getSenderId() { return senderId; }
    public void setSenderId(int senderId) { this.senderId = senderId; }
    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }
    public int getCourseId() { return courseId; }
    public void setCourseId(int courseId) { this.courseId = courseId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
}