package com.kiro.jdk21.core.adapter.out.persistence;

import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

interface UserJdbcRepository extends CrudRepository<UserEntity, Long> {
	Optional<UserEntity> findById(Long id);
}
