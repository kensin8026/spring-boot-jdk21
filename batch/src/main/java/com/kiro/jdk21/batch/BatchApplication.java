package com.kiro.jdk21.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.kiro.jdk21.batch", "com.kiro.jdk21.core"})
public class BatchApplication {

	public static void main(String[] args) {
		System.exit(SpringApplication.exit(SpringApplication.run(BatchApplication.class, args)));
	}

}
