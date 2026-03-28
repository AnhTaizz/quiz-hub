package com.example.quizhub.service;

import org.springframework.stereotype.Service;

import com.example.quizhub.dto.request.AuthRequest;
import com.example.quizhub.dto.request.RegisterRequest;
import com.example.quizhub.dto.response.AuthResponse;

@Service
public interface AuthService {
    AuthResponse register(RegisterRequest request);

    AuthResponse login(AuthRequest request);
}
