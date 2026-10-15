package com.example.styloflow.service.impl;

import com.example.styloflow.dto.user.ChangePasswordRequest;
import com.example.styloflow.dto.user.UserRequest;
import com.example.styloflow.dto.user.UserResponse;
import com.example.styloflow.dto.user.UserSummaryResponse;
import com.example.styloflow.entity.User;
import com.example.styloflow.enums.Role;
import com.example.styloflow.exception.BusinessRuleException;
import com.example.styloflow.exception.ConflictException;
import com.example.styloflow.exception.NotFoundException;
import com.example.styloflow.mapper.UserMapper;
import com.example.styloflow.repository.UserRepository;
import com.example.styloflow.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public List<UserResponse> findAllUsers() {
        return userMapper.toResponseList(userRepository.findAll(Sort.by("name", "id")));
    }

    @Override
    public List<UserSummaryResponse> findActiveStylists() {
        return userMapper.toSummaryList(userRepository.findByRoleAndActiveTrueOrderByNameAsc(Role.STYLIST));
    }

    @Override
    public UserResponse findUserById(Long userId) {
        return userMapper.toResponse(getUserOrThrowById(userId));
    }

    @Override
    @Transactional
    public UserResponse createUser(UserRequest request) {
        if (request.password() == null)
            throw new BusinessRuleException("Password is required for a new user");
        if (userRepository.existsByUsername(request.username()))
            throw new ConflictException("Username already exists: " + request.username());
        var user = User.builder().build(); // keeps the entity defaults for omitted commissionRate/active
        userMapper.toEntityUpdated(request, user);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserResponse updateUserById(Long userId, UserRequest request) {
        var user = getUserOrThrowById(userId);
        if (!user.getUsername().equals(request.username()) && userRepository.existsByUsername(request.username()))
            throw new ConflictException("Username already exists: " + request.username());
        var losesAdmin = request.role() != Role.ADMIN || Boolean.FALSE.equals(request.active());
        if (user.isActive() && user.getRole() == Role.ADMIN && losesAdmin && userRepository.countByRoleAndActiveTrue(Role.ADMIN) <= 1)
            throw new BusinessRuleException("The business must keep at least one active ADMIN");
        userMapper.toEntityUpdated(request, user);
        if (request.password() != null)
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        var user = getUserOrThrowById(userId);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        userRepository.save(user);
    }

    private User getUserOrThrowById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + userId));
    }
}
