package com.bst.server.common.utils;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Standard API response")
public class CustomResponse<T> {
    @Schema(example = "Company created successfully")
    private String message;

    private T data;

    private T errors;

    @Schema(example = "OK")
    private HttpStatus status;

    @Schema(example = "2026-03-13T15:12:26")
    private OffsetDateTime timestamp;

    private String apiPath;
}
