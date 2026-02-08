package com.kiro.jdk21.core.adapter.out.persistence;

import com.kiro.jdk21.core.domain.User;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
interface UserMapper {
	User toDomain(UserEntity entity);
	UserEntity toEntity(User domain);
}
