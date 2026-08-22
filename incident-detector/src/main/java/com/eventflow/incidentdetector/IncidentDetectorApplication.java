package com.eventflow.incidentdetector;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = {"com.eventflow.incidentdetector", "com.eventflow.common"})
public class IncidentDetectorApplication {

    public static void main(String[] args) {
        SpringApplication.run(IncidentDetectorApplication.class, args);
    }
}
