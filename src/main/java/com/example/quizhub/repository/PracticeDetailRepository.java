package com.example.quizhub.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.quizhub.entity.PracticeDetail;

@Repository
public interface PracticeDetailRepository extends JpaRepository<PracticeDetail, Long> {
    Optional<PracticeDetail> findFirstByPracticeIdAndQuestionIdOrderByIdAsc(Long practiceId, Long questionId);
}
