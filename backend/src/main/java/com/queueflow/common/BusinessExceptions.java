package com.queueflow.common;

import org.springframework.http.HttpStatus;

/** Common exception factories. Domain-specific services may still use their own codes. */
public final class BusinessExceptions {
    private BusinessExceptions() {}

    public static BusinessException validation(String message) {
        return new BusinessException(ErrorCode.VALIDATION_ERROR, message, HttpStatus.BAD_REQUEST);
    }

    public static BusinessException notFound(String message) {
        return new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, message, HttpStatus.NOT_FOUND);
    }
}
