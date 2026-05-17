package com.example.quizhub.service.impl;

import com.example.quizhub.dto.question.AnswerCreationRequestDTO;
import com.example.quizhub.dto.question.QuestionRequestDTO;
import com.example.quizhub.entity.enums.QuestionLevel;
import com.example.quizhub.entity.enums.QuestionType;
import com.example.quizhub.exception.AppException;
import com.example.quizhub.exception.ErrorCode;
import com.example.quizhub.service.QuestionImportService;
import com.example.quizhub.service.QuestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;

import com.example.quizhub.dto.question.QuestionResponseDTO;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionImportServiceImpl implements QuestionImportService {

    private final QuestionService questionService;

    @Override
    public List<QuestionRequestDTO> parseExcelOnly(MultipartFile file) {
        List<QuestionRequestDTO> list = new ArrayList<>();
        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) continue;
                try {
                    QuestionRequestDTO dto = mapRowToDTO(row, formatter);
                    list.add(dto);
                } catch (Exception e) {
                    log.warn("Bỏ qua dòng {} trong file review do lỗi: {}", i + 1, e.getMessage());
                }
            }
        } catch (Exception e) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
        return list;
    }

    private QuestionRequestDTO mapRowToDTO(Row row, DataFormatter formatter) {
        // Column 0: Nội dung câu hỏi
        String text = getCellValue(row, 0, formatter).trim();
        if (text.isEmpty()) {
            throw new AppException(ErrorCode.QUESTION_TEXT_EMPTY);
        }

        // Column 1: Loại câu hỏi
        String typeRaw = getCellValue(row, 1, formatter).trim().toLowerCase();
        QuestionType type = mapType(typeRaw);

        // Column 2: Mức độ
        String levelRaw = getCellValue(row, 2, formatter).trim().toLowerCase();
        QuestionLevel level = mapLevel(levelRaw);

        // Columns 3 -> 10: Đáp án A -> H
        List<String> rawAnswers = new ArrayList<>();
        for (int col = 3; col <= 10; col++) {
            String val = getCellValue(row, col, formatter).trim();
            if (!val.isEmpty()) {
                rawAnswers.add(val);
            }
        }

        if (rawAnswers.isEmpty()) {
            throw new AppException(ErrorCode.QUESTION_ANSWERS_EMPTY);
        }

        // Column 11: Đáp án đúng
        String correctStr = getCellValue(row, 11, formatter).trim().toUpperCase();
        Set<Integer> correctIndices = parseCorrectIndices(correctStr, type, rawAnswers.size());

        // Build AnswerDTOs
        List<AnswerCreationRequestDTO> answerDTOs = new ArrayList<>();
        for (int j = 0; j < rawAnswers.size(); j++) {
            boolean isCorrect = (type == QuestionType.FILL_IN_BLANK) || correctIndices.contains(j);
            answerDTOs.add(AnswerCreationRequestDTO.builder()
                    .text(rawAnswers.get(j))
                    .correct(isCorrect)
                    .build());
        }

        // Return DTO shell
        return QuestionRequestDTO.builder()
                .text(text)
                .type(type)
                .level(level)
                .answers(answerDTOs)
                .build();
    }

    @Override
    @Transactional
    public Map<String, Object> importQuestionsFromExcel(MultipartFile file, Long categoryId, Long teacherId) {
        int successCount = 0;
        int errorCount = 0;
        List<String> errors = new ArrayList<>();
        List<QuestionResponseDTO> importedList = new ArrayList<>();

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();

            for (int i = 1; i <= sheet.getLastRowNum(); i++) { // Bỏ qua row 0 (tiêu đề)
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) continue;

                try {
                    QuestionRequestDTO dto = mapRowToDTO(row, formatter);
                    dto.setCategoryId(categoryId); // Override with supplied destination folder

                    // Persist via existing QuestionService
                    QuestionResponseDTO saved = questionService.createNewQuestion(teacherId, dto);
                    successCount++;
                    importedList.add(saved);

                } catch (Exception ex) {
                    errorCount++;
                    log.error("Lỗi khi đọc dòng {}: {}", i + 1, ex.getMessage());
                    errors.add("Dòng " + (i + 1) + ": " + ex.getMessage());
                }
            }

        } catch (Exception e) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("successCount", successCount);
        response.put("errorCount", errorCount);
        response.put("errors", errors);
        response.put("importedQuestions", importedList);
        return response;
    }

    private boolean isRowEmpty(Row row) {
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK) return false;
        }
        return true;
    }

    private String getCellValue(Row row, int cellIndex, DataFormatter formatter) {
        Cell cell = row.getCell(cellIndex);
        if (cell == null) return "";
        return formatter.formatCellValue(cell);
    }

    private QuestionType mapType(String raw) {
        if (raw.contains("nhiều") || raw.contains("multiple")) return QuestionType.MULTIPLE_CHOICE;
        if (raw.contains("khuyết") || raw.contains("fill")) return QuestionType.FILL_IN_BLANK;
        return QuestionType.SINGLE_CHOICE; // Mặc định là trắc nghiệm 1
    }

    private QuestionLevel mapLevel(String raw) {
        if (raw.contains("dễ") || raw.contains("easy")) return QuestionLevel.EASY;
        if (raw.contains("khó") || raw.contains("hard")) return QuestionLevel.HARD;
        return QuestionLevel.MEDIUM; // Mặc định là Trung bình
    }

    private Set<Integer> parseCorrectIndices(String raw, QuestionType type, int answerCount) {
        Set<Integer> result = new HashSet<>();
        if (type == QuestionType.FILL_IN_BLANK) return result; // Xử lý riêng sau

        String clean = raw.replaceAll("[^A-H,;]", ""); // Lọc các ký tự A-H và dấu phẩy
        String[] tokens = clean.split("[,;]");

        for (String t : tokens) {
            t = t.trim();
            if (t.isEmpty()) continue;
            char c = t.charAt(0);
            int index = c - 'A';
            if (index >= 0 && index < answerCount) {
                result.add(index);
            }
        }
        return result;
    }
}
