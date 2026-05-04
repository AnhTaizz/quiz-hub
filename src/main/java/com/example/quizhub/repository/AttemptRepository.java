package com.example.quizhub.repository;

import com.example.quizhub.entity.Attempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttemptRepository extends JpaRepository<Attempt, Long> {

    List<Attempt> findByQuizTakingId(Long quizTakingId);

    long countByResultLessThan(java.math.BigDecimal score);
    long countByResultBetween(java.math.BigDecimal min, java.math.BigDecimal max);
    long countByResultGreaterThanEqual(java.math.BigDecimal score);

    java.util.List<Attempt> findTop10ByOrderByStartedAtDesc();

    long countByQuizTakingLearnerId(Long learnerId);
    java.util.List<Attempt> findByQuizTakingLearnerId(Long learnerId);
    long countByQuizTakingIdAndEndedAtIsNotNull(Long quizTakingId);
}
