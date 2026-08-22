package com.eventflow.incidentanalyzer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = {"com.eventflow.incidentanalyzer", "com.eventflow.common"})
public class IncidentAnalyzerApplication {

    public static void main(String[] args) {
        SpringApplication.run(IncidentAnalyzerApplication.class, args);
    }
}
