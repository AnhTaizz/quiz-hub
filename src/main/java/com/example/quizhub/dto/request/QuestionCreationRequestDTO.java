package com.example.quizhub.dto.request;

import java.util.List;

import com.example.quizhub.dto.AnswerDTO;
import com.example.quizhub.entity.enums.QuestionType;

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

public class QuestionCreationRequestDTO {
    Long categoryId;
    String text;
    QuestionType type;
    List<AnswerDTO> answers;
}
