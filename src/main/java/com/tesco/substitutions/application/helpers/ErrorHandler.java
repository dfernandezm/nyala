package com.tesco.substitutions.application.helpers;

import com.tesco.personalisation.commons.errorhandling.ApiError;
import com.tesco.personalisation.commons.errorhandling.ApiErrorException;
import com.tesco.personalisation.commons.jsonutils.JsonUtils;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.json.DecodeException;
import io.vertx.rxjava.core.http.HttpServerResponse;
import java.util.concurrent.TimeoutException;

//TODO a lot of common code... does it make sense to add a generic error handler to commons jar?
public class ErrorHandler {

    //TODO this message shouldn't be hardcoded here...is seems like the only parameter, we can make this a common class that takes this is a parameter in the preparaErrorResponse.
    private static final String DEFAULT_TIMEOUT_MESSAGE = "Connecting timeout";
    private static final String BREAK_LINE = System.lineSeparator();

    public static void prepareErrorResponse(final HttpServerResponse response, final Throwable cause) {
        final String errorMessage;
        final HttpResponseStatus responseStatus;

        if (cause instanceof ApiErrorException) {
            final ApiErrorException apiErrorException = (ApiErrorException) cause;
            errorMessage = getDefaultErrorMessageWithoutBreakLines(apiErrorException);
            responseStatus = apiErrorException.getResponseStatus();
        } else if (cause instanceof DecodeException) {
            errorMessage = getDefaultErrorMessageWithoutBreakLines(cause);
            responseStatus = HttpResponseStatus.BAD_REQUEST;
        } else if (cause instanceof TimeoutException) {
            errorMessage = DEFAULT_TIMEOUT_MESSAGE;
            responseStatus = getDefaultErrorStatus();
        } else {
            errorMessage = getDefaultErrorMessageWithoutBreakLines(cause);
            responseStatus = getDefaultErrorStatus();
        }
        final ApiError apiError = ApiError.create(responseStatus, errorMessage);
        response.setStatusCode(responseStatus.code()).setStatusMessage(errorMessage).end(JsonUtils.encodeResult(apiError));
    }

    private static String getDefaultErrorMessageWithoutBreakLines(final Throwable cause) {
        return cause.getMessage().replace(BREAK_LINE, "");
    }

    private static HttpResponseStatus getDefaultErrorStatus() {
        return HttpResponseStatus.INTERNAL_SERVER_ERROR;
    }

}

