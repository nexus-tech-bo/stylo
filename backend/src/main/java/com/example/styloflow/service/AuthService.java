package com.example.styloflow.service;

import com.example.styloflow.dto.auth.LoginRequest;
import com.example.styloflow.dto.auth.LoginResponse;
import com.example.styloflow.dto.auth.PlatformLoginRequest;
import com.example.styloflow.dto.auth.PlatformLoginResponse;
import com.example.styloflow.dto.user.UserResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request);

    UserResponse findCurrentUser(Long userId);

    PlatformLoginResponse platformLogin(PlatformLoginRequest request);
}
