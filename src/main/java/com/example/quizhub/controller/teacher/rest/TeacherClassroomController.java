package com.example.quizhub.controller.teacher.rest;

import java.security.Principal;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.quizhub.dto.classroom.request.ClassroomRequestDTO;
import com.example.quizhub.dto.classroom.response.ClassroomResponseDTO;
import com.example.quizhub.dto.classroom.response.MemberResponseDTO;
import com.example.quizhub.service.classroom.ClassroomService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/teacher/classrooms")
@RequiredArgsConstructor
public class TeacherClassroomController {

    private final ClassroomService classroomService;

    @PostMapping
    public ResponseEntity<ClassroomResponseDTO> createClassroom(
            Principal principal,
            @RequestBody @Valid ClassroomRequestDTO request) {
        return ResponseEntity.ok(classroomService.createClassroom(principal.getName(), request));
    }

    @GetMapping("/{classroomId}/students")
    public ResponseEntity<List<MemberResponseDTO>> getMembersInClass(
            Principal principal,
            @PathVariable Long classroomId) {

        return ResponseEntity.ok(classroomService.getMembersInClass(classroomId, principal.getName()));
    }

    @DeleteMapping("/{classroomId}/students/{studentId}")
    public ResponseEntity<String> removeStudent(
            Principal principal,
            @PathVariable Long classroomId,
            @PathVariable Long studentId) {

        classroomService.removeStudentFromClass(classroomId, studentId, principal.getName());
        return ResponseEntity.ok("Đã kích học sinh khỏi lớp!");
    }
}
