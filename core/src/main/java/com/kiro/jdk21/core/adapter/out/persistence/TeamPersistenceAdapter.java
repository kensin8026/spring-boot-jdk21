package com.kiro.jdk21.core.adapter.out.persistence;

import com.kiro.jdk21.core.application.port.out.TeamPort;
import com.kiro.jdk21.core.domain.Team;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.StreamSupport;

@Component
class TeamPersistenceAdapter implements TeamPort {

	private final TeamJdbcRepository teamJdbcRepository;
	private final TeamMapper teamMapper;

	TeamPersistenceAdapter(TeamJdbcRepository teamJdbcRepository, TeamMapper teamMapper) {
		this.teamJdbcRepository = teamJdbcRepository;
		this.teamMapper = teamMapper;
	}

	@Override
	public List<Team> findAll() {
		return StreamSupport.stream(teamJdbcRepository.findAll().spliterator(), false)
			.map(teamMapper::toDomain)
			.toList();
	}

	@Override
	public Team save(Team team) {
		TeamEntity entity = teamMapper.toEntity(team);
		TeamEntity saved = teamJdbcRepository.save(entity);
		return teamMapper.toDomain(saved);
	}
}
