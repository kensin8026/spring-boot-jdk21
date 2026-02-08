package com.kiro.jdk21.core.adapter.out.persistence;

import com.kiro.jdk21.core.domain.Team;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
interface TeamMapper {
	Team toDomain(TeamEntity entity);
	TeamEntity toEntity(Team domain);
}
