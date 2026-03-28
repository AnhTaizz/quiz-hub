package com.example.quizhub.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    @NotBlank(message = "BLANK_FIELD")
    private String username;
    @NotBlank(message = "BLANK_FIELD")
    @Email(message = "INVALID_EMAIL")
    private String email;
    @NotBlank(message = "BLANK_FIELD")
    @Size(min = 6, message = "INVALID_PASSWORD")
    private String password;

    private String role;
}
