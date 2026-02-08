package com.kiro.jdk21.core.application.port.in;

import com.kiro.jdk21.core.domain.User;

import java.util.List;

public interface UserUseCase {
	List<User> getUsers();
	User createUser(String name, String email, Long teamId);
	User updateUser(Long id, String name, String email, Long teamId);
	void deleteUser(Long id);
}
