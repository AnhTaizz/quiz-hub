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

    BLANK_FIELD(1007, "Field cannot be blank", HttpStatus.BAD_REQUEST),
    INVALID_EMAIL(1008, "Invalid email format", HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD(1009, "Password must be at least 6 characters", HttpStatus.BAD_REQUEST),
    PASSWORD_MISMATCH(1010, "Password confirmation does not match", HttpStatus.BAD_REQUEST),
    WRONG_PASSWORD(1011, "Current password is incorrect", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(1012, "Unauthorized", HttpStatus.UNAUTHORIZED),
    USER_EXISTED(1013, "Email already exists", HttpStatus.BAD_REQUEST),
    QUESTION_INVALID_LOGIC(1014, "Question invalid logic", HttpStatus.BAD_REQUEST),
    CATEGORY_HAS_QUESTIONS(1015, "Category has questions", HttpStatus.BAD_REQUEST),
    CATEGORY_INVALID_LOGIC(1016, "Category invalid logic", HttpStatus.BAD_REQUEST),
    CLASSROOM_NOT_FOUND(1017, "Classroom not found", HttpStatus.NOT_FOUND),
    QUIZ_ASSIGNING_NOT_FOUND(1018, "Quiz assigning not found", HttpStatus.NOT_FOUND),
    ATTEMPT_NOT_FOUND(1019, "Attempt not found", HttpStatus.NOT_FOUND),
    ATTEMPT_ALREADY_SUBMITTED(1020, "Attempt already submitted", HttpStatus.BAD_REQUEST),
    INVALID_OTP(1021, "Invalid OTP", HttpStatus.BAD_REQUEST),
    PASSWORD_SAME(1022, "New password cannot be the same as the old password", HttpStatus.BAD_REQUEST),
    QUESTION_ALREADY_PUBLIC(1023, "Question already public", HttpStatus.BAD_REQUEST);

    final int code;
    final String message;
    final HttpStatusCode statusCode;

}
