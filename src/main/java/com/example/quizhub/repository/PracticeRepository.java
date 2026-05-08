package com.example.quizhub.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.quizhub.entity.Practice;

@Repository
public interface PracticeRepository extends JpaRepository<Practice, Long> {
    List<Practice> findByUserIdAndCategoryIdOrderByCreatedAtDesc(Long userId, Long categoryId);
    List<Practice> findTop10ByOrderByCreatedAtDesc();
    long countByUserId(Long userId);
    long countByUserIdAndIsCompletedTrue(Long userId);
    List<Practice> findByUserId(Long userId);
    List<Practice> findByUserIdOrderByCreatedAtDesc(Long userId);
    java.util.Optional<Practice> findFirstByUserIdAndCategoryIdAndPracticeLimitAndPracticeOffsetAndIsCompletedFalseOrderByCreatedAtDesc(
            Long userId, Long categoryId, Integer practiceLimit, Integer practiceOffset);
    List<Practice> findByUserIdAndCategoryIdAndIsCompletedTrueOrderByCreatedAtDesc(Long userId, Long categoryId);
    List<Practice> findByUserIdAndIsCompletedTrueOrderByCreatedAtDesc(Long userId);
}

