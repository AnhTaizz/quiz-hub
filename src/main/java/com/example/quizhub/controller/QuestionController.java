package com.example.quizhub.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.quizhub.dto.request.QuestionCreationRequestDTO;
import com.example.quizhub.dto.response.QuestionResponseDTO;
import com.example.quizhub.mapper.QuestionMapper;
import com.example.quizhub.service.QuestionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;
    private final QuestionMapper questionMapper;

    @PostMapping
    public ResponseEntity<?> createQuestion(@RequestBody QuestionCreationRequestDTO request) {
        try {
            Long mockedId = 2L;
            QuestionResponseDTO response = questionService.createNewQuestion(mockedId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
