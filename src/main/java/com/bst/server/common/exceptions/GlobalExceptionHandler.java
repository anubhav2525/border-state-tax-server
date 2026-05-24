package com.bst.server.common.exceptions;

import com.bst.server.common.exceptions.sub.*;
import com.bst.server.common.utils.CreateResponseEntity;
import com.bst.server.common.utils.CustomResponse;
import com.fasterxml.jackson.databind.JsonMappingException;
import lombok.AllArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.InvalidFormatException;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Order(999)
@RestControllerAdvice
@AllArgsConstructor
public class GlobalExceptionHandler {
    private final CreateResponseEntity createResponseEntity;
    private static final String handler = "Global exception";

    // handles @Valid failures on @RequestBody -> 400
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CustomResponse<Map<String, String>>> handleValidation(
            MethodArgumentNotValidException ex, WebRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }
        return createResponseEntity.buildExceptionResponse("Validation failed", fieldErrors, handler, request, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<CustomResponse<Void>> handleUnreadable(
            HttpMessageNotReadableException ex,
            WebRequest request
    ) {

        String message = "Invalid request body";

        Throwable rootCause = ex.getMostSpecificCause();

        if (rootCause instanceof InvalidFormatException invalidFormatException) {

            Class<?> targetType = invalidFormatException.getTargetType();

            if (targetType.isEnum()) {

                String fieldName = invalidFormatException.getPath()
                        .stream()
                        .map(JacksonException.Reference::getPropertyName)
                        .findFirst()
                        .orElse("field");

                Object invalidValue = invalidFormatException.getValue();

                String acceptedValues = Arrays.stream(targetType.getEnumConstants())
                        .map(Object::toString)
                        .collect(Collectors.joining(", "));

                message = String.format(
                        "Invalid value '%s' for field '%s'. Accepted values are: [%s]",
                        invalidValue,
                        fieldName,
                        acceptedValues
                );
            }
        }

        return createResponseEntity.buildExceptionResponse(
                message,
                handler,
                request,
                HttpStatus.BAD_REQUEST
        );
    }

    // Operation Not Allowed -> 400
    @ExceptionHandler(HttpOperationNotAllowedException.class)
    public ResponseEntity<CustomResponse<Void>> handleOperationNotAllowed(
            HttpOperationNotAllowedException ex, WebRequest request) {
        return createResponseEntity.buildExceptionResponse(ex.getMessage(), handler, request, HttpStatus.METHOD_NOT_ALLOWED);
    }

    // handles missing @RequestParam -> 400
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<CustomResponse<Void>> handleMissingParam(
            MissingServletRequestParameterException ex, WebRequest request) {
        return createResponseEntity.buildExceptionResponse("Required parameter '" + ex.getParameterName() + "' is missing", handler, request, HttpStatus.BAD_REQUEST);
    }

    // handles wrong type for path variable e.g. passing a string instead of UUID -> 400
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<CustomResponse<Void>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, WebRequest request) {
        String message = "Invalid value '" + ex.getValue() + "' for parameter '" + ex.getName() + "'";
        return createResponseEntity.buildExceptionResponse(message, handler, request, HttpStatus.BAD_REQUEST);
    }

    // Duplicate resource -> 409
    @ExceptionHandler(ResourceAlreadyExistsException.class)
    public ResponseEntity<CustomResponse<Void>> handleResourceAlreadyExists(
            ResourceAlreadyExistsException ex, WebRequest request) {
        return createResponseEntity.buildExceptionResponse(ex.getMessage(), handler, request, HttpStatus.CONFLICT);
    }

    // Resource not found -> 404
    @ExceptionHandler(ResourceNotExistsException.class)
    public ResponseEntity<CustomResponse<Void>> handleResourceNotExists(
            ResourceNotExistsException ex, WebRequest request) {
        return createResponseEntity.buildExceptionResponse(ex.getMessage(), handler, request, HttpStatus.NOT_FOUND);
    }

    // Already deleted -> 410
    @ExceptionHandler(ResourceAlreadyDeletedException.class)
    public ResponseEntity<CustomResponse<Void>> handleResourceAlreadyDeleted(
            ResourceAlreadyDeletedException ex,
            WebRequest request) {
        return createResponseEntity.buildExceptionResponse(ex.getMessage(), handler, request, HttpStatus.ALREADY_REPORTED);
    }

    // Validation exception -> 400
    @ExceptionHandler(ResourceValidationException.class)
    public ResponseEntity<CustomResponse<Void>> handleResourceValidation(
            ResourceValidationException ex,
            WebRequest request) {
        return createResponseEntity.buildExceptionResponse(ex.getMessage(), handler, request, HttpStatus.BAD_REQUEST);
    }

    // Already disabled exception -> 400
    @ExceptionHandler(ResourceAlreadyDisabledException.class)
    public ResponseEntity<CustomResponse<Void>> handleResourceAlreadyDisabled(
            ResourceAlreadyDisabledException ex,
            WebRequest request) {
        return createResponseEntity.buildExceptionResponse(ex.getMessage(), handler, request, HttpStatus.BAD_REQUEST);
    }

    // Already enabled exception -> 400
    @ExceptionHandler(ResourceAlreadyEnabledException.class)
    public ResponseEntity<CustomResponse<Void>> handleResourceAlreadyEnabled(
            ResourceAlreadyEnabledException ex,
            WebRequest request) {
        return createResponseEntity.buildExceptionResponse(ex.getMessage(), handler, request, HttpStatus.BAD_REQUEST);
    }

    // In Use exception -> 226
    @ExceptionHandler(ResourceInUseException.class)
    public ResponseEntity<CustomResponse<Void>> handleResourceInUse(
            ResourceInUseException ex,
            WebRequest request) {
        return createResponseEntity.buildExceptionResponse(ex.getMessage(), handler, request, HttpStatus.IM_USED);
    }

    // In Use exception -> 400
    @ExceptionHandler(ResourceNotDeletedException.class)
    public ResponseEntity<CustomResponse<Void>> handleResourceNotDeleted(
            ResourceNotDeletedException ex,
            WebRequest request) {
        return createResponseEntity.buildExceptionResponse(ex.getMessage(), handler, request, HttpStatus.BAD_REQUEST);
    }

    // Resource Not Restore Exception -> 400
    @ExceptionHandler(ResourceNotRestoreException.class)
    public ResponseEntity<CustomResponse<Void>> handleResourceNotRestore(
            ResourceNotRestoreException ex,
            WebRequest request) {
        return createResponseEntity.buildExceptionResponse(ex.getMessage(), handler, request, HttpStatus.BAD_REQUEST);
    }

    // Resource Not Restore Exception -> 405
    @ExceptionHandler(ResourceOperationNotAllowed.class)
    public ResponseEntity<CustomResponse<Void>> handleResourceOperationNotAllowed(
            ResourceOperationNotAllowed ex,
            WebRequest request) {
        return createResponseEntity.buildExceptionResponse(ex.getMessage(), handler, request, HttpStatus.METHOD_NOT_ALLOWED);
    }

    // catch 500 status code
    // catch-all for any unhandled exception
    @ExceptionHandler(Exception.class)
    public ResponseEntity<CustomResponse<Void>> handleGeneric(
            Exception ex, WebRequest request) {
        return createResponseEntity.buildExceptionResponse("An unexpected error occurred: " + ex.getMessage(), handler, request, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
