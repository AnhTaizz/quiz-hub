package com.example.quizhub.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import com.example.quizhub.exception.AppException;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;

@ControllerAdvice
public class DebugExceptionAdvice {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<?> handleAppException(AppException e) {
        return ResponseEntity
                .status(e.getErrorCode().getStatusCode())
                .body(java.util.Map.of(
                        "code", e.getErrorCode().getCode(),
                        "message", e.getErrorCode().getMessage()));
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ResponseEntity<String> handleException(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        String stackTrace = sw.toString();

        try {
            FileWriter fw = new FileWriter("error_log.txt", false);
            fw.write(stackTrace);
            fw.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("<pre>" + stackTrace + "</pre>");
    }
}
