package org.example;

public class VideoSession {
    private int    id;
    private String topic;
    private String subject;
    private String host;
    private String scheduledTime;
    private String meetingLink;

    public VideoSession() {}
    public VideoSession(String topic, String subject, String host,
                        String scheduledTime, String meetingLink) {
        this.topic         = topic;
        this.subject       = subject;
        this.host          = host;
        this.scheduledTime = scheduledTime;
        this.meetingLink   = meetingLink;
    }

    public int    getId()                       { return id; }
    public void   setId(int id)                 { this.id = id; }
    public String getTopic()                    { return topic; }
    public void   setTopic(String t)            { this.topic = t; }
    public String getSubject()                  { return subject; }
    public void   setSubject(String s)          { this.subject = s; }
    public String getHost()                     { return host; }
    public void   setHost(String h)             { this.host = h; }
    public String getScheduledTime()            { return scheduledTime; }
    public void   setScheduledTime(String t)    { this.scheduledTime = t; }
    public String getMeetingLink()              { return meetingLink; }
    public void   setMeetingLink(String l)      { this.meetingLink = l; }

    public static void displaySession(VideoSession session) {
        System.out.println(session);
    }
}
