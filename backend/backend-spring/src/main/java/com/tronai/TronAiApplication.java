package com.tronai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * TRON AI Spring Boot Application Entry Point
 * Step 1B Foundation — Health endpoint only.
 * Chat, Ollama, and MySQL connections will be added in subsequent steps.
 */
@SpringBootApplication
public class TronAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(TronAiApplication.class, args);
    }
}
