package com.example.quizhub.service.classroom.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

import org.springframework.stereotype.Service;

import com.example.quizhub.dto.classroom.request.ClassroomRequestDTO;
import com.example.quizhub.dto.classroom.response.ClassroomResponseDTO;
import com.example.quizhub.dto.classroom.response.MemberResponseDTO;
import com.example.quizhub.entity.ClassJoining;
import com.example.quizhub.entity.Classroom;
import com.example.quizhub.entity.User;
import com.example.quizhub.exception.AppException;
import com.example.quizhub.exception.ErrorCode;
import com.example.quizhub.repository.ClassJoiningRepository;
import com.example.quizhub.repository.ClassroomRepository;
import com.example.quizhub.repository.UserRepository;
import com.example.quizhub.service.classroom.ClassroomService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClassroomServiceImpl implements ClassroomService {

    private final ClassroomRepository classroomRepository;
    private final UserRepository userRepository;
    private final ClassJoiningRepository classJoiningRepository;

    @Override
    public ClassroomResponseDTO createClassroom(String teacherEmail, ClassroomRequestDTO request) {
        User teacher = userRepository.findByEmail(teacherEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        String joinCode = generateUniqueCode();

        Classroom classroom = Classroom.builder()
                .name(request.getName())
                .description(request.getDescription())
                .code(joinCode)
                .isEnable(true)
                .isDraft(false)
                .creator(teacher)
                .build();

        classroomRepository.save(classroom);

        return ClassroomResponseDTO.builder()
                .id(classroom.getId())
                .name(classroom.getName())
                .description(classroom.getDescription())
                .code(classroom.getCode())
                .teacherName(teacher.getFullName())
                .createdAt(classroom.getCreatedAt())
                .build();
    }

    private String generateUniqueCode() {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        Random rnd = new Random();

        while (true) {
            code.setLength(0);
            for (int i = 0; i < 6; i++) {
                code.append(characters.charAt(rnd.nextInt(characters.length())));
            }
            if (!classroomRepository.existsByCode(code.toString())) {
                return code.toString();
            }
        }
    }

    @Override
    public List<MemberResponseDTO> getMembersInClass(Long classroomId, String teacherEmail) {
        User teacher = userRepository.findByEmail(teacherEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Classroom classroom = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new AppException(ErrorCode.CLASSROOM_NOT_FOUND));

        if (!classroom.getCreator().getId().equals(teacher.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        var joinings = classJoiningRepository.findByClassroomId(classroomId);

        return joinings.stream().map(join -> MemberResponseDTO.builder()
                .studentId(join.getLearner().getId())
                .fullName(join.getDisplayedName()) // Lấy tên hiển thị trong lớp
                .email(join.getLearner().getEmail())
                .phone(join.getDisplayedPhone())
                .joinedAt(join.getJoinedAt())
                .build()).toList();
    }

    @Override
    public void removeStudentFromClass(Long classroomId, Long studentId, String teacherEmail) {
        User teacher = userRepository.findByEmail(teacherEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Classroom classroom = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new AppException(ErrorCode.CLASSROOM_NOT_FOUND));

        if (!classroom.getCreator().getId().equals(teacher.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        ClassJoining joiningRecord = classJoiningRepository
                .findByClassroomIdAndLearnerId(classroomId, studentId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_IN_CLASS));

        classJoiningRepository.delete(joiningRecord);
    }

    @Override
    public void joinClass(String studentEmail, String classCode) {
        User learner = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Classroom classroom = classroomRepository.findByCode(classCode)
                .orElseThrow(() -> new AppException(ErrorCode.CLASSROOM_NOT_FOUND));

        boolean isAlreadyJoined = classJoiningRepository
                .findByClassroomIdAndLearnerId(classroom.getId(), learner.getId())
                .isPresent();
        if (isAlreadyJoined) {
            throw new AppException(ErrorCode.USER_ALREADY_IN_CLASS);
        }

        ClassJoining classJoining = ClassJoining.builder()
                .classroom(classroom)
                .learner(learner)
                .displayedName(learner.getFullName())
                .displayedPhone(learner.getPhone())
                .joinedAt(LocalDateTime.now())
                .build();

        classJoiningRepository.save(classJoining);
    }
}
