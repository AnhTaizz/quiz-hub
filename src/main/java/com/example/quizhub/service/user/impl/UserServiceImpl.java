package com.example.quizhub.service.user.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.example.quizhub.dto.user.request.UpdateProfileRequest;
import com.example.quizhub.dto.user.response.UserProfileResponse;
import com.example.quizhub.dto.user.response.UserResponseDTO;
import com.example.quizhub.entity.User;
import com.example.quizhub.entity.enums.Role;
import com.example.quizhub.exception.AppException;
import com.example.quizhub.exception.ErrorCode;
import com.example.quizhub.repository.UserRepository;
import com.example.quizhub.service.user.UserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public UserResponseDTO getUserById(Long id) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getUserById'");
    }

    @Override
    public UserProfileResponse getMyProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .build();
    }

    @Override
    public UserProfileResponse updateMyProfile(UpdateProfileRequest request, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setAvatarUrl(request.getAvatarUrl());
        userRepository.save(user);
        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .build();
    }

    @Override
    public void changeUserStatus(Long userId, boolean isEnable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        user.setIsEnable(isEnable);
        userRepository.save(user);
    }

    @Override
    public void changeUserRole(Long userId, Role role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        user.setRole(role);
        userRepository.save(user);
    }

    @Override
    public Page<UserProfileResponse> getAllUsers(String keyword, int page, int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<User> userPage;

        if (keyword == null || keyword.trim().isEmpty()) {
            userPage = userRepository.findAll(pageable);
        } else {
            userPage = userRepository.searchUsers(keyword, pageable);
        }

        return userPage.map(user -> UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .build());
    }
}
