package org.example; // This matches your folder structure seen in the screenshot

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @GetMapping("/stats")
    public Map<String, Object> getStudentStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("coursesJoined", 4);
        stats.put("skillPoints", 1250);
        stats.put("hoursStudied", 42);
        return stats;
    }

    @GetMapping("/courses")
    public List<Map<String, String>> getActiveCourses() {
        List<Map<String, String>> courses = new ArrayList<>();

        Map<String, String> course1 = new HashMap<>();
        course1.put("title", "Java Spring Boot Mastery");
        course1.put("progress", "75");
        course1.put("category", "Backend");

        Map<String, String> course2 = new HashMap<>();
        course2.put("title", "Advanced Database Systems");
        course2.put("progress", "30");
        course2.put("category", "Data");

        courses.add(course1);
        courses.add(course2);

        return courses;
    }
}