package org.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import java.util.List;

@SpringBootApplication
@EnableScheduling
public class HiveApplication {

    private final ContentDAO contentDAO = new ContentDAO();

    public static void main(String[] args) {
        SpringApplication.run(HiveApplication.class, args);
    }

    @Scheduled(fixedRate = 3_600_000)
    public void escalateUnansweredQuestions() {
        List<Question> urgent = contentDAO.getUnansweredOver24Hours();
        if (!urgent.isEmpty()) {
            System.out.println("\n[HIVE ALERT] " + urgent.size() + " question(s) unanswered for 24h+:");
            urgent.forEach(q -> System.out.println("  ⚠ [ID " + q.getId() + "] " + q.getTitle()));
        }
    }
}
