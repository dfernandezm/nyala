package com.nyala.server.common.errorhandling;

import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApiError {
    private String errorMessage;
    private Integer statusCode;

    public ApiError(HttpResponseStatus resultCode, String message) {
        this.errorMessage = message;
        this.statusCode = resultCode.code();
    }

    public ApiError(Integer statusCode, String message) {
        this.errorMessage = message;
        this.statusCode = statusCode;
    }

    public static ApiError create(HttpResponseStatus responseStatus, String message) {
        return new ApiError(responseStatus, message);
    }


}
