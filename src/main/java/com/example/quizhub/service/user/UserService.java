package com.example.quizhub.service.user;

import com.example.quizhub.dto.user.response.UserResponseDTO;

public interface UserService {
    UserResponseDTO getUserById(Long id);
}
