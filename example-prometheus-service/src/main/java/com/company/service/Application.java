package com.company.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication sa = new SpringApplication(Application.class);
        sa.addListeners(new EndpointsListener());
        sa.run(args);

    }

}