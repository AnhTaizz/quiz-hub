package com.example.quizhub.dto.practice;

import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PracticeAnswerRequestDTO {
    
    @NotNull(message = "Question ID is required")
    Long questionId;

    // Can be null if the student didn't answer this question
    Long selectedAnswerId;
}
