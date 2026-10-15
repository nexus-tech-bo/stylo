package com.example.styloflow.service.impl;

import com.example.styloflow.dto.user.ChangePasswordRequest;
import com.example.styloflow.dto.user.UserRequest;
import com.example.styloflow.entity.User;
import com.example.styloflow.enums.Role;
import com.example.styloflow.exception.BusinessRuleException;
import com.example.styloflow.exception.ConflictException;
import com.example.styloflow.exception.NotFoundException;
import com.example.styloflow.mapper.UserMapper;
import com.example.styloflow.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
    @Mock
    UserRepository userRepository;
    @Mock
    PasswordEncoder passwordEncoder;

    UserServiceImpl service;

    @BeforeEach
    void setUp() {
        // Real mapper: the null-keeps-value rules live there.
        var mapper = Mappers.getMapper(UserMapper.class);
        service = new UserServiceImpl(userRepository, mapper, passwordEncoder);
    }

    private static UserRequest request(String username, String password, Role role, Boolean active) {
        return new UserRequest("Ana", username, password, role, null, null, active);
    }

    private static User admin(Long id) {
        return User.builder().id(id).name("Admin").username("admin").passwordHash("hash").role(Role.ADMIN).build();
    }

    @Test
    void createUser_should_throw422_when_passwordMissing() {
        assertThatThrownBy(() -> service.createUser(request("ana", null, Role.STYLIST, null)))
                .isInstanceOf(BusinessRuleException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUser_should_throw409_when_usernameTaken() {
        when(userRepository.existsByUsername("ana")).thenReturn(true);

        assertThatThrownBy(() -> service.createUser(request("ana", "secret1", Role.STYLIST, null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void createUser_should_encodePasswordAndApplyDefaults_when_valid() {
        when(passwordEncoder.encode("secret1")).thenReturn("encoded");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = service.createUser(request("ana", "secret1", Role.STYLIST, null));

        var saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("encoded");
        assertThat(response.active()).isTrue();
        assertThat(response.commissionRate()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void updateUserById_should_throw404_when_missing() {
        when(userRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateUserById(9L, request("ana", null, Role.STYLIST, null)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateUserById_should_throw409_when_newUsernameTaken() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin(1L)));
        when(userRepository.existsByUsername("taken")).thenReturn(true);

        assertThatThrownBy(() -> service.updateUserById(1L, request("taken", null, Role.ADMIN, null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void updateUserById_should_throw422_when_demotingLastActiveAdmin() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin(1L)));
        when(userRepository.countByRoleAndActiveTrue(Role.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> service.updateUserById(1L, request("admin", null, Role.CASHIER, null)))
                .isInstanceOf(BusinessRuleException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUserById_should_throw422_when_deactivatingLastActiveAdmin() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin(1L)));
        when(userRepository.countByRoleAndActiveTrue(Role.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> service.updateUserById(1L, request("admin", null, Role.ADMIN, false)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void updateUserById_should_demoteAndKeepPassword_when_anotherAdminExists() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin(1L)));
        when(userRepository.countByRoleAndActiveTrue(Role.ADMIN)).thenReturn(2L);
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = service.updateUserById(1L, request("admin", null, Role.CASHIER, null));

        assertThat(response.role()).isEqualTo(Role.CASHIER);
        assertThat(response.active()).isTrue();
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void changePassword_should_saveEncodedPassword() {
        var user = admin(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("nueva123")).thenReturn("encoded");

        service.changePassword(1L, new ChangePasswordRequest("nueva123"));

        assertThat(user.getPasswordHash()).isEqualTo("encoded");
        verify(userRepository).save(user);
    }
}
