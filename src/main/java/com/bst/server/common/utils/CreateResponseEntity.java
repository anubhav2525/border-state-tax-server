package com.bst.server.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.WebRequest;

import java.time.OffsetDateTime;

@Slf4j
@Component
public class CreateResponseEntity {

    // -------------------------------------------------------------------------
    // Core converter
    // -------------------------------------------------------------------------

    public <T> ResponseEntity<CustomResponse<T>> toResponseEntity(CustomResponse<T> response) {
        return ResponseEntity
                .status(response.getStatus())
                .body(response);
    }

    public String getApiPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }

    // =========================================================================
    // Exception error responses
    // =========================================================================

    // Simple error — no data payload (most common exception case)
    public ResponseEntity<CustomResponse<Void>> buildExceptionResponse(
            String message, String handler, WebRequest request, HttpStatus httpStatus) {

        log.warn("{} exception: {}", handler, message);
        return buildException(message, null, null, handler, request, httpStatus);
    }

    // Error with error-detail payload — e.g. validation field errors
    public <T> ResponseEntity<CustomResponse<T>> buildExceptionResponse(
            String message, T errors, String handler, WebRequest request, HttpStatus httpStatus) {

        log.warn("{} exception: {} | errors: {}", handler, message, errors);
        return buildException(message, null, errors, handler, request, httpStatus);
    }

    // Full control — both data and errors (e.g. partial bulk failure)
    public <T> ResponseEntity<CustomResponse<T>> buildExceptionResponse(
            String message, T data, T errors, String handler, WebRequest request, HttpStatus httpStatus) {

        log.warn("{} exception: {} | data: {} | errors: {}", handler, message, data, errors);
        return buildException(message, data, errors, handler, request, httpStatus);
    }

    // Single private builder — all exception overloads converge here
    private <T> ResponseEntity<CustomResponse<T>> buildException(
            String message, T data, T errors, String handler, WebRequest request, HttpStatus httpStatus) {

        CustomResponse<T> response = CustomResponse.<T>builder()
                .message(message)
                .data(data)
                .errors(errors)
                .status(httpStatus)
                .timestamp(OffsetDateTime.now())
                .apiPath(getApiPath(request))
                .build();

        return ResponseEntity.status(response.getStatus()).body(response);
    }

    // =========================================================================
    // Normal API responses
    // =========================================================================

    // Success with data — most common (GET, POST, PUT)
    public <T> ResponseEntity<CustomResponse<T>> buildResponse(
            String message, T data, HttpStatus status, WebRequest request) {

        log.info("Response [{}] {} - {}", status.value(), getApiPath(request), message);
        return buildSuccess(message, data, null, status, request);
    }

    // No data body — DELETE, logout, or any 204-style response
    public ResponseEntity<CustomResponse<Void>> buildResponse(
            String message, HttpStatus status, WebRequest request) {

        log.info("Response [{}] {} - {}", status.value(), getApiPath(request), message);
        return buildSuccess(message, null, null, status, request);
    }

    // Success with both data and errors — bulk operations with partial failures
    public <T> ResponseEntity<CustomResponse<T>> buildResponse(
            String message, T data, T errors, HttpStatus status, WebRequest request) {

        log.info("Response [{}] {} - {} | partial errors present", status.value(), getApiPath(request), message);
        return buildSuccess(message, data, errors, status, request);
    }

    // Single private builder — all normal response overloads converge here
    private <T> ResponseEntity<CustomResponse<T>> buildSuccess(
            String message, T data, T errors, HttpStatus status, WebRequest request) {

        CustomResponse<T> response = CustomResponse.<T>builder()
                .message(message)
                .data(data)
                .errors(errors)
                .status(status)
                .timestamp(OffsetDateTime.now())
                .apiPath(getApiPath(request))
                .build();

        return ResponseEntity.status(response.getStatus()).body(response);
    }
}
