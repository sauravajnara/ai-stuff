package com.rag.engine.ragsearchengine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RagSearchEngineApplication {

	public static void main(String[] args) {
		SpringApplication.run(RagSearchEngineApplication.class, args);
	}
}
