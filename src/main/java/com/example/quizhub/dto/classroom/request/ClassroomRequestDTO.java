package com.example.quizhub.dto.classroom.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class ClassroomRequestDTO {
    @NotBlank(message = "Tên lớp không được để trống")
    String name;

    String description;

    String imageUrl;

    Boolean requireApproval;
}
