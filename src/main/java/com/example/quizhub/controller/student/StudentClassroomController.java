package com.example.quizhub.controller.student;

import com.example.quizhub.service.classroom.ClassroomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/student/classrooms")
@RequiredArgsConstructor
public class StudentClassroomController {

    private final ClassroomService classroomService;

    @PostMapping("/join")
    public ResponseEntity<String> joinClass(Principal principal, @RequestParam String code) {
        classroomService.joinClass(principal.getName(), code);
        return ResponseEntity.ok("Đã tham gia lớp học thành công!");
    }
}