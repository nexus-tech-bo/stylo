package com.example.styloflow.mapper;

import com.example.styloflow.dto.user.UserRequest;
import com.example.styloflow.dto.user.UserResponse;
import com.example.styloflow.dto.user.UserSummaryResponse;
import com.example.styloflow.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(config = MapStructConfig.class)
public interface UserMapper {
    UserResponse toResponse(User user);

    List<UserResponse> toResponseList(List<User> users);

    UserSummaryResponse toSummary(User user);

    List<UserSummaryResponse> toSummaryList(List<User> users);

    /** Copies the request onto {@code user}; {@code null} commissionRate/active keep the current value. Password is the service's job. */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "commissionRate", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "active", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void toEntityUpdated(UserRequest request, @MappingTarget User user);
}
