package com.eventflow.notificationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class NotificationServiceApplication {

    public static void main(String[] args) {
        System.out.println(">>> EVENTFLOW DB HOST: [" + System.getenv("NEON_HOST") + "]");
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
