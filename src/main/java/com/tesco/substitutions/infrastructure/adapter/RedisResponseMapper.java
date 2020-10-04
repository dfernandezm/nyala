package com.tesco.substitutions.infrastructure.adapter;

import com.google.inject.Inject;
import io.vertx.core.json.JsonArray;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class RedisResponseMapper {

    public RedisResponseMapper(){

    }

    public JsonArray mapSubstitutionsResponse(JsonArray redisResponse){
        return  wrapResponseAsJsonArrayIfNeeded(redisResponse);
    }

    private JsonArray wrapResponseAsJsonArrayIfNeeded(JsonArray value){
        if (value == null) {
            log.debug("Returning empty substitutions");
            return new JsonArray();
        }
        return transformRedisResponseToJsonArray(value);
    }

    private JsonArray transformRedisResponseToJsonArray(JsonArray redisResponse){
        return redisResponse.stream()
                .map(substitution -> Optional.ofNullable(substitution).map(o -> new JsonArray(o.toString())))
                .map(o -> o.orElseGet(JsonArray::new))
                .collect(JsonArray::new, JsonArray::add, JsonArray::add);
    }
}
