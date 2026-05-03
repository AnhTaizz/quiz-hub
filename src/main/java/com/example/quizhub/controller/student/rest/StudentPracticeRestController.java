package com.example.quizhub.controller.student.rest;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.quizhub.dto.practice.PracticeQuestionResponseDTO;
import com.example.quizhub.dto.practice.PracticeResultResponseDTO;
import com.example.quizhub.dto.practice.PracticeStartRequestDTO;
import com.example.quizhub.dto.practice.PracticeSubmitRequestDTO;
import com.example.quizhub.service.practice.PracticeService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/student/practice")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
public class StudentPracticeRestController {

    private final PracticeService practiceService;

    @PostMapping("/start")
    public ResponseEntity<List<PracticeQuestionResponseDTO>> startPractice(
            @RequestBody @Valid PracticeStartRequestDTO request) {
        return ResponseEntity.ok(practiceService.startPractice(request));
    }

    @PostMapping("/preview")
    public ResponseEntity<List<PracticeQuestionResponseDTO>> previewPractice(
            @RequestBody @Valid PracticeStartRequestDTO request) {
        return ResponseEntity.ok(practiceService.previewPractice(request));
    }

    @PostMapping("/submit")
    public ResponseEntity<PracticeResultResponseDTO> submitPractice(
            @RequestBody @Valid PracticeSubmitRequestDTO request) {
        return ResponseEntity.ok(practiceService.submitPractice(request));
    }

    @GetMapping("/count")
    public ResponseEntity<Long> countQuestions(@RequestParam("categoryId") Long categoryId) {
        return ResponseEntity.ok(practiceService.countQuestions(categoryId));
    }

    @GetMapping("/history")
    public ResponseEntity<List<com.example.quizhub.dto.practice.PracticeHistoryResponseDTO>> getPracticeHistory(
            @RequestParam("categoryId") Long categoryId) {
        return ResponseEntity.ok(practiceService.getPracticeHistory(categoryId));
    }

    @GetMapping("/history/detail")
    public ResponseEntity<PracticeResultResponseDTO> getPracticeDetail(@RequestParam("id") Long id) {
        return ResponseEntity.ok(practiceService.getPracticeDetail(id));
    }
}
