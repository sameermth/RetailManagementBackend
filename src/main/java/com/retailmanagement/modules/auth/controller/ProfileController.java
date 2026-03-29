package com.retailmanagement.modules.auth.controller;

import com.retailmanagement.modules.auth.dto.request.ChangePasswordPayload;
import com.retailmanagement.modules.auth.dto.request.UpdateProfileRequest;
import com.retailmanagement.modules.auth.dto.response.ProfileResponse;
import com.retailmanagement.modules.auth.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/profile")
@RequiredArgsConstructor
@Tag(name = "Profile", description = "Current user profile endpoints")
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    @Operation(summary = "Get current profile")
    public ResponseEntity<ProfileResponse> current() {
        return ResponseEntity.ok(profileService.currentProfile());
    }

    @PutMapping
    @Operation(summary = "Update current profile")
    public ResponseEntity<ProfileResponse> update(@Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(profileService.updateCurrentProfile(request));
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change current profile password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordPayload request) {
        profileService.changeCurrentPassword(request);
        return ResponseEntity.ok().build();
    }
}
