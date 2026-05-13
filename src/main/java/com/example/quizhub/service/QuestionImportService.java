package com.example.quizhub.service;

import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;

public interface QuestionImportService {
    Map<String, Object> importQuestionsFromExcel(MultipartFile file, Long categoryId, Long teacherId);
    
    List<com.example.quizhub.dto.question.QuestionRequestDTO> parseExcelOnly(MultipartFile file);
}
