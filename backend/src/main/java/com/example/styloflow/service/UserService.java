package com.example.styloflow.service;

import com.example.styloflow.dto.user.ChangePasswordRequest;
import com.example.styloflow.dto.user.UserRequest;
import com.example.styloflow.dto.user.UserResponse;
import com.example.styloflow.dto.user.UserSummaryResponse;

import java.util.List;

/** Users of the business of the current request (tenant schema). */
public interface UserService {
    List<UserResponse> findAllUsers();

    List<UserSummaryResponse> findActiveStylists();

    UserResponse findUserById(Long userId);

    /** @throws com.example.styloflow.exception.BusinessRuleException without password
     *  @throws com.example.styloflow.exception.ConflictException duplicate username */
    UserResponse createUser(UserRequest request);

    /** @throws com.example.styloflow.exception.BusinessRuleException when it would leave no active ADMIN
     *  @throws com.example.styloflow.exception.ConflictException duplicate username */
    UserResponse updateUserById(Long userId, UserRequest request);

    void changePassword(Long userId, ChangePasswordRequest request);
}
