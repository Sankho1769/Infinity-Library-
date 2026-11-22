package com.librarymanagement.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private final String type;
    private final String title;
    private final int status;
    private final String detail;
    private final String instance;
    private final String errorCode;
    private final Instant timestamp;
    private final Map<String, String> invalidParams;

    public ErrorResponse(String type, String title, int status, String detail, String instance, String errorCode, Map<String, String> invalidParams) {
        this.type = type;
        this.title = title;
        this.status = status;
        this.detail = detail;
        this.instance = instance;
        this.errorCode = errorCode;
        this.timestamp = Instant.now();
        this.invalidParams = invalidParams;
    }

    public String getType() { return type; }
    public String getTitle() { return title; }
    public int getStatus() { return status; }
    public String getDetail() { return detail; }
    public String getInstance() { return instance; }
    public String getErrorCode() { return errorCode; }
    public Instant getTimestamp() { return timestamp; }
    public Map<String, String> getInvalidParams() { return invalidParams; }
}
