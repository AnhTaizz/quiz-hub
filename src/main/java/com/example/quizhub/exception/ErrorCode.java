package com.example.quizhub.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(1001, "Invalid key", HttpStatus.BAD_REQUEST),
    USER_NOT_FOUND(1002, "User not found", HttpStatus.NOT_FOUND),
    CATEGORY_NOT_FOUND(1003, "Category not found", HttpStatus.NOT_FOUND),
    QUESTION_NOT_FOUND(1004, "Question not found", HttpStatus.NOT_FOUND),
    ANSWER_NOT_FOUND(1005, "Answer not found", HttpStatus.NOT_FOUND),
    QUIZ_NOT_FOUND(1006, "Quiz not found", HttpStatus.NOT_FOUND),

    BLANK_FIELD(1007, "Trường này không được để trống", HttpStatus.BAD_REQUEST),
    INVALID_EMAIL(1008, "Định dạng email không hợp lệ", HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD(1009, "Mật khẩu phải có ít nhất 6 ký tự", HttpStatus.BAD_REQUEST),
    PASSWORD_MISMATCH(1010, "Mật khẩu xác nhận không khớp", HttpStatus.BAD_REQUEST),
    WRONG_PASSWORD(1011, "Mật khẩu hiện tại không chính xác", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(1012, "Không có quyền truy cập", HttpStatus.UNAUTHORIZED),
    USER_EXISTED(1013, "Email đã tồn tại", HttpStatus.BAD_REQUEST),
    QUESTION_INVALID_LOGIC(1014, "Question invalid logic", HttpStatus.BAD_REQUEST),
    CATEGORY_HAS_QUESTIONS(1015, "Category has questions", HttpStatus.BAD_REQUEST),
    CATEGORY_INVALID_LOGIC(1016, "Category invalid logic", HttpStatus.BAD_REQUEST),
    CLASSROOM_NOT_FOUND(1017, "Classroom not found", HttpStatus.NOT_FOUND),
    QUIZ_ASSIGNING_NOT_FOUND(1018, "Quiz assigning not found", HttpStatus.NOT_FOUND),
    ATTEMPT_NOT_FOUND(1019, "Attempt not found", HttpStatus.NOT_FOUND),
    ATTEMPT_ALREADY_SUBMITTED(1020, "Attempt already submitted", HttpStatus.BAD_REQUEST),
    INVALID_OTP(1021, "Mã OTP không hợp lệ", HttpStatus.BAD_REQUEST),
    PASSWORD_SAME(1022, "Mật khẩu mới không được trùng với mật khẩu cũ", HttpStatus.BAD_REQUEST),
    USER_NOT_IN_CLASS(1023, "User is not a member of this class", HttpStatus.BAD_REQUEST),
    USER_ALREADY_IN_CLASS(1024, "User is already a member of this class", HttpStatus.BAD_REQUEST),
    QUESTION_ALREADY_PUBLIC(1025, "Question already public", HttpStatus.BAD_REQUEST),
    CLASS_TOPIC_NOT_FOUND(1026, "Class topic not found", HttpStatus.NOT_FOUND),
    PRACTICE_NOT_FOUND(1027, "Practice not found", HttpStatus.NOT_FOUND),
    MAX_ATTEMPTS_REACHED(1028, "You have reached the maximum number of attempts for this quiz", HttpStatus.BAD_REQUEST),
    QUIZ_EXPIRED(1029, "The quiz has already ended", HttpStatus.BAD_REQUEST),
    QUIZ_NOT_STARTED(1030, "The quiz has not started yet", HttpStatus.BAD_REQUEST),
    INVALID_DATE_RANGE(1031, "Due date must be after start date", HttpStatus.BAD_REQUEST),
    INVALID_DURATION(1032, "Duration must be greater than 0 and not exceed the time range", HttpStatus.BAD_REQUEST),
    PRACTICE_ALREADY_SUBMITTED(1033, "Practice already submitted", HttpStatus.BAD_REQUEST),
    EXCEL_IMPORT_ERROR(1034, "Error importing Excel file", HttpStatus.BAD_REQUEST),
    INVALID_GENERATION_METHOD(1035, "Invalid method. Must be RANDOM or RANGE", HttpStatus.BAD_REQUEST),
    QUESTION_TEXT_EMPTY(1036, "Question text cannot be empty", HttpStatus.BAD_REQUEST),
    QUESTION_ANSWERS_EMPTY(1037, "Question must have at least one answer option", HttpStatus.BAD_REQUEST);

    final int code;
    final String message;
    final HttpStatusCode statusCode;

}
