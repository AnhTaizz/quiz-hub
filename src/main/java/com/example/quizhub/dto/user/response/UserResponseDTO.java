package com.example.quizhub.dto.user.response;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserResponseDTO {
    private Long id;
    private String fullName;
    private String email;
    private String role;
}
