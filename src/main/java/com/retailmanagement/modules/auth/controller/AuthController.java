package com.retailmanagement.modules.auth.controller;

import com.retailmanagement.modules.auth.dto.request.LoginRequest;
import com.retailmanagement.modules.auth.dto.request.RegisterRequest;
import com.retailmanagement.modules.auth.dto.request.SwitchOrganizationRequest;
import com.retailmanagement.modules.auth.dto.response.JwtResponse;
import com.retailmanagement.modules.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication endpoints")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Login user")
    public ResponseEntity<JwtResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        return ResponseEntity.ok(authService.login(loginRequest));
    }

    @PostMapping("/register")
    @Operation(summary = "Register new user")
    public ResponseEntity<JwtResponse> register(@Valid @RequestBody RegisterRequest registerRequest) {
        return ResponseEntity.ok(authService.register(registerRequest));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String token) {
        authService.logout(token);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/switch-organization")
    @Operation(summary = "Switch active organization for the current account")
    public ResponseEntity<JwtResponse> switchOrganization(@Valid @RequestBody SwitchOrganizationRequest request) {
        return ResponseEntity.ok(authService.switchOrganization(request.getOrganizationId()));
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change password")
    public ResponseEntity<Void> changePassword(
            @RequestParam Long userId,
            @RequestParam String oldPassword,
            @RequestParam String newPassword) {
        authService.changePassword(userId, oldPassword, newPassword);
        return ResponseEntity.ok().build();
    }
}
