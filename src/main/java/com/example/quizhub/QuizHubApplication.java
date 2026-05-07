package com.example.quizhub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class QuizHubApplication {
	public static void main(String[] args) {
		SpringApplication.run(QuizHubApplication.class, args);
	}

}
