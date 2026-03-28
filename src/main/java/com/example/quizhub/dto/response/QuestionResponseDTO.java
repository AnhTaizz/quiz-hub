package com.example.quizhub.dto.response;

import java.util.List;

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
public class QuestionResponseDTO {
    Long id;
    String text;
    QuestionType type;
    Boolean isActive;
    Boolean isPublic;

    // Chỉ lấy thông tin cơ bản của creator, không dùng cả entity User
    Long creatorId;
    String creatorEmail;

    // Chỉ lấy thông tin cơ bản của category
    Integer categoryId;
    String categoryName;

    // Danh sách câu trả lời dạng DTO (không chứa back-reference tới Question)
    List<AnswerResponseDTO> answers;
}
