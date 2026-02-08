package com.kiro.jdk21.core.domain;

import java.time.LocalDateTime;

public record User(
	Long id,
	String name,
	String email,
	Long teamId,
	LocalDateTime createdAt
) {
	public User(String name, String email, Long teamId) {
		this(null, name, email, teamId, LocalDateTime.now());
	}
}
