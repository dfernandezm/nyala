package com.nyala.common.errorhandling;

import com.mongodb.MongoException;
import com.nyala.common.jsonutils.JsonUtils;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.MultiMap;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.DecodeException;
import io.vertx.rxjava.core.eventbus.Message;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ErrorHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(ErrorHelper.class);
    public static final Integer DEFAULT_ERROR_STATUS_CODE = 500;

    public ErrorHelper() {
    }

    public static void manageBadRequestError(Throwable e, HttpServerResponse response) {
        LOGGER.error("Bad Request Error detected", e);
        String errorMessage = e.getMessage();
        int statusCode = 400;
        respondWithError((HttpServerResponse)response, statusCode, errorMessage);
    }

    public static void manageAppError(ReplyException cause, HttpServerResponse response) {
        LOGGER.error("Error occurred during application execution: ", cause);
        String errorMessage = "Error occurred during processing";
        int statusCode;
        if (isExistingHttpStatusCode(cause.failureCode())) {
            statusCode = cause.failureCode();
            if (StringUtils.isNotEmpty(cause.getMessage())) {
                errorMessage = cause.getMessage();
            }
        } else {
            ApiError apiError = getErrorFromException(cause);
            statusCode = apiError.getStatusCode();
            errorMessage = apiError.getErrorMessage();
        }

        respondWithError(response, statusCode, errorMessage);
    }

    private static boolean isExistingHttpStatusCode(int failureCode) {
        try {
            HttpResponseStatus.valueOf(failureCode);
            return true;
        } catch (IllegalArgumentException var2) {
            LOGGER.info(failureCode + " is not a valid HttpStatusCode");
            return false;
        }
    }

    private static void addResponseHeaders(HttpServerResponse response) {
        MultiMap headersMultimap = MultiMap.caseInsensitiveMultiMap();
        headersMultimap.add("Content-Type", "application/json");
        response.headers().addAll(headersMultimap);
    }

    private static void addResponseHeaders(io.vertx.rxjava.core.http.HttpServerResponse response) {
        io.vertx.rxjava.core.MultiMap headersMultimap = io.vertx.rxjava.core.MultiMap.caseInsensitiveMultiMap();
        headersMultimap.add("Content-Type", "application/json");
        response.headers().addAll(headersMultimap);
    }

    private static void respondWithError(HttpServerResponse response, int statusCode, String resultMessage) {
        HttpResponseStatus responseStatus = HttpResponseStatus.valueOf(statusCode);
        ApiError apiError = ApiError.create(responseStatus, resultMessage);
        String statusMessage = responseStatus.reasonPhrase();
        addResponseHeaders(response);
        response.setStatusCode(statusCode).setStatusMessage(statusMessage).end(JsonUtils.encodeResult(apiError));
    }

    private static void respondWithError(io.vertx.rxjava.core.http.HttpServerResponse response, int statusCode, String resultMessage) {
        HttpResponseStatus responseStatus = HttpResponseStatus.valueOf(statusCode);
        ApiError apiError = ApiError.create(responseStatus, resultMessage);
        String statusMessage = responseStatus.reasonPhrase();
        addResponseHeaders(response);
        response.setStatusCode(statusCode).setStatusMessage(statusMessage).end(JsonUtils.encodeResult(apiError));
    }

    public static void failWithUnauthorized(Message message, String errorMessage) {
        message.fail(HttpResponseStatus.FORBIDDEN.code(), errorMessage);
    }

    public static void respondWithUnauthorized(HttpServerResponse response, Throwable cause, String message) {
        LOGGER.error("Error occurred during application execution: ", cause);
        respondWithError(response, HttpResponseStatus.FORBIDDEN.code(), message);
    }

    public static void respondWithUnauthorized(io.vertx.rxjava.core.http.HttpServerResponse response, Throwable cause, String message) {
        LOGGER.error("Error occurred during application execution: ", cause);
        respondWithError(response, HttpResponseStatus.FORBIDDEN.code(), message);
    }

    public static void failMessageWithException(Message message, Throwable cause, String defaultErrorMessage) {
        if (cause != null) {
            LOGGER.error("Failure with cause", cause);
            ApiError apiError = getErrorFromException(cause);
            message.fail(apiError.getStatusCode(), apiError.getErrorMessage());
        } else {
            message.fail(DEFAULT_ERROR_STATUS_CODE, defaultErrorMessage);
        }
    }

    private static ApiError getErrorFromException(Throwable cause) {
        String errorMessage;
        HttpResponseStatus responseStatus;
        if (cause instanceof ApiErrorException) {
            ApiErrorException apiErrorException = (ApiErrorException)cause;
            errorMessage = apiErrorException.getMessage();
            responseStatus = apiErrorException.getResponseStatus();
        } else if (cause instanceof DecodeException) {
            errorMessage = cause.getMessage();
            responseStatus = HttpResponseStatus.BAD_REQUEST;
        } else if (cause instanceof MongoException) {
            errorMessage = "Mongo Error: " + cause.getMessage();
            responseStatus = HttpResponseStatus.INTERNAL_SERVER_ERROR;
        } else {
            errorMessage = cause.getMessage();
            responseStatus = HttpResponseStatus.INTERNAL_SERVER_ERROR;
        }

        return ApiError.create(responseStatus, errorMessage);
    }
}

