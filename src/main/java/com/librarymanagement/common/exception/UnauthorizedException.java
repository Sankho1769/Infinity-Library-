package com.librarymanagement.common.exception;

public class UnauthorizedException extends BusinessException {
    public UnauthorizedException(String message) {
        super(ErrorCode.UNAUTHORIZED_OPERATION, message);
    }

    public UnauthorizedException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
