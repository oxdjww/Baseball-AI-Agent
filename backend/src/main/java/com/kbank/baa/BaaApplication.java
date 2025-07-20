package com.kbank.baa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BaaApplication {
    public static void main(String[] args) {
        SpringApplication.run(BaaApplication.class, args);
    }
}
