package com.socialworld.app.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Generic
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Request validation failed."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "Resource not found."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred."),

    // Auth
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Invalid username or password."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "Refresh token is invalid or expired."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Authentication required."),
    ACCOUNT_SUSPENDED(HttpStatus.FORBIDDEN, "This account is suspended."),

    // Registration
    USERNAME_TAKEN(HttpStatus.CONFLICT, "Username is already taken."),
    EMAIL_TAKEN(HttpStatus.CONFLICT, "Email is already registered."),
    UNDERAGE(HttpStatus.UNPROCESSABLE_ENTITY, "You must be at least 18 years old.");

    private final HttpStatus status;
    private final String defaultMessage;
}
