package com.kiro.jdk21.core.application.port.in;

import com.kiro.jdk21.core.domain.Team;

import java.util.List;

public interface TeamUseCase {
	List<Team> getTeams();
	Team createTeam(String name, String description);
}
