package com.kiro.jdk21.api.adapter.in.web;

import com.kiro.jdk21.core.application.port.in.TeamUseCase;
import com.kiro.jdk21.core.domain.Team;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/teams")
public class TeamController {

	private final TeamUseCase teamUseCase;

	public TeamController(TeamUseCase teamUseCase) {
		this.teamUseCase = teamUseCase;
	}

	@GetMapping
	public List<Team> getTeams() {
		return teamUseCase.getTeams();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public Team createTeam(@RequestBody CreateTeamRequest request) {
		return teamUseCase.createTeam(request.name(), request.description());
	}

	record CreateTeamRequest(String name, String description) {}
}
