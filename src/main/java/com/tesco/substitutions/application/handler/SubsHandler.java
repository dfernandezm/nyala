package com.tesco.substitutions.application.handler;

import com.tesco.personalisation.commons.vertx.errorhandling.ErrorHandler;
import com.tesco.substitutions.application.SubstitutionsServiceAdapter;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.http.HttpServerResponse;
import io.vertx.rxjava.ext.web.RoutingContext;
import java.util.List;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import rx.functions.Action1;

@Slf4j
public class SubsHandler {

    private static final String SUBSTITUTIONS_JSON_OBJECT_NAME_RESPONSE = "substitutions";
    public static final String UNAVAILABLE_TPNB_PARAMETER = "unavailableTpnb";
    public static final String UNAVAILABLE_MULTIPLE_TPNBS_PARAMETER = "unavailableTpnbs";
    public static final String BAD_REQUEST_ERROR_MESSAGE = "Unable to provide substitutions. The tpnb is not passed correctly";
    public static final String BAD_REQUEST_EMPTY_BODY_MESSAGE = "Unable to provide substitutions. The request body is empty";

    private final SubstitutionsServiceAdapter substitutionsServiceAdapter;
    private final SubTpnbValidator subTpnbValidator;

    @Inject
    public SubsHandler(final SubstitutionsServiceAdapter substitutionsServiceAdapter,
            SubTpnbValidator subTpnbValidator) {
        this.substitutionsServiceAdapter = substitutionsServiceAdapter;
        this.subTpnbValidator = subTpnbValidator;
    }

    public void substitutions(final RoutingContext routingContext) {
        final HttpServerResponse response = routingContext.response();
        final String unavailableTpnb = routingContext.request().getParam(UNAVAILABLE_TPNB_PARAMETER);
        if (subTpnbValidator.validateTpnb(unavailableTpnb)) {
            log.info("Asking for substitutions for {} ", unavailableTpnb);
            this.obtainSubstitutionsFor(response, unavailableTpnb);
        } else {
            log.info("Bad request happened, parameters could not been validated");
            response.setStatusCode(HttpStatus.SC_BAD_REQUEST).setStatusMessage(BAD_REQUEST_ERROR_MESSAGE).end();
        }
    }

    public void bulkSubstitutions(RoutingContext routingContext) {
        final HttpServerResponse response = routingContext.response();
        //TODO once we update the version of vert.x to >3.7.0 this can be simplified to
        // if (routingContext.getBodyAsJson().isEmpty())
        // but as of now vert.x does not respect the length of the body,
        // i.e. it tries to parse json object even if body is an empty string which is not quite correct
        // see https://github.com/vert-x3/vertx-web/issues/1133
        if (routingContext.getBody().length() == 0) {
            log.info("Bad request received, body is empty");
            response.setStatusCode(HttpStatus.SC_BAD_REQUEST).setStatusMessage(BAD_REQUEST_EMPTY_BODY_MESSAGE).end();
        }
        final List<String> unavailableTpnbs = routingContext.getBodyAsJson().getJsonArray(UNAVAILABLE_MULTIPLE_TPNBS_PARAMETER).getList();
        if (subTpnbValidator.validateTpnbs(unavailableTpnbs)) {
            log.info("Asking for multiple substitutions for {} ", unavailableTpnbs);
            this.obtainBulkSubstitutionsFor(response, unavailableTpnbs);
        } else {
            log.info("Bad request received, parameters could not be validated");
            response.setStatusCode(HttpStatus.SC_BAD_REQUEST).setStatusMessage(BAD_REQUEST_ERROR_MESSAGE).end();
        }
    }

    private void obtainSubstitutionsFor(final HttpServerResponse response, final String unavailableTpnb) {
        this.substitutionsServiceAdapter.obtainSubstitutionsFor(unavailableTpnb)
                .subscribe(resultHandler(response), errorHandler(response, "Error obtaining substitutions"));
    }

    private void obtainBulkSubstitutionsFor(final HttpServerResponse response, final List<String> unavailableTpnbs) {
        this.substitutionsServiceAdapter.obtainBulkSubstitutionsFor(unavailableTpnbs)
                .subscribe(resultHandler(response), errorHandler(response, "Error obtaining multiple substitutions"));
    }

    private <T> Action1<List<T>> resultHandler(final HttpServerResponse httpServerResponse) {
        return result -> {
            final JsonObject responseObject = new JsonObject();
            responseObject.put(SUBSTITUTIONS_JSON_OBJECT_NAME_RESPONSE, new JsonArray(result));
            httpServerResponse.setStatusCode(HttpStatus.SC_OK).end(responseObject.encodePrettily());
        };
    }

    private Action1<Throwable> errorHandler(final HttpServerResponse httpServerResponse, String errorMessage){
        return error -> ErrorHandler.prepareErrorResponse(httpServerResponse, error, errorMessage);
    }
}
