package com.tesco.substitutions.application.handler;

import com.tesco.substitutions.application.SubstitutionsApplicationService;
import com.tesco.substitutions.application.helpers.ErrorHandler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.http.HttpServerResponse;
import io.vertx.rxjava.ext.web.RoutingContext;
import javax.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SubsHandler {

    private static final String SUBSTITUTIONS_JSON_OBJECT_NAME_RESPONSE = "substitutions";
    public static final String TPNB_UNAVAILABLE_PRODUCT_PARAMETER = "unavailableTpnb";
    public static final String BAD_REQUEST_ERROR_MESSAGE = "Unable to provide substitutions. The tpnb is not passed correctly";
    private static final Logger LOGGER = LoggerFactory.getLogger(SubsHandler.class);

    private final SubstitutionsApplicationService substitutionsApplicationService;

    @Inject
    public SubsHandler(final SubstitutionsApplicationService substitutionsApplicationService) {
        this.substitutionsApplicationService = substitutionsApplicationService;
    }

    private static boolean validateTpnb(final String tpnb) {
        return StringUtils.isNotEmpty(tpnb) && isWellFormatted(tpnb);
    }

    private static boolean isWellFormatted(String tpnb) {
        try {
            Long.parseLong(tpnb);
        }catch(Exception e){
            LOGGER.info("tpnb is not well formatted {} ", tpnb);
            return false;
        }
        return true;
    }

    public void substitutions(final RoutingContext routingContext) {

        final HttpServerResponse response = routingContext.response();
        final String unavailableTpnb = routingContext.request().getParam(TPNB_UNAVAILABLE_PRODUCT_PARAMETER);
        if (validateTpnb(unavailableTpnb)) {
            LOGGER.info("Asking for substitutions for {} ", unavailableTpnb);
            this.obtainCandidateSubstitutionsFor(response,  Long.parseLong(unavailableTpnb));
        } else {
            LOGGER.info("Bad request happened, parameters could not been validated");
            response.setStatusCode(HttpStatus.SC_BAD_REQUEST).setStatusMessage(
                    BAD_REQUEST_ERROR_MESSAGE)
                    .end();
        }
    }

    private void obtainCandidateSubstitutionsFor(final HttpServerResponse response, final Long unavailableTpnb) {
        this.substitutionsApplicationService.obtainCandidateSubstitutionsFor(unavailableTpnb)
                .subscribe(result -> {
                    final JsonObject responseObject = new JsonObject();
                    responseObject.put(SUBSTITUTIONS_JSON_OBJECT_NAME_RESPONSE, new JsonArray(result));
                    response.setStatusCode(HttpStatus.SC_OK).end(responseObject.encodePrettily());
                }, error -> ErrorHandler.prepareErrorResponse(response, error));
    }

}
