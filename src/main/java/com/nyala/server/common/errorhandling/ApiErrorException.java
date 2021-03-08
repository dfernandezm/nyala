package com.nyala.server.common.errorhandling;

import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.Generated;

public class ApiErrorException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private HttpResponseStatus responseStatus;

    public ApiErrorException(HttpResponseStatus resultCode, String message) {
        super(message);
        this.responseStatus = resultCode;
    }

    public ApiErrorException(HttpResponseStatus responseStatus, String message, Throwable cause) {
        super(message, cause);
        this.responseStatus = responseStatus;
    }

    public Integer getStatusCode() {
        return this.responseStatus.code();
    }

    public String getStatusAsString() {
        return this.responseStatus.codeAsText().toString();
    }

    public HttpResponseStatus getResponseStatus() {
        return this.responseStatus;
    }

    public static ApiErrorException create(HttpResponseStatus responseStatus, String message) {
        return new ApiErrorException(responseStatus, message);
    }

    public static ApiErrorException create(HttpResponseStatus responseStatus, String message, Throwable cause) {
        return new ApiErrorException(responseStatus, message, cause);
    }

    @Generated
    public void setResponseStatus(HttpResponseStatus responseStatus) {
        this.responseStatus = responseStatus;
    }
}

