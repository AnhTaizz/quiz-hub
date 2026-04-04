package com.example.quizhub.service.user;

import org.springframework.stereotype.Service;

import com.example.quizhub.dto.user.response.UserResponseDTO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    @Override
    public UserResponseDTO getUserById(Long id) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getUserById'");
    }

}
