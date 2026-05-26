package com.example.sleep.recordings;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class RecordingExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<RecordingErrorResponse> badRequest(IllegalArgumentException exception) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new RecordingErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<RecordingErrorResponse> conflict(IllegalStateException exception) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new RecordingErrorResponse(exception.getMessage()));
    }
}
