package com.example.quizhub.service.classroom.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.quizhub.dto.classroom.request.ClassTopicRequestDTO;
import com.example.quizhub.dto.classroom.response.ClassTopicResponseDTO;
import com.example.quizhub.entity.ClassTopic;
import com.example.quizhub.entity.Classroom;
import com.example.quizhub.exception.AppException;
import com.example.quizhub.exception.ErrorCode;
import com.example.quizhub.repository.ClassTopicRepository;
import com.example.quizhub.repository.ClassroomRepository;
import com.example.quizhub.service.classroom.ClassTopicService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClassTopicServiceImpl implements ClassTopicService {

    private final ClassTopicRepository classTopicRepository;
    private final ClassroomRepository classroomRepository;

    @Override
    @Transactional
    public ClassTopicResponseDTO createTopic(ClassTopicRequestDTO request, String userEmail) {
        Classroom classroom = classroomRepository.findById(request.getClassroomId())
                .orElseThrow(() -> new AppException(ErrorCode.CLASSROOM_NOT_FOUND));

        if (!classroom.getCreator().getEmail().equals(userEmail)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        ClassTopic topic = new ClassTopic();
        topic.setName(request.getName());
        topic.setClassroom(classroom);

        return ClassTopicResponseDTO.fromEntity(classTopicRepository.save(topic));
    }

    @Override
    @Transactional
    public ClassTopicResponseDTO updateTopic(Long id, ClassTopicRequestDTO request, String userEmail) {
        ClassTopic topic = classTopicRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CLASS_TOPIC_NOT_FOUND));

        if (!topic.getClassroom().getCreator().getEmail().equals(userEmail)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        topic.setName(request.getName());
        return ClassTopicResponseDTO.fromEntity(classTopicRepository.save(topic));
    }

    @Override
    @Transactional
    public void deleteTopic(Long id, String userEmail) {
        ClassTopic topic = classTopicRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CLASS_TOPIC_NOT_FOUND));

        if (!topic.getClassroom().getCreator().getEmail().equals(userEmail)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        classTopicRepository.deleteById(id);
    }

    @Override
    public List<ClassTopicResponseDTO> getTopicsByClassroom(Long classroomId, String userEmail) {
        Classroom classroom = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new AppException(ErrorCode.CLASSROOM_NOT_FOUND));

        return classTopicRepository.findByClassroomId(classroomId).stream()
                .map(ClassTopicResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }
}
