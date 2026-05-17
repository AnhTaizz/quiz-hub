package com.example.quizhub.service;

import java.util.List;
import java.util.Map;

import org.springframework.web.multipart.MultipartFile;

import com.example.quizhub.dto.question.QuestionRequestDTO;

public interface QuestionImportService {
    Map<String, Object> importQuestionsFromExcel(MultipartFile file, Long categoryId, Long teacherId);

    List<QuestionRequestDTO> parseExcelOnly(MultipartFile file);
}
