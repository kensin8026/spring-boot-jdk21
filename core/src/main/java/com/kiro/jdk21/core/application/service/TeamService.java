package com.kiro.jdk21.core.application.service;

import com.kiro.jdk21.core.application.port.in.TeamUseCase;
import com.kiro.jdk21.core.application.port.out.TeamPort;
import com.kiro.jdk21.core.domain.Team;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TeamService implements TeamUseCase {

	private final TeamPort teamPort;

	public TeamService(TeamPort teamPort) {
		this.teamPort = teamPort;
	}

	@Override
	public List<Team> getTeams() {
		return teamPort.findAll();
	}

	@Override
	public Team createTeam(String name, String description) {
		Team newTeam = new Team(name, description);
		return teamPort.save(newTeam);
	}
}
