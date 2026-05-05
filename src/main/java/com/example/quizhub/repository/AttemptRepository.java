package com.example.quizhub.repository;

import com.example.quizhub.entity.Attempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AttemptRepository extends JpaRepository<Attempt, Long> {

    List<Attempt> findByQuizTakingId(Long quizTakingId);
    int countByQuizTakingId(Long quizTakingId);

    long countByResultLessThan(BigDecimal score);
    long countByResultBetween(BigDecimal min, BigDecimal max);
    long countByResultGreaterThanEqual(BigDecimal score);

    List<Attempt> findTop10ByOrderByStartedAtDesc();

    long countByQuizTakingLearnerId(Long learnerId);
    List<Attempt> findByQuizTakingLearnerId(Long learnerId);

    @Query("SELECT a FROM Attempt a WHERE a.endedAt IS NULL AND a.quizTaking.quizAssigning.dueDate < :now")
    List<Attempt> findExpiredAttempts(@Param("now") LocalDateTime now);

    long countByQuizTakingIdAndEndedAtIsNotNull(Long quizTakingId);
}
