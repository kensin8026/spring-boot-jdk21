package com.kiro.jdk21.core.application.port.out;

import com.kiro.jdk21.core.domain.User;

import java.util.List;
import java.util.Optional;

public interface UserPort {
	List<User> findAll();
	Optional<User> findById(Long id);
	User save(User user);
	void deleteById(Long id);
}
