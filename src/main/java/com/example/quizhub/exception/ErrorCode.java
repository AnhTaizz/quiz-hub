package com.example.quizhub.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public enum ErrorCode {
    USER_EXISTED(HttpStatus.CONFLICT, "Email này đã được sử dụng!"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản!"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Email hoặc mật khẩu không chính xác!"),
    UNCATEGORIZED_EXCEPTION(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống không xác định!"),

    BLANK_FIELD(HttpStatus.BAD_REQUEST, "Dữ liệu không được để trống!"),
    INVALID_EMAIL(HttpStatus.BAD_REQUEST, "Định dạng email không hợp lệ!"),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "Mật khẩu phải có ít nhất 6 ký tự!");

    private final HttpStatus statusCode;
    private final String message;

    ErrorCode(HttpStatus statusCode, String message) {
        this.statusCode = statusCode;
        this.message = message;
    }
}
