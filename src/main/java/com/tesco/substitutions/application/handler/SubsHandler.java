package com.tesco.substitutions.application.handler;


import com.tesco.substitutions.commons.vertx.errorhandling.ErrorHandler;
import com.tesco.substitutions.domain.model.UnavailableProduct;
import com.tesco.substitutions.domain.service.SubstitutionsService;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.http.HttpServerResponse;
import io.vertx.rxjava.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import rx.functions.Action1;

import javax.inject.Singleton;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Singleton
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

    public SubsHandler(final SubstitutionsService substitutionsService,
                       final TpnbValidator tpnbValidator, final StoreIdValidator storeIdValidator) {
        this.substitutionsService = substitutionsService;
        this.tpnbValidator = tpnbValidator;
        this.storeIdValidator = storeIdValidator;
    }

    public void substitutions(final RoutingContext routingContext) {

        //final HttpServerResponse response = routingContext.response();
        //this.returnErrorIfEmptyRequestBody(routingContext, response);

        //final JsonObject requestJson = routingContext.getBodyAsJson();
        String url = "https://e10.habrox.xyz/ingestnb4s/espn3_sur/f.m3u8";
        String proxyUrlOutput = proxyUrl(url);
        routingContext.response().putHeader("Content-Type", "application/vnd.apple.mpegurl").end(proxyUrlOutput);


        //final List<String> unavailableTpnbs = this.getTpnbs(requestJson.getJsonArray(UNAVAILABLE_TPNB_PARAMETER));
        //this.logRequestAndAddCorrelationID(UID, response, requestJson);

//        if (this.hasStoreId(requestJson)) {
//            this.handleRequestWithStoreId(response, requestJson, unavailableTpnbs, UID);
//        } else {
//            this.handleRequestWithoutStoreId(response, requestJson, unavailableTpnbs, UID);
//        }
    }

    public static String proxyUrl(String url) {

        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                    public void checkClientTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                    }
                    public void checkServerTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                    }
                }
        };

        // Install the all-trusting trust manager
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
        }

        try {
            URL urlChannel = new URL(url);
            HttpURLConnection connection = (HttpURLConnection)  urlChannel.openConnection();
            connection.setConnectTimeout(1000);
            connection.setReadTimeout(1000);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Referer", "https://espn-live.stream/stream/54015.html");
            connection.setRequestProperty("Origin", "https://espn-live.stream/stream/54015.html");
            connection.setRequestProperty("User-Agent", " Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/85.0.4183.102 Safari/537.36");
            connection.connect();
            int statusCode = connection.getResponseCode();
            String contentType = connection.getContentType();
            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String output = IOUtils.toString(br);
            log.info("{} -> Response code: {}, Content Type: {}", url, statusCode, contentType);
            log.info("Response content: {}", output);
            return output;
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }



    private void returnInvalidParametersResponse(final String UID, final HttpServerResponse response,
                                                 final JsonObject requestJson) {
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

    private void handleRequestWithStoreId(final HttpServerResponse response, final JsonObject requestJson,
                                          final List unavailableTpnbs,
            final String uid) {
        final String storeId = requestJson.getString(STORE_ID_PARAMETER);
        if (this.requestParametersAreValid(storeId, unavailableTpnbs)) {
            log.info("{}: Asking for substitutions for {} in store {}", uid, unavailableTpnbs, storeId);
            this.obtainSubstitutionsFor(response, storeId, unavailableTpnbs);
        } else {
            this.returnInvalidParametersResponse(uid, response, requestJson);
        }
    }

    private void handleRequestWithoutStoreId(final HttpServerResponse response, final JsonObject requestJson,
                                             final List unavailableTpnbs,
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
