package com.fazula.server.common.swagger.extractors;

import com.github.phiz71.vertx.swagger.router.extractors.HeaderParameterExtractor;
import io.swagger.models.parameters.HeaderParameter;
import io.swagger.models.parameters.Parameter;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.RoutingContext;

public class HeadersParameterExtractor extends HeaderParameterExtractor {

    public Object extract(String name, Parameter parameter, RoutingContext context) {
        HeaderParameter headerParam = (HeaderParameter)parameter;
        MultiMap params = context.request().headers();
        if (headerParam.getEnum() != null && !headerParam.getEnum().contains(params.get(name))) {
            throw new IllegalArgumentException("Invalid header value " + params.get(name) + " valid values " + headerParam.getEnum());
        } else {
            return super.extract(name, parameter, context);
        }
    }
}
