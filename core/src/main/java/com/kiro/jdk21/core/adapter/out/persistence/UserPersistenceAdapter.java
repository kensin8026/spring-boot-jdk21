package com.kiro.jdk21.core.adapter.out.persistence;

import com.kiro.jdk21.core.application.port.out.UserPort;
import com.kiro.jdk21.core.domain.User;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

@Component
class UserPersistenceAdapter implements UserPort {

	private final UserJdbcRepository userJdbcRepository;
	private final UserMapper userMapper;

	UserPersistenceAdapter(UserJdbcRepository userJdbcRepository, UserMapper userMapper) {
		this.userJdbcRepository = userJdbcRepository;
		this.userMapper = userMapper;
	}

	@Override
	public List<User> findAll() {
		return StreamSupport.stream(userJdbcRepository.findAll().spliterator(), false)
			.map(userMapper::toDomain)
			.toList();
	}

	@Override
	public Optional<User> findById(Long id) {
		return userJdbcRepository.findById(id)
			.map(userMapper::toDomain);
	}

	@Override
	public User save(User user) {
		UserEntity entity = userMapper.toEntity(user);
		UserEntity saved = userJdbcRepository.save(entity);
		return userMapper.toDomain(saved);
	}

	@Override
	public void deleteById(Long id) {
		userJdbcRepository.deleteById(id);
	}
}
