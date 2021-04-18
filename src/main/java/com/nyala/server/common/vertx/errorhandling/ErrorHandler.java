package com.nyala.server.common.vertx.errorhandling;


import com.nyala.server.common.errorhandling.ApiError;
import com.nyala.server.common.jsonutils.JsonUtils;
import com.nyala.server.common.errorhandling.ApiErrorException;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.json.DecodeException;
import io.vertx.rxjava.core.http.HttpServerResponse;

import java.util.concurrent.TimeoutException;

public class ErrorHandler {
    private static final String DEFAULT_TIMEOUT_MESSAGE = "Connection timeout";
    private static final String BREAK_LINE = System.lineSeparator();

    public static void prepareErrorResponse(HttpServerResponse response, Throwable cause) {
        prepareErrorResponse(response, cause, DEFAULT_TIMEOUT_MESSAGE);
    }

    public static void prepareErrorResponse(HttpServerResponse response, Throwable cause,
                                            String connectionTimeoutMessage) {
        String errorMessage;
        HttpResponseStatus responseStatus;
        if (cause instanceof ApiErrorException) {
            ApiErrorException apiErrorException = (ApiErrorException)cause;
            errorMessage = getDefaultErrorMessageWithoutBreakLines(apiErrorException);
            responseStatus = apiErrorException.responseStatus;
        } else if (cause instanceof DecodeException) {
            errorMessage = getDefaultErrorMessageWithoutBreakLines(cause);
            responseStatus = HttpResponseStatus.BAD_REQUEST;
        } else if (cause instanceof TimeoutException) {
            errorMessage = connectionTimeoutMessage;
            responseStatus = getDefaultErrorStatus();
        } else {
            errorMessage = getDefaultErrorMessageWithoutBreakLines(cause);
            responseStatus = getDefaultErrorStatus();
        }

        ApiError apiError = ApiError.create(responseStatus, errorMessage);
        response.setStatusCode(responseStatus.code()).setStatusMessage(errorMessage).end(JsonUtils.encodeResult(apiError));
    }

    private static String getDefaultErrorMessageWithoutBreakLines(Throwable cause) {
        return cause.getMessage().replace(BREAK_LINE, "");
    }

    private static HttpResponseStatus getDefaultErrorStatus() {
        return HttpResponseStatus.INTERNAL_SERVER_ERROR;
    }
}
