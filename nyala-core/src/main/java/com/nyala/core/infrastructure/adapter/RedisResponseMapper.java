package com.nyala.core.infrastructure.adapter;

import io.vertx.core.json.JsonArray;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Singleton;
import java.util.Optional;

@Slf4j
@Singleton
public class RedisResponseMapper {

    private JsonArray wrapResponseAsJsonArrayIfNeeded(JsonArray value){
        if (value == null) {
            log.debug("Returning empty substitutions");
            return new JsonArray();
        }
        return transformRedisResponseToJsonArray(value);
    }

    private JsonArray transformRedisResponseToJsonArray(JsonArray redisResponse){
        return redisResponse.stream()
                .map(result -> Optional.ofNullable(result).map(o -> new JsonArray(o.toString())))
                .map(o -> o.orElseGet(JsonArray::new))
                .collect(JsonArray::new, JsonArray::add, JsonArray::add);
    }
}
