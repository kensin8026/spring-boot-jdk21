package com.kiro.jdk21.core.adapter.out.persistence;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("users")
record UserEntity(
	@Id Long id,
	String name,
	String email,
	Long teamId,
	LocalDateTime createdAt
) {}
