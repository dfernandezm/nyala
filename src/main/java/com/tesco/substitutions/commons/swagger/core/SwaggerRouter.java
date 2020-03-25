package com.tesco.substitutions.commons.swagger.core;

import com.github.phiz71.vertx.swagger.router.DefaultServiceIdResolver;
import com.github.phiz71.vertx.swagger.router.ServiceIdResolver;
import com.github.phiz71.vertx.swagger.router.extractors.BodyParameterExtractor;
import com.github.phiz71.vertx.swagger.router.extractors.FormParameterExtractor;
import com.github.phiz71.vertx.swagger.router.extractors.ParameterExtractor;
import com.github.phiz71.vertx.swagger.router.extractors.PathParameterExtractor;
import com.github.phiz71.vertx.swagger.router.extractors.QueryParameterExtractor;
import com.tesco.substitutions.commons.errorhandling.ErrorHelper;
import com.tesco.substitutions.commons.swagger.extractors.HeadersParameterExtractor;
import io.swagger.models.HttpMethod;
import io.swagger.models.Operation;
import io.swagger.models.Swagger;
import io.vertx.core.MultiMap;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SwaggerRouter {

    private static final Logger vertxLogger = LoggerFactory.getLogger(SwaggerRouter.class);
    public static final String CUSTOM_STATUS_CODE_HEADER_KEY = "CUSTOM_STATUS_CODE";
    public static final String CUSTOM_STATUS_MESSAGE_HEADER_KEY = "CUSTOM_STATUS_MESSAGE";
    private static final Pattern PATH_PARAMETER_NAME = Pattern.compile("\\{([A-Za-z][A-Za-z0-9_]*)\\}");
    private static final Pattern PATH_PARAMETERS = Pattern.compile("\\{(.*?)\\}");
    private static final Map<HttpMethod, SwaggerRouter.RouteBuilder> ROUTE_BUILDERS = new EnumMap(HttpMethod.class);
    private static final Map<String, ParameterExtractor> PARAMETER_EXTRACTORS = new HashMap();

    public static Router swaggerRouter(Router baseRouter, Swagger swagger, EventBus eventBus) {
        return swaggerRouter(baseRouter, swagger, eventBus, new DefaultServiceIdResolver(), (Function)null);
    }

    public static Router swaggerRouter(Router baseRouter, Swagger swagger, EventBus eventBus, ServiceIdResolver serviceIdResolver) {
        return swaggerRouter(baseRouter, swagger, eventBus, serviceIdResolver, (Function)null);
    }

    public static Router swaggerRouter(Router baseRouter, Swagger swagger, EventBus eventBus, ServiceIdResolver serviceIdResolver, Function<RoutingContext, DeliveryOptions> configureMessage) {
        baseRouter.route().handler(BodyHandler.create());
        String basePath = getBasePath(swagger);
        swagger.getPaths().forEach((path, pathDescription) -> {
            pathDescription.getOperationMap().forEach((method, operation) -> {
                vertxLogger.info("Building route: " + method + " " + basePath + path);
                Route route = ((SwaggerRouter.RouteBuilder)ROUTE_BUILDERS.get(method)).buildRoute(baseRouter, convertParametersToVertx(basePath + path));
                String serviceId = serviceIdResolver.resolve(method, path, operation);
                configureRoute(route, serviceId, operation, eventBus, configureMessage);
            });
        });
        return baseRouter;
    }

    private static String getBasePath(Swagger swagger) {
        String result = swagger.getBasePath();
        if (result == null) {
            result = "";
        }

        return result;
    }

    private static void configureRoute(Route route, String serviceId, Operation operation, EventBus eventBus, Function<RoutingContext, DeliveryOptions> configureMessage) {
        Optional.ofNullable(operation.getConsumes()).ifPresent((consumes) -> {
            consumes.forEach(route::consumes);
        });
        Optional.ofNullable(operation.getProduces()).ifPresent((produces) -> {
            produces.forEach(route::produces);
        });
        route.handler((context) -> {
            try {
                JsonObject message = new JsonObject();
                operation.getParameters().forEach((parameter) -> {
                    String name = parameter.getName();
                    Object value = ((ParameterExtractor)PARAMETER_EXTRACTORS.get(parameter.getIn())).extract(name, parameter, context);
                    message.put(name, value);
                });
                DeliveryOptions deliveryOptions = configureMessage != null ? (DeliveryOptions)configureMessage.apply(context) : new DeliveryOptions();
                eventBus.send(serviceId, message, deliveryOptions, (operationResponse) -> {
                    if (operationResponse.succeeded()) {
                        if (((Message)operationResponse.result()).body() != null) {
                            vertxLogger.debug((String)((Message)operationResponse.result()).body());
                            manageHeaders(context.response(), ((Message)operationResponse.result()).headers());
                            context.response().end((String)((Message)operationResponse.result()).body());
                        } else {
                            vertxLogger.debug("Empty response sent");
                            manageHeaders(context.response(), ((Message)operationResponse.result()).headers());
                            context.response().end();
                        }
                    } else {
                        vertxLogger.error("Error processing request");
                        ErrorHelper.manageAppError((ReplyException)operationResponse.cause(), context.response());
                    }

                });
            } catch (RuntimeException var7) {
                ErrorHelper.manageBadRequestError(var7, context.response());
            }

        });
    }

    private static void manageHeaders(HttpServerResponse httpServerResponse, MultiMap messageHeaders) {
        if (messageHeaders.contains("CUSTOM_STATUS_CODE")) {
            Integer customStatusCode = Integer.valueOf(messageHeaders.get("CUSTOM_STATUS_CODE"));
            httpServerResponse.setStatusCode(customStatusCode);
            messageHeaders.remove("CUSTOM_STATUS_CODE");
        }

        if (messageHeaders.contains("CUSTOM_STATUS_MESSAGE")) {
            String customStatusMessage = messageHeaders.get("CUSTOM_STATUS_MESSAGE");
            httpServerResponse.setStatusMessage(customStatusMessage);
            messageHeaders.remove("CUSTOM_STATUS_MESSAGE");
        }

        httpServerResponse.headers().addAll(messageHeaders);
    }

    private static String convertParametersToVertx(String path) {
        Matcher pathMatcher = PATH_PARAMETERS.matcher(path);

        while(pathMatcher.find()) {
            checkParameterName(pathMatcher.group());
        }

        return pathMatcher.replaceAll(":$1");
    }

    private static void checkParameterName(String parameterPlaceholder) {
        Matcher matcher = PATH_PARAMETER_NAME.matcher(parameterPlaceholder);
        if (!matcher.matches()) {
            String parameterName = parameterPlaceholder.substring(1, parameterPlaceholder.length() - 1);
            throw new IllegalArgumentException("Illegal path parameter name: " + parameterName + ". Parameter names should only consist of alphabetic character, numeric character or underscore and follow this pattern: [A-Za-z][A-Za-z0-9_]*");
        }
    }

    static {
        ROUTE_BUILDERS.put(HttpMethod.POST, Router::post);
        ROUTE_BUILDERS.put(HttpMethod.GET, Router::get);
        ROUTE_BUILDERS.put(HttpMethod.PUT, Router::put);
        ROUTE_BUILDERS.put(HttpMethod.PATCH, Router::patch);
        ROUTE_BUILDERS.put(HttpMethod.DELETE, Router::delete);
        ROUTE_BUILDERS.put(HttpMethod.HEAD, Router::head);
        ROUTE_BUILDERS.put(HttpMethod.OPTIONS, Router::options);
        PARAMETER_EXTRACTORS.put("path", new PathParameterExtractor());
        PARAMETER_EXTRACTORS.put("query", new QueryParameterExtractor());
        PARAMETER_EXTRACTORS.put("header", new HeadersParameterExtractor());
        PARAMETER_EXTRACTORS.put("formData", new FormParameterExtractor());
        PARAMETER_EXTRACTORS.put("body", new BodyParameterExtractor());
    }

    private interface RouteBuilder {
        Route buildRoute(Router var1, String var2);
    }
}
