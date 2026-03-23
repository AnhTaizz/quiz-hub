package com.example.quizhub.repository;

import com.example.quizhub.entity.QuizAssigning;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuizAssigningRepository extends JpaRepository<QuizAssigning, Long> {

    List<QuizAssigning> findByClassroomId(Long classroomId);

    List<QuizAssigning> findByQuizId(UUID quizId);

    List<QuizAssigning> findByClassroomIdAndQuizId(Long classroomId, UUID quizId);
}
