package com.example.quizhub.repository;

import com.example.quizhub.entity.Question;
import com.example.quizhub.entity.enums.QuestionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findByCreatorId(Long creatorId);

    List<Question> findByCategoryId(Integer categoryId);

    List<Question> findByType(QuestionType type);

    List<Question> findByIsPublicTrue();
}
