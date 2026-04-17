package com.example.quizhub.dto.question.request;

import java.util.List;

import com.example.quizhub.entity.enums.QuestionType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
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

public class QuestionRequestDTO {
    Long categoryId;

    @NotBlank(message="Câu hỏi không được để trống")
    String text;

    @NotNull(message="Loại câu hỏi không được để trống")
    QuestionType type;

    @NotEmpty(message="Câu hỏi phải có ít nhất 1 đáp án")
    List<@Valid AnswerCreationRequestDTO> answers;
}
