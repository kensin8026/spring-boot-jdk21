package com.kiro.jdk21.core.domain;

import java.time.LocalDateTime;

public record Team(
	Long id,
	String name,
	String description,
	LocalDateTime createdAt
) {
	public Team(String name, String description) {
		this(null, name, description, LocalDateTime.now());
	}
}
