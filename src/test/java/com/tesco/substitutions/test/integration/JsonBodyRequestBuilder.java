package com.tesco.substitutions.test.integration;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Arrays;

import static com.tesco.substitutions.application.handler.ChannelProxyHandler.STORE_ID_PARAMETER;
import static com.tesco.substitutions.application.handler.ChannelProxyHandler.UNAVAILABLE_TPNB_PARAMETER;

public class JsonBodyRequestBuilder {

    public static String getJsonRequestBodyForTpnbs(String tpnbs) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.put(UNAVAILABLE_TPNB_PARAMETER, jsonArrayFromTpnbs(tpnbs));
        return jsonObject.toString();
    }

    public static String getJsonRequestBodyForTpnbsInStore(String tpnbs, String storeId) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.put(STORE_ID_PARAMETER, storeId);
        jsonObject.put(UNAVAILABLE_TPNB_PARAMETER, jsonArrayFromTpnbs(tpnbs));
        return jsonObject.toString();
    }

    private static JsonArray jsonArrayFromTpnbs(String tpnbs) {
        JsonArray jsonArray = new JsonArray();
        if (tpnbs.isEmpty()) return jsonArray;
        Arrays.stream(tpnbs.split(",")).forEach(jsonArray::add);
        return jsonArray;
    }

}
