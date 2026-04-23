package com.example.quizhub.repository;

import com.example.quizhub.entity.ClassJoining;
import com.example.quizhub.entity.JoinStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassJoiningRepository extends JpaRepository<ClassJoining, Long> {

    List<ClassJoining> findByClassroomId(Long classroomId);

    List<ClassJoining> findByClassroomIdAndStatus(Long classroomId, JoinStatus status);

    List<ClassJoining> findByLearnerId(Long learnerId);

    Optional<ClassJoining> findByClassroomIdAndLearnerId(Long classroomId, Long learnerId);
}
