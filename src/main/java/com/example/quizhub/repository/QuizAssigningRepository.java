package com.example.quizhub.repository;

import com.example.quizhub.entity.QuizAssigning;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuizAssigningRepository extends JpaRepository<QuizAssigning, Long> {

    @EntityGraph(attributePaths = {"quiz", "topic"})
    List<QuizAssigning> findByClassroomId(Long classroomId);

    @EntityGraph(attributePaths = {"quiz", "topic"})
    List<QuizAssigning> findByQuizId(UUID quizId);

    @EntityGraph(attributePaths = {"quiz", "topic"})
    List<QuizAssigning> findByClassroomIdAndQuizId(Long classroomId, UUID quizId);

    @EntityGraph(attributePaths = {"quiz", "topic"})
    List<QuizAssigning> findByClassroomCreatorId(Long teacherId);
}
