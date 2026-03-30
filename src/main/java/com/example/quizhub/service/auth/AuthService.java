package com.example.quizhub.service.auth;

import org.springframework.stereotype.Service;

import com.example.quizhub.dto.auth.response.AuthResponse;
import com.example.quizhub.dto.auth.request.AuthRequest;
import com.example.quizhub.dto.auth.request.ChangePasswordRequest;
import com.example.quizhub.dto.auth.request.RegisterRequest;

@Service
public interface AuthService {
    AuthResponse register(RegisterRequest request);

    AuthResponse login(AuthRequest request);

    void changePassword(ChangePasswordRequest request, String email);
}
