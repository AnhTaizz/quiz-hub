package com.example.quizhub.repository;

import com.example.quizhub.entity.UserAttemptAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserAttemptAnswerRepository extends JpaRepository<UserAttemptAnswer, Long> {

    List<UserAttemptAnswer> findByAttemptId(Long attemptId);

    List<UserAttemptAnswer> findByQuestionId(Long questionId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    void deleteByAttemptIdAndQuestionId(Long attemptId, Long questionId);
}
