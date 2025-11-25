package org.example.expenseapi.controller;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        Throwable cause = ex.getCause();

        // Jackson throws InvalidFormatException when a value can't be deserialized to the target type
        if (cause instanceof InvalidFormatException) {
            InvalidFormatException ife = (InvalidFormatException) cause;
            Class<?> targetType = ife.getTargetType();

            // Check for BigDecimal target (amount field)
            if (BigDecimal.class.equals(targetType)) {
                Map<String, String> body = new HashMap<>();
                // try to determine field name from path
                String fieldName = "amount";
                if (!ife.getPath().isEmpty() && ife.getPath().get(0) != null && ife.getPath().get(0).getFieldName() != null) {
                    fieldName = ife.getPath().get(0).getFieldName();
                }
                body.put("error", String.format("%s must be a numeric value", fieldName));
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
            }

            // generic invalid format
            Map<String, String> body = new HashMap<>();
            body.put("error", "Invalid value type in request body");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
        }

        Map<String, String> body = new HashMap<>();
        body.put("error", "Malformed JSON request");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }
}

