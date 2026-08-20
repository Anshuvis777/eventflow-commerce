package com.eventflow.incidentquery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.eventflow.incidentquery", "com.eventflow.common"})
public class IncidentQueryApplication {

    public static void main(String[] args) {
        SpringApplication.run(IncidentQueryApplication.class, args);
    }
}
