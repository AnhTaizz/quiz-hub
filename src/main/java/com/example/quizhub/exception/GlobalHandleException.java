package com.example.quizhub.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalHandleException {
    @ExceptionHandler(value = RuntimeException.class)
    public ResponseEntity<String> handlingRuntimeException(RuntimeException exception) {
        // Trả về mã 400 và cái câu thông báo lỗi
        return ResponseEntity.badRequest().body(exception.getMessage());
    }

    // 2. Hứng lỗi do Validation thất bại (Cái vụ password < 8 ký tự lúc nãy)
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public ResponseEntity<String> handlingValidation(MethodArgumentNotValidException exception) {
        // Lôi cái câu message "Password must be..." mà mình đã cấu hình ở DTO ra
        String errorMessage = exception.getFieldError().getDefaultMessage();
        return ResponseEntity.badRequest().body(errorMessage);
    }
}
