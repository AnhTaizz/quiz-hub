package com.example.quizhub.service.impl;

import java.util.Optional;

import com.example.quizhub.entity.enums.Role;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.quizhub.dto.request.AuthRequest;
import com.example.quizhub.dto.request.RegisterRequest;
import com.example.quizhub.dto.response.AuthResponse;
import com.example.quizhub.entity.User;
import com.example.quizhub.repository.UserRepository;
import com.example.quizhub.security.JwtService;
import com.example.quizhub.service.AuthService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email này đã được sử dụng!");
        }
        Role assignedRole = Role.STUDENT;
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getUsername())
                .role(assignedRole)
                .isEnable(true)
                .isVerified(false)
                .build();
        userRepository.save(user);
        String token = jwtService.generateToken(user);
        return AuthResponse.builder()
                .token(token)
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .build();
    }

    @Override
    public AuthResponse login(AuthRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        } catch (BadCredentialsException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sai email hoặc mật khẩu");
        }
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"));
        String token = jwtService.generateToken(user);
        return AuthResponse.builder()
                .token(token)
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .build();
    }
}
