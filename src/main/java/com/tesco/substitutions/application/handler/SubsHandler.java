package com.tesco.substitutions.application.handler;

import com.tesco.personalisation.commons.vertx.errorhandling.ErrorHandler;
import com.tesco.substitutions.domain.model.UnavailableProduct;
import com.tesco.substitutions.domain.service.SubstitutionsService;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.http.HttpServerResponse;
import io.vertx.rxjava.ext.web.RoutingContext;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import rx.functions.Action1;

@Slf4j
public class SubsHandler {

    public static final String UNAVAILABLE_TPNB_PARAMETER = "unavailableTpnbs";
    public static final String STORE_ID_PARAMETER = "storeId";
    public static final String BAD_REQUEST_ERROR_MESSAGE = "Unable to provide substitutions. The tpnb is not passed correctly";
    public static final String BAD_REQUEST_EMPTY_BODY_MESSAGE = "Unable to provide substitutions. The request body is empty";
    public static final String REQUEST_BODY_ERROR_HEADER = "Incorrect-Request-Body-Parameters";
    public static final String UID_HEADER = "correlation-id";
    private static final String SUBSTITUTIONS_JSON_OBJECT_NAME_RESPONSE = "substitutions";
    private final SubstitutionsService substitutionsService;
    private final TpnbValidator tpnbValidator;
    private final StoreIdValidator storeIdValidator;

    @Inject
    public SubsHandler(final SubstitutionsService substitutionsService,
            final TpnbValidator tpnbValidator, final StoreIdValidator storeIdValidator) {
        this.substitutionsService = substitutionsService;
        this.tpnbValidator = tpnbValidator;
        this.storeIdValidator = storeIdValidator;
    }

    public void substitutions(final RoutingContext routingContext) {
        final String UID = UUID.randomUUID().toString();
        final HttpServerResponse response = routingContext.response();

        this.returnErrorIfEmptyRequestBody(routingContext, response);

        final JsonObject requestJson = routingContext.getBodyAsJson();
        final List<String> unavailableTpnbs = this.getTpnbs(requestJson.getJsonArray(UNAVAILABLE_TPNB_PARAMETER));
        this.logRequestAndAddCorrelationID(UID, response, requestJson);

        if (this.hasStoreId(requestJson)) {
            this.handleRequestWithStoreId(response, requestJson, unavailableTpnbs, UID);
        } else {
            this.handleRequestWithoutStoreId(response, requestJson, unavailableTpnbs, UID);
        }
    }

    private void returnInvalidParametersResponse(final String UID, final HttpServerResponse response, final JsonObject requestJson) {
        log.info("{}: Bad request received, parameters could not be validated: {}", UID, requestJson);
        response.setStatusCode(HttpStatus.SC_BAD_REQUEST).setStatusMessage(BAD_REQUEST_ERROR_MESSAGE).end();
    }

    private void returnErrorIfEmptyRequestBody(final RoutingContext routingContext, final HttpServerResponse response) {
        // TODO once we update the version of vert.x to >3.7.0 this can be simplified to
        // if (routingContext.getBodyAsJson().isEmpty())
        // but as of now vert.x does not respect the length of the body,
        // i.e. it tries to parse json object even if body is an empty string which is not quite correct
        // see https://github.com/vert-x3/vertx-web/issues/1133
        if (routingContext.getBody().length() == 0) {
            log.info("Bad request received, body is empty");
            response.setStatusCode(HttpStatus.SC_BAD_REQUEST).setStatusMessage(BAD_REQUEST_EMPTY_BODY_MESSAGE).end();
        }
    }

    private void handleRequestWithStoreId(final HttpServerResponse response, final JsonObject requestJson, final List unavailableTpnbs,
            final String uid) {
        final String storeId = requestJson.getString(STORE_ID_PARAMETER);
        if (this.requestParametersAreValid(storeId, unavailableTpnbs)) {
            log.info("{}: Asking for substitutions for {} in store {}", uid, unavailableTpnbs, storeId);
            this.obtainSubstitutionsFor(response, storeId, unavailableTpnbs);
        } else {
            this.returnInvalidParametersResponse(uid, response, requestJson);
        }
    }

    private void handleRequestWithoutStoreId(final HttpServerResponse response, final JsonObject requestJson, final List unavailableTpnbs,
            final String uid) {
        if (this.areValidTpnbs(unavailableTpnbs)) {
            log.info("{}: No StoreId provided, asking for default substitutions for these unavailable products {} ", uid, unavailableTpnbs);
            this.obtainSubstitutionsFor(response, unavailableTpnbs);
        } else {
            this.returnInvalidParametersResponse(uid, response, requestJson);
        }
    }

    private List<String> getTpnbs(final JsonArray tpnbArray) {
        return tpnbArray.stream()
                .map(Object::toString)
                .collect(Collectors.toList());
    }

    private boolean hasStoreId(final JsonObject requestJson) {
        return StringUtils.isNotEmpty(requestJson.getString(STORE_ID_PARAMETER));
    }

    private boolean isValidStoreId(final String storeId) {
        return this.storeIdValidator.isValidStoreId(storeId);
    }

    private boolean areValidTpnbs(final List<String> unavailableTpnbs) {
        return this.tpnbValidator.areValidTpnbs(unavailableTpnbs);
    }

    // TODO: Should this perhaps validate them one by one and return only valid ones so whole batch doesn't fail
    private boolean requestParametersAreValid(final String storeId, final List<String> unavailableTpnbs) {
        return this.isValidStoreId(storeId) && this.areValidTpnbs(unavailableTpnbs);
    }

    private void obtainSubstitutionsFor(final HttpServerResponse response, final List<String> unavailableTpnbs) {
        // Header set to notify clients that they are not sending requests in the expected format
        response.putHeader(REQUEST_BODY_ERROR_HEADER, "No Store ID passed");
        this.substitutionsService.getSubstitutionsFor(this.getUnavailableProducts(unavailableTpnbs))
                .subscribe(this.resultHandler(response),
                        this.errorHandler(response, "Error obtaining multiple substitutions for unavailable products " + unavailableTpnbs));
    }

    private void obtainSubstitutionsFor(final HttpServerResponse response, final String storeId, final List<String> unavailableTpnbs) {
        this.substitutionsService.getSubstitutionsFor(storeId, this.getUnavailableProducts(unavailableTpnbs))
                .subscribe(this.resultHandler(response), this.errorHandler(response,
                        "Error obtaining multiple substitutions for unavailable products " + unavailableTpnbs + " and store id "
                                + storeId));
    }

    private List<UnavailableProduct> getUnavailableProducts(final List<String> unavailableTpnbs) {
        return unavailableTpnbs.stream()
                .map(UnavailableProduct::of)
                .collect(Collectors.toList());
    }

    private <T> Action1<List<T>> resultHandler(final HttpServerResponse httpServerResponse) {
        return result -> {
            final JsonObject responseObject = new JsonObject();
            responseObject.put(SUBSTITUTIONS_JSON_OBJECT_NAME_RESPONSE, new JsonArray(result));
            log.info("{}: Returning substitutions {}", httpServerResponse.headers().get(UID_HEADER), responseObject.encode());
            httpServerResponse.setStatusCode(HttpStatus.SC_OK).end(responseObject.encodePrettily());
        };
    }

    private Action1<Throwable> errorHandler(final HttpServerResponse httpServerResponse, final String errorMessage) {
        return error -> ErrorHandler.prepareErrorResponse(httpServerResponse, error, errorMessage);
    }

    private void logRequestAndAddCorrelationID(final String uid, final HttpServerResponse response, final JsonObject bodyJson) {
        response.headers().add(UID_HEADER, uid);
        log.info("{}: Request for subs for {}", uid, bodyJson.toString());
    }
}
