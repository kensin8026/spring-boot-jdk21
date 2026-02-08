package com.kiro.jdk21.api.adapter.in.web;

import com.kiro.jdk21.core.application.port.in.UserUseCase;
import com.kiro.jdk21.core.domain.User;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

	private final UserUseCase userUseCase;

	public UserController(UserUseCase userUseCase) {
		this.userUseCase = userUseCase;
	}

	@GetMapping
	public List<User> getUsers() {
		return userUseCase.getUsers();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public User createUser(@RequestBody CreateUserRequest request) {
		return userUseCase.createUser(request.name(), request.email(), request.teamId());
	}

	@PutMapping("/{id}")
	public User updateUser(@PathVariable Long id, @RequestBody UpdateUserRequest request) {
		return userUseCase.updateUser(id, request.name(), request.email(), request.teamId());
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteUser(@PathVariable Long id) {
		userUseCase.deleteUser(id);
	}

	record CreateUserRequest(String name, String email, Long teamId) {}
	record UpdateUserRequest(String name, String email, Long teamId) {}
}
