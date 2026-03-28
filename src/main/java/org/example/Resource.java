package org.example;

public class Resource {
    private int    id;
    private String title;
    private String link;
    private String course;
    private String postedBy;
    private String resourceType;
    private String originalFileName;

    public Resource() {}
    public Resource(String title, String link, String course, String postedBy, String resourceType) {
        this.title        = title;
        this.link         = link;
        this.course       = course;
        this.postedBy     = postedBy;
        this.resourceType = resourceType;
    }

    public int    getId()                          { return id; }
    public void   setId(int id)                    { this.id = id; }
    public String getTitle()                       { return title; }
    public void   setTitle(String t)               { this.title = t; }
    public String getLink()                        { return link; }
    public void   setLink(String l)                { this.link = l; }
    public String getCourse()                      { return course; }
    public void   setCourse(String c)              { this.course = c; }
    public String getPostedBy()                    { return postedBy; }
    public void   setPostedBy(String p)            { this.postedBy = p; }
    public String getResourceType()                { return resourceType; }
    public void   setResourceType(String t)        { this.resourceType = t; }
    public String getOriginalFileName()            { return originalFileName; }
    public void   setOriginalFileName(String f)    { this.originalFileName = f; }
}
