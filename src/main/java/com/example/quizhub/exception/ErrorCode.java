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
    QUIZ_NOT_FOUND(1006, "Quiz not found", HttpStatus.NOT_FOUND);

    final int code;
    final String message;
    final HttpStatusCode statusCode;
}
