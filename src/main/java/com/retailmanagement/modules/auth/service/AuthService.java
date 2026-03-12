package com.retailmanagement.modules.auth.service;

import com.retailmanagement.modules.auth.dto.request.LoginRequest;
import com.retailmanagement.modules.auth.dto.request.RegisterRequest;
import com.retailmanagement.modules.auth.dto.response.JwtResponse;

public interface AuthService {
    JwtResponse login(LoginRequest loginRequest);
    JwtResponse register(RegisterRequest registerRequest);
    void logout(String token);
    void changePassword(Long userId, String oldPassword, String newPassword);
}