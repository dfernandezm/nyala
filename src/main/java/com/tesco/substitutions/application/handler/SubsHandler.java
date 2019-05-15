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
    private static final String TPNB_UNAVAILABLE_PRODUCT_PARAMETER = "unavailableTpnb";
    private static final String BAD_REQUEST_ERROR_MESSAGE = "Unable to provide substitutions. The tpnb is not passed correctly";
    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    private final SubstitutionsApplicationService substitutionsApplicationService;

    @Inject
    public SubsHandler(final SubstitutionsApplicationService substitutionsApplicationService) {
        this.substitutionsApplicationService = substitutionsApplicationService;
    }

    private static boolean validateTpnb(final String tpnb) {
        return StringUtils.isNotEmpty(tpnb);
    }

    public void substitutions(final RoutingContext routingContext) {

        final HttpServerResponse response = routingContext.response();
        final String unavailableTpnb = routingContext.request().getParam(TPNB_UNAVAILABLE_PRODUCT_PARAMETER);
        if (SubsHandler.validateTpnb(unavailableTpnb)) {
            this.LOGGER.info("Asking for substitutions for {} ", unavailableTpnb);
            this.obtainCandidateSubstitutionsFor(response, unavailableTpnb);
        } else {
            this.LOGGER.info("Bad request happened, parameters could not been validated");
            response.setStatusCode(HttpStatus.SC_BAD_REQUEST).setStatusMessage(
                    BAD_REQUEST_ERROR_MESSAGE)
                    .end();
        }
    }

    private void obtainCandidateSubstitutionsFor(final HttpServerResponse response, final String unavailableTpnb) {
        this.substitutionsApplicationService.obtainCandidateSubstitutionsFor(unavailableTpnb)
                .subscribe(result -> {
                    final JsonObject responseObject = new JsonObject();
                    responseObject.put(SUBSTITUTIONS_JSON_OBJECT_NAME_RESPONSE, new JsonArray(result));
                    response.setStatusCode(HttpStatus.SC_OK).end(responseObject.encodePrettily());
                }, error -> ErrorHandler.prepareErrorResponse(response, error));
    }

}
