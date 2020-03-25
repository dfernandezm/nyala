package com.tesco.substitutions.commons.errorhandling;

import io.netty.handler.codec.http.HttpResponseStatus;

public class UnAuthorizedException extends ApiErrorException {
    public UnAuthorizedException(String message) {
        super(HttpResponseStatus.FORBIDDEN, message);
    }
}
