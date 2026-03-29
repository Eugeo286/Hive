package org.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@SpringBootApplication
@EnableScheduling // 🔴 1. THIS TURNS ON THE BACKGROUND CLOCK
public class HiveApplication {

    public static void main(String[] args) {
        SpringApplication.run(HiveApplication.class, args);
    }

    // 🔴 2. THE TIMER: This tells Java to run this exact code at Midnight every day
    @Scheduled(cron = "0 0 0 * * ?")
    public void runDailyCleanup() {
        System.out.println("⏰ Midnight reached! Starting database cleanup...");
        ContentDAO dao = new ContentDAO();
        dao.deleteOldQuestions();
    }
}