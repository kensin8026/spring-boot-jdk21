package com.kiro.jdk21.core.application.port.out;

import com.kiro.jdk21.core.domain.Team;

import java.util.List;

public interface TeamPort {
	List<Team> findAll();
	Team save(Team team);
}
