package com.kiro.jdk21.core.application.service;

import com.kiro.jdk21.core.application.port.in.UserUseCase;
import com.kiro.jdk21.core.application.port.out.UserPort;
import com.kiro.jdk21.core.domain.User;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService implements UserUseCase {

	private final UserPort userPort;

	public UserService(UserPort userPort) {
		this.userPort = userPort;
	}

	@Override
	public List<User> getUsers() {
		return userPort.findAll();
	}

	@Override
	public User createUser(String name, String email, Long teamId) {
		User newUser = new User(name, email, teamId);
		return userPort.save(newUser);
	}

	@Override
	public User updateUser(Long id, String name, String email, Long teamId) {
		User existingUser = userPort.findById(id)
			.orElseThrow(() -> new RuntimeException("User not found: " + id));
		
		User updatedUser = new User(id, name, email, teamId, existingUser.createdAt());
		return userPort.save(updatedUser);
	}

	@Override
	public void deleteUser(Long id) {
		userPort.deleteById(id);
	}
}
