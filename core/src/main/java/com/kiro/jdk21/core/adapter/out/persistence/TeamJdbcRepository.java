package com.kiro.jdk21.core.adapter.out.persistence;

import org.springframework.data.repository.CrudRepository;

interface TeamJdbcRepository extends CrudRepository<TeamEntity, Long> {
}
