package com.arsh.workflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class WorkflowOrchestratorApplication {
	public static void main(String[] args) {
		SpringApplication.run(WorkflowOrchestratorApplication.class, args);
	}
}
