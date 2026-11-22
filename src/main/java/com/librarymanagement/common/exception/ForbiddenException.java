package com.librarymanagement.common.exception;

public class ForbiddenException extends BusinessException {
    public ForbiddenException(String message) {
        super(ErrorCode.ACCESS_DENIED, message);
    }

    public ForbiddenException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
