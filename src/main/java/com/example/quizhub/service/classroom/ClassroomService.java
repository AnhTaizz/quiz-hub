package com.example.quizhub.service.classroom;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.quizhub.dto.classroom.request.ClassroomRequestDTO;
import com.example.quizhub.dto.classroom.response.ClassroomResponseDTO;
import com.example.quizhub.dto.classroom.response.MemberResponseDTO;

@Service
public interface ClassroomService {
    ClassroomResponseDTO createClassroom(String teacherEmail, ClassroomRequestDTO request);

    List<MemberResponseDTO> getMembersInClass(Long classroomId, String teacherEmail);

    void removeStudentFromClass(Long classroomId, Long studentId, String teacherEmail);

    void joinClass(String studentEmail, String classCode);
}
