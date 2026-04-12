package com.example.quizhub.dto.question.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AnswerCreationRequestDTO {
    @NotBlank(message="Đáp án không được để trống")
    private String text;
    
    @com.fasterxml.jackson.annotation.JsonProperty("isCorrect")
    @Builder.Default
    private Boolean correct = false; // Gán mặc định là false nếu FE quên gửi
}
