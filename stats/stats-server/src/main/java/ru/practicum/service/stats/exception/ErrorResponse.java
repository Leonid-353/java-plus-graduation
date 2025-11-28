package ru.practicum.service.stats.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ErrorResponse {
    String status;
    String description;
    String message;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime timestamp;

    public ErrorResponse(String status, String description, String message) {
        this.status = status;
        this.description = description;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }
}
