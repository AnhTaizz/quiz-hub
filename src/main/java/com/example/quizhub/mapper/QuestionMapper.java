package com.example.quizhub.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.example.quizhub.dto.question.AnswerResponseDTO;
import com.example.quizhub.dto.question.QuestionResponseDTO;
import com.example.quizhub.entity.Answer;
import com.example.quizhub.entity.Question;

@Mapper(componentModel = "spring")
public interface QuestionMapper {

    // Answer -> AnswerResponseDTO (các field trùng tên, MapStruct tự map)
    AnswerResponseDTO toResponseDTO(Answer answer);

    // Question -> QuestionResponseDTO
    @Mapping(source = "creator.id",       target = "creatorId")
    @Mapping(source = "creator.email",    target = "creatorEmail")
    @Mapping(source = "category.id",      target = "categoryId")
    @Mapping(source = "category.name",    target = "categoryName")
    @Mapping(source = "questionStatus",   target = "questionStatus")
    QuestionResponseDTO toResponseDTO(Question question);

    // List<Question> -> List<QuestionResponseDTO>
    List<QuestionResponseDTO> toResponseList(List<Question> questions);
}
