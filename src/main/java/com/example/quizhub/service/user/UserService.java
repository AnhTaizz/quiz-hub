package com.example.quizhub.service.user;

import org.springframework.data.domain.Page;

import com.example.quizhub.dto.user.request.UpdateProfileRequest;
import com.example.quizhub.dto.user.response.UserProfileResponse;
import com.example.quizhub.dto.user.response.UserResponseDTO;
import com.example.quizhub.entity.enums.Role;

public interface UserService {
    UserResponseDTO getUserById(Long id);

    UserProfileResponse getMyProfile(String email);

    UserProfileResponse updateMyProfile(UpdateProfileRequest request, String email);

    void changeUserStatus(Long userId, boolean isEnable);

    void changeUserRole(Long userId, Role role);

    Page<UserProfileResponse> getAllUsers(String key, int page, int size);
}
