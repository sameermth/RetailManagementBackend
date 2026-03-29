package com.retailmanagement.modules.auth.service;

import com.retailmanagement.modules.auth.dto.request.ChangePasswordPayload;
import com.retailmanagement.modules.auth.dto.request.UpdateProfileRequest;
import com.retailmanagement.modules.auth.dto.response.ProfileResponse;

public interface ProfileService {
    ProfileResponse currentProfile();
    ProfileResponse updateCurrentProfile(UpdateProfileRequest request);
    void changeCurrentPassword(ChangePasswordPayload request);
}
