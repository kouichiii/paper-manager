package com.example.demo.web;

import java.time.Instant;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice(basePackages = "com.example.demo.web")
public class ApiExceptionHandler {

    // エラー共通フォーマット
    public record ErrorResponse(
            String code,            // "BAD_REQUEST" など
            String message,         // 人間向け説明
            List<FieldError> details, // フィールド単位のエラー詳細（任意）
            String path,            // リクエストパス
            Instant timestamp       // 発生時刻
    ) {}

    public record FieldError(String field, String error) {}

    // 1) バリデーション失敗（@Valid）
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse onValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        var details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return new ErrorResponse(
                "BAD_REQUEST",
                "Validation failed",
                details,
                req.getRequestURI(),
                Instant.now()
        );
    }

    // 2) JSONが壊れている / 型不一致
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse onBadJson(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return new ErrorResponse(
                "BAD_REQUEST",
                "Malformed JSON or type mismatch",
                List.of(),
                req.getRequestURI(),
                Instant.now()
        );
    }

    // 3) 明示的に投げた 404/400 など（ResponseStatusException）
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> onRSE(ResponseStatusException ex, HttpServletRequest req) {
        var body = new ErrorResponse(
                ex.getStatusCode().toString().replace(" ", "_"),
                ex.getReason() != null ? ex.getReason() : "Error",
                List.of(),
                req.getRequestURI(),
                Instant.now()
        );
        return ResponseEntity.status(ex.getStatusCode()).body(body);
    }

    // 4) フレームワーク由来のErrorResponseException（Spring 6の一部）
    @ExceptionHandler(ErrorResponseException.class)
    public ResponseEntity<ErrorResponse> onERE(ErrorResponseException ex, HttpServletRequest req) {
        var body = new ErrorResponse(
                ex.getStatusCode().toString().replace(" ", "_"),
                ex.getBody() != null && ex.getBody().getDetail() != null
                        ? ex.getBody().getDetail() : "Error",
                List.of(),
                req.getRequestURI(),
                Instant.now()
        );
        return ResponseEntity.status(ex.getStatusCode()).body(body);
    }
    
    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(ApiExceptionHandler.class);
    
    // 5) 想定外（最後の砦）
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse onUnknown(Exception ex, HttpServletRequest req) {
    	log.error("Unhandled error at {}: {}", req.getRequestURI(), ex.toString(), ex);
        return new ErrorResponse(
                "INTERNAL_SERVER_ERROR",
                "Unexpected error",
                List.of(),
                req.getRequestURI(),
                Instant.now()
        );
    }
}
