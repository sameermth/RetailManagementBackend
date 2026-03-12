package com.retailmanagement.modules.auth.mapper;

import com.retailmanagement.modules.auth.dto.response.UserResponse;
import com.retailmanagement.modules.auth.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    @Mapping(target = "roles", expression = "java(user.getRoles().stream().map(role -> role.getName()).collect(java.util.stream.Collectors.toSet()))")
    UserResponse toResponse(User user);

    List<UserResponse> toResponseList(List<User> users);
}