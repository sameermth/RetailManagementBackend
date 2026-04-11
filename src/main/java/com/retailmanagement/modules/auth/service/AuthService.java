package com.retailmanagement.modules.auth.service;

import com.retailmanagement.modules.auth.dto.request.LoginRequest;
import com.retailmanagement.modules.auth.dto.request.RefreshTokenRequest;
import com.retailmanagement.modules.auth.dto.request.RegisterRequest;
import com.retailmanagement.modules.auth.dto.response.JwtResponse;

public interface AuthService {
    JwtResponse login(LoginRequest loginRequest, String userAgent, String deviceId, String deviceName);
    JwtResponse refresh(RefreshTokenRequest refreshTokenRequest, String userAgent, String deviceId, String deviceName);
    JwtResponse register(RegisterRequest registerRequest);
    JwtResponse switchOrganization(Long organizationId);
    void logout(String token, RefreshTokenRequest refreshTokenRequest);
    void changePassword(Long userId, String oldPassword, String newPassword);
}
