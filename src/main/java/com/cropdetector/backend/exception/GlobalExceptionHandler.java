
package com.cropdetector.backend.exception;

import java.time.Instant;

import com.cropdetector.backend.dto.PredictionDtos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log =
        LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<PredictionDtos.ErrorResponse> handleApi(
        ApiException ex
    ) {
        return response(ex.getStatus(), ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<PredictionDtos.ErrorResponse> handleValidation(
        MethodArgumentNotValidException ex
    ) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .orElse("Request validation failed.");

        return response(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }

    @ExceptionHandler({
        HttpMessageNotReadableException.class,
        MissingServletRequestPartException.class,
        MethodArgumentTypeMismatchException.class,
        MultipartException.class
    })
    public ResponseEntity<PredictionDtos.ErrorResponse> handleBadRequest(
        Exception ex
    ) {
        return response(HttpStatus.BAD_REQUEST, "BAD_REQUEST",
            "Request body, parameter, or required file is invalid.");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<PredictionDtos.ErrorResponse> handleLargeUpload(
        MaxUploadSizeExceededException ex
    ) {
        return response(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE",
            "Uploaded file exceeds the allowed size.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<PredictionDtos.ErrorResponse> handleUnexpected(
        Exception ex
    ) {
        log.error("Unhandled API error", ex);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
            "An unexpected server error occurred.");
    }

    private ResponseEntity<PredictionDtos.ErrorResponse> response(
        HttpStatus status, String code, String message
    ) {
        return ResponseEntity.status(status).body(
            new PredictionDtos.ErrorResponse(code, message, Instant.now()));
    }
}
