package com.retailmanagement.modules.auth.service.impl;

import com.retailmanagement.common.exceptions.BusinessException;
import com.retailmanagement.modules.auth.dto.request.LoginRequest;
import com.retailmanagement.modules.auth.dto.request.RegisterRequest;
import com.retailmanagement.modules.auth.dto.response.JwtResponse;
import com.retailmanagement.modules.auth.model.Role;
import com.retailmanagement.modules.auth.model.User;
import com.retailmanagement.modules.auth.repository.RoleRepository;
import com.retailmanagement.modules.auth.repository.UserRepository;
import com.retailmanagement.modules.auth.security.JwtTokenProvider;
import com.retailmanagement.modules.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    @Override
    public JwtResponse login(LoginRequest loginRequest) {
        log.info("Login attempt for user: {}", loginRequest.getUsername());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);

        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new BusinessException("User not found"));

        // Update last login
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        Set<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        return JwtResponse.builder()
                .token(jwt)
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(roles)
                .build();
    }

    @Override
    @Transactional
    public JwtResponse register(RegisterRequest request) {
        log.info("Registering new user: {}", request.getUsername());

        // Check if username exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("Username already exists");
        }

        // Check if email exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email already exists");
        }

        // Create new user
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .active(true)
                .build();

        // Assign default role (ROLE_USER)
        Set<Role> roles = new HashSet<>();
        Role userRole = roleRepository.findByName("ROLE_EMPLOYEE")
                .orElseThrow(() -> new BusinessException("Default role not found"));
        roles.add(userRole);
        user.setRoles(roles);

        User savedUser = userRepository.save(user);

        // Auto login after registration
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(request.getUsername());
        loginRequest.setPassword(request.getPassword());

        return login(loginRequest);
    }

    @Override
    public void logout(String token) {
        log.info("Logout user with token: {}", token);
        // Invalidate token (implement blacklist if needed)
        SecurityContextHolder.clearContext();
    }

    @Override
    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        log.info("Changing password for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found"));

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("Old password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}