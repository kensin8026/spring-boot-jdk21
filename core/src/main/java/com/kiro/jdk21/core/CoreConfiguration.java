package com.kiro.jdk21.core;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;

@Configuration
@ComponentScan(basePackages = "com.kiro.jdk21.core")
@EnableJdbcRepositories(basePackages = "com.kiro.jdk21.core.adapter.out.persistence")
public class CoreConfiguration {
}
