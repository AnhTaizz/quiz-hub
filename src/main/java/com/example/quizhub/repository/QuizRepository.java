package com.example.quizhub.repository;

import com.example.quizhub.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, UUID> {

    List<Quiz> findByCreatorId(Long creatorId);

    List<Quiz> findByIsDraftFalseAndIsEnableTrue();

    List<Quiz> findByIsExamTrue();
}
