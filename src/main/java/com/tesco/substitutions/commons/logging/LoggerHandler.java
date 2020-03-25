package com.tesco.substitutions.commons.logging;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LoggerHandler implements Handler<RoutingContext> {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoggerHandler.class);
    private Map<String, String> requestIdToUid = new ConcurrentHashMap();
    private static volatile ConcurrentHashMap<String, String> requestsMap = new ConcurrentHashMap();

    public LoggerHandler() {
    }

    public void handle(RoutingContext context) {
        long timestamp = System.currentTimeMillis();
        String requestId = UUID.randomUUID().toString();
        if (!context.request().uri().contains("healthCheck")) {
            LOGGER.debug("Starting Request: {}", requestId);
            requestsMap.put(requestId, context.request().uri());
        }

        context.addHeadersEndHandler((v) -> {
            this.getUidFromHeadersAndClear(requestId, context);
        });
        context.addBodyEndHandler((v) -> {
            this.log(context, requestId, timestamp);
        });
        context.request().response().endHandler((v) -> {
            if (!context.request().uri().contains("healthCheck")) {
                LOGGER.debug("Ending Request: {}", requestId);
                requestsMap.remove(requestId);
            }

        });
        context.next();
    }

    private void getUidFromHeadersAndClear(String requestId, RoutingContext context) {
        String headerUid = context.response().headers().get("correlation-id");
        context.response().headers().remove("correlation-id");
        if (headerUid != null) {
            this.requestIdToUid.put(requestId, headerUid);
        }

    }

    private void log(RoutingContext context, String requestId, long timestamp) {
        String uri = context.request().uri();
        uri = this.obfuscateTokenInUri(uri);
        HttpMethod method = context.request().method();
        int status = context.response().getStatusCode();
        String clientId = context.request().headers().get("ClientId");
        String uid = (String)this.requestIdToUid.get(requestId);
        if (!uri.contains("/healthCheck")) {
            LOGGER.info("RequestId: {} Client: {} User: {} EndPoint: {} {} Status: {} TotalTime: {} ms", new Object[]{requestId, clientId, uid, method, uri, status, System.currentTimeMillis() - timestamp});
        }

    }

    public static Integer getPendingRequestsCount() {
        return requestsMap.keySet().size();
    }

    public static void logPendingRequests() {
        if (!requestsMap.keySet().isEmpty()) {
            requestsMap.keySet().forEach((key) -> {
                LOGGER.info("PendingRequest {} -> {}", key, requestsMap.get(key));
            });
        }

    }

    private String obfuscateTokenInUri(String uri) {
        return uri.replaceAll("access_token=[0-9a-f]{8}\\-[0-9a-f]{4}\\-[0-9a-f]{4}\\-[0-9a-f]{4}\\-[0-9a-f]{12}", "access_token=****************");
    }
}
