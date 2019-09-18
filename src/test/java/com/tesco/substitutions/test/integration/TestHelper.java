package com.tesco.substitutions.test.integration;

import static com.tesco.substitutions.infrastructure.adapter.SubsNamespaceProvider.REDIS_KEYS_SUBS_IN_CFC_NAMESPACE;
import static com.tesco.substitutions.infrastructure.adapter.SubsNamespaceProvider.REDIS_KEYS_SUBS_NAMESPACE;

import com.tesco.substitutions.application.verticle.MainStarter;
import com.tesco.substitutions.infrastructure.adapter.SubsNamespaceProvider;
import com.tesco.substitutions.infrastructure.endpoints.SubstitutionsRoutes;
import io.restassured.RestAssured;
import io.restassured.config.LogConfig;
import io.restassured.config.RestAssuredConfig;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.redis.RedisOptions;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.redis.RedisClient;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import io.vertx.ext.unit.TestContext;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import rx.Single;

public class TestHelper {

    private static final String RESTASSURED_LOG_FILENAME = "restassured.log";
    private static final String CONFIG_JSON_FILE = "config/config.json";
    private static final int NUMBER_INSTANCES = 1;
    private static final String HTTP_LOCALHOST_BASE_URI = "http://localhost";
    private static final String HTTP_PORT_KEY = "http.port";
    private static Set<String> deploymentIDs;
    private static final String TEST_DATA_TREXSUBS_RESPONSE_JSON_FILE = "testData/trexsubsResponse.json";
    private static final String CFC_STORE_ID = "1111";
    private static final String CONFIG_JSON_KEY = "config";
    private static final String REDIS_CONFIGURATION_KEY = "redisConfiguration";
    private static final String TEST_DATA_TREXSUBS_CFC_RESPONSE_JSON_FILE = "testData/trexsubsResponseCfc.json";
    private static final String TEST_DATA_SUBS_DATE_PREFIX_VALUE = "18_09_2019";

    private static Vertx vertx;

    public static void configureTestSuite(TestContext context) throws FileNotFoundException {
        ConfigureRestAssuredLog();

        final Async async = context.async();
        vertx = Vertx.vertx();

        vertx.fileSystem().readFile(CONFIG_JSON_FILE, result -> {
            if (result.succeeded()) {
                final JsonObject config = result.result().toJsonObject();

                //Multideploy class expects this structure
                config.put("verticles", config.getJsonObject("config").getJsonArray("verticles"));

                final DeploymentOptions options = new DeploymentOptions().setInstances(
                        NUMBER_INSTANCES).setConfig(config);

                configureRestAssured(options);
                vertx.deployVerticle(MainStarter.class.getName(), options,
                        ar -> {
                            if (ar.succeeded()) {
                                async.complete();
                                saveDeploymentIds(vertx.deploymentIDs());
                                loadTestDataToRedis();
                            } else {
                                context.fail(ar.cause());
                            }
                        });
            } else {
                context.fail(result.cause());
            }
        });
    }

    private static void configureRestAssured(final DeploymentOptions options) {
        RestAssured.baseURI = HTTP_LOCALHOST_BASE_URI;
        RestAssured.basePath = SubstitutionsRoutes.SUBSTITUTIONS_MODULE_BASE_PATH;
        RestAssured.port = options.getConfig().getInteger(HTTP_PORT_KEY);
    }

    private static void ConfigureRestAssuredLog() throws FileNotFoundException {
        final PrintStream fileOutPutStream = new PrintStream(new File(RESTASSURED_LOG_FILENAME));
        RestAssured.config = RestAssuredConfig.config()
                .logConfig(new LogConfig().defaultStream(fileOutPutStream));
    }

    private static void saveDeploymentIds(final Set<String> deploymentIDs) {
        TestHelper.deploymentIDs = deploymentIDs;
    }
    private static void loadTestDataToRedis() {
        insertCfcStoreId();
        insertSubsDatePrefix();
        insertMultipleSubstitutionsInRedisForTpnb();
        insertMultipleSubstitutionsInRedisForTpnbInCFCStores();
    }

    private static void insertMultipleSubstitutionsInRedisForTpnb() {
        JsonObject jsonResponse = new JsonObject(vertx.fileSystem().readFileBlocking(TEST_DATA_TREXSUBS_RESPONSE_JSON_FILE).getDelegate());
        jsonResponse.getJsonArray("substitutions").forEach(obj -> {
            JsonObject substitutionFromFile = (JsonObject) obj;
            String tpnb = substitutionFromFile.getString("tpnb");

            JsonArray subsArray = substitutionFromFile.getJsonArray("substitutes").stream().map(subTpn -> {
                JsonObject jsonObject = new JsonObject();
                jsonObject.put("subTpn", subTpn.toString());
                return jsonObject;
            }).collect(JsonArray::new, JsonArray::add, JsonArray::addAll);
            String redisKey = REDIS_KEYS_SUBS_NAMESPACE + TEST_DATA_SUBS_DATE_PREFIX_VALUE + "_" + tpnb;
            getRedisClient(vertx).rxSet(redisKey, subsArray.toString()).subscribe();
        });
    }

    private static void insertMultipleSubstitutionsInRedisForTpnbInCFCStores(){
        JsonObject jsonResponse = new JsonObject(vertx.fileSystem().readFileBlocking(TEST_DATA_TREXSUBS_CFC_RESPONSE_JSON_FILE).getDelegate());
        jsonResponse.getJsonArray("substitutions").forEach(obj -> {
            JsonObject substitutionFromFile = (JsonObject) obj;
            String tpnb = substitutionFromFile.getString("tpnb");

            JsonArray subsArray = substitutionFromFile.getJsonArray("substitutes").stream().map(subTpn -> {
                JsonObject jsonObject = new JsonObject();
                jsonObject.put("subTpn", subTpn.toString()).put("storeIds", new JsonArray(Arrays.stream(CFC_STORE_ID.split(",")).collect(
                        Collectors.toList())));
                return jsonObject;
            }).collect(JsonArray::new, JsonArray::add, JsonArray::addAll);
            String redisKey = REDIS_KEYS_SUBS_IN_CFC_NAMESPACE + TEST_DATA_SUBS_DATE_PREFIX_VALUE + "_" + tpnb;
            getRedisClient(vertx).rxSet(redisKey, subsArray.toString()).subscribe();
        });
    }

    private static void insertCfcStoreId() {
        getRedisClient(vertx).rxSet(SubsNamespaceProvider.REDIS_KEYS_CFC_STORE_IDS, CFC_STORE_ID + ",1234").subscribe();
    }
    private static void insertSubsDatePrefix() {
        getRedisClient(vertx).rxSet(SubsNamespaceProvider.REDIS_KEYS_SUBS_DATE_PREFIX, TEST_DATA_SUBS_DATE_PREFIX_VALUE).subscribe();
    }

    private static RedisClient getRedisClient(Vertx vertx) {
        JsonObject config = (vertx.fileSystem().readFileBlocking(CONFIG_JSON_FILE).toJsonObject());
        RedisOptions redisOptions = new RedisOptions(config.getJsonObject(CONFIG_JSON_KEY).getJsonObject(REDIS_CONFIGURATION_KEY));
        return RedisClient.create(vertx, redisOptions);
    }

    public static void tearDownTestSuite() {
        RestAssured.reset();
        undeployVerticles();
        waitUntilVertxContextIsClosed();
    }

    private static void undeployVerticles() {
        deploymentIDs.forEach(id -> vertx.rxUndeploy(id).toBlocking().value());
    }

    private static void waitUntilVertxContextIsClosed() {
        final Single<Void> result = vertx.rxClose();
        result.toBlocking().value();
    }
}
