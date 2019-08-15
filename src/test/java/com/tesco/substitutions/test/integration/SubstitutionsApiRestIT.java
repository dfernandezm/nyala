package com.tesco.substitutions.test.integration;

import static com.google.common.net.MediaType.JSON_UTF_8;
import static com.tesco.substitutions.application.handler.SubsHandler.BAD_REQUEST_EMPTY_BODY_MESSAGE;
import static com.tesco.substitutions.application.handler.SubsHandler.BAD_REQUEST_ERROR_MESSAGE;
import static com.tesco.substitutions.application.handler.SubsHandler.UNAVAILABLE_MULTIPLE_TPNBS_PARAMETER;
import static com.tesco.substitutions.application.handler.SubsHandler.UNAVAILABLE_TPNB_PARAMETER;
import static com.tesco.substitutions.infrastructure.endpoints.SubsEndpointDefinition.SUBSTITUTES_PATH;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.apache.http.HttpHeaders.ACCEPT_ENCODING;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.StringContains.containsString;

import com.google.common.collect.Lists;
import com.tesco.substitutions.application.handler.SubsHandler;
import com.tesco.substitutions.application.verticle.MainStarter;
import com.tesco.substitutions.infrastructure.adapter.SubstitutesRedisService;
import com.tesco.substitutions.infrastructure.endpoints.SubstitutionsRoutes;
import io.restassured.RestAssured;
import io.restassured.config.LogConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.module.jsv.JsonSchemaValidator;
import io.restassured.path.json.JsonPath;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.redis.RedisOptions;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.redis.RedisClient;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.http.HttpStatus;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import rx.Single;

@RunWith(VertxUnitRunner.class)
public class SubstitutionsApiRestIT {

    private static final String RESTASSURED_LOG_FILENAME = "restassured.log";
    private static final String CONFIG_JSON_FILE = "config/config.json";
    private static final int NUMBER_INSTANCES = 1;
    private static final String HTTP_LOCALHOST_BASE_URI = "http://localhost";
    private static final String HTTP_PORT_KEY = "http.port";
    private static final String REDIS_CONFIGURATION_KEY = "redisConfiguration";
    private static final String TEST_DATA_TREXSUBS_RESPONSE_JSON_FILE = "testData/trexsubsResponse.json";
    private static final String TEST_DATA_TREXSUBS_BULK_RESPONSE_JSON_FILE = "testData/trexsubsBulkResponse.json";
    private static final String TREXSUBS_SUBSTITUTES_SCHEMA_JSON = "testData/trexsubsSubstitutesSchema.json";
    private static final String TREXSUBS_BULK_SUBSTITUTES_SCHEMA_JSON = "testData/trexsubsBulkSubstitutesSchema.json";
    private static final String CONFIG_JSON_KEY = "config";
    private static Vertx vertx;
    private static Set<String> deploymentIDs;


    //TODO refactor once we do the refactor of the tests code. Duplicate code.
    @BeforeClass
    public static void beforeAllTests(final TestContext context) throws FileNotFoundException {

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
                                saveDeplomentIds(vertx.deploymentIDs());
                            } else {
                                context.fail(ar.cause());
                            }
                        });
            } else {
                context.fail(result.cause());
            }
        });
    }

    private static void saveDeplomentIds(final Set<String> deploymentIDs) {
        SubstitutionsApiRestIT.deploymentIDs = deploymentIDs;
    }

    private static void ConfigureRestAssuredLog() throws FileNotFoundException {
        final PrintStream fileOutPutStream = new PrintStream(new File(RESTASSURED_LOG_FILENAME));
        RestAssured.config = RestAssuredConfig.config()
                .logConfig(new LogConfig().defaultStream(fileOutPutStream));
    }

    private static void configureRestAssured(final DeploymentOptions options) {
        RestAssured.baseURI = HTTP_LOCALHOST_BASE_URI;
        RestAssured.basePath = SubstitutionsRoutes.SUBSTITUTIONS_MODULE_BASE_PATH;
        RestAssured.port = options.getConfig().getInteger(HTTP_PORT_KEY);
    }

    private static RedisClient getRedisClient(Vertx vertx) {
        JsonObject config = (vertx.fileSystem().readFileBlocking(CONFIG_JSON_FILE).toJsonObject());
        RedisOptions redisOptions = new RedisOptions(config.getJsonObject(CONFIG_JSON_KEY).getJsonObject(REDIS_CONFIGURATION_KEY));
        return RedisClient.create(vertx, redisOptions);
    }


    @AfterClass
    public static void tearDown() {
        RestAssured.reset();
        undeployVerticles();
        waitUntilVertxContextIsClosed();
    }

    private static void undeployVerticles() {
        deploymentIDs.stream().forEach(id -> vertx.rxUndeploy(id).toBlocking().value());
    }

    private static void waitUntilVertxContextIsClosed() {
        final Single<Void> result = vertx.rxClose();
        result.toBlocking().value();
    }

    @Test
    public void substitutes_endpoint_returns_empty_substitutions_for_a_tpnb_not_found() {
        given()
                .header(ACCEPT_ENCODING, JSON_UTF_8.toString())
                .queryParam(UNAVAILABLE_TPNB_PARAMETER, "11111122")
                .log().all()
                .when()
                .get(SUBSTITUTES_PATH)
                .then()
                .log().all()
                .assertThat()
                .contentType(JSON)
                .assertThat().body(containsString("\"substitutions\" : [ ]"))
                .statusCode(SC_OK);
    }

    @Test
    public void substitutes_endpoint_returns_substitutions_list_for_a_tpnb_that_has_subs() {

        String tpnb = "99999999";
        insertSubstitutionsInRedisForTpnb(tpnb);
        given()
                .header(ACCEPT_ENCODING, JSON_UTF_8.toString())
                .queryParam(UNAVAILABLE_TPNB_PARAMETER, tpnb)
                .log().all()
                .when()
                .get(SUBSTITUTES_PATH)
                .then()
                .log().all()
                .assertThat()
                .contentType(JSON)
                .assertThat().body(JsonSchemaValidator.matchesJsonSchemaInClasspath(TREXSUBS_SUBSTITUTES_SCHEMA_JSON))
                .assertThat().body("", equalTo(getExpectedFullJsonResponse()))
                .statusCode(SC_OK);
    }

    @Test
    public void substitutes_endpoint_returns_substitutions_list_for_a_tpnb_that_starts_with_a_zero_and_has_subs() {

        String tpnb = "09999999";
        insertSubstitutionsInRedisForTpnb(tpnb);
        given()
                .header(ACCEPT_ENCODING, JSON_UTF_8.toString())
                .queryParam(UNAVAILABLE_TPNB_PARAMETER, tpnb)
                .log().all()
                .when()
                .get(SUBSTITUTES_PATH)
                .then()
                .log().all()
                .assertThat()
                .contentType(JSON)
                .assertThat().body(JsonSchemaValidator.matchesJsonSchemaInClasspath(TREXSUBS_SUBSTITUTES_SCHEMA_JSON))
                .assertThat().body("", equalTo(getExpectedFullJsonResponse()))
                .statusCode(SC_OK);

    }

    @Test
    public void a_bad_request_status_code_and_custom_message_is_returned_if_no_tpnb_has_been_passed_as_parameter() {
        given()
                .header(ACCEPT_ENCODING, JSON_UTF_8.toString())
                .log().all()
                .when()
                .get(SUBSTITUTES_PATH)
                .then()
                .log().all()
                .assertThat()
                .contentType(JSON)
                .statusLine(containsString(BAD_REQUEST_ERROR_MESSAGE))
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void a_bad_request_status_code_is_returned_and_custom_message_if_tpnb_has_been_passed_as_parameter_but_it_is_not_formatted_as_a_long_number() {
        given()
                .header(ACCEPT_ENCODING, JSON_UTF_8.toString())
                .queryParam(UNAVAILABLE_TPNB_PARAMETER, "abcdef12")
                .log().all()
                .when()
                .get(SUBSTITUTES_PATH)
                .then()
                .log().all()
                .assertThat()
                .contentType(JSON)
                .statusLine(containsString(BAD_REQUEST_ERROR_MESSAGE))
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void a_not_found_status_code_is_returned_if_the_endpoint_path_is_incorrect() {
        given()
                .header(ACCEPT_ENCODING, JSON_UTF_8.toString())
                .log().all()
                .when()
                .get(SUBSTITUTES_PATH + "/wrongpath")
                .then()
                .log().all()
                .assertThat()
                .statusCode(HttpStatus.SC_NOT_FOUND);

    }

    @Test
    public void substitutes_endpoint_should_return_substitutions_list_for_multiple_tpnbs_that_have_subs() {
        String tpnbs = "64522828,80644752";
        insertMultipleSubstitutionsInRedisForTpnb(tpnbs);

        given()
                .header(ACCEPT_ENCODING, JSON_UTF_8.toString())
                .body(getJsonBodyForTpnbs(tpnbs))
                .log().all()
                .when()
                .post(SUBSTITUTES_PATH)
                .then()
                .log().all()
                .assertThat()
                .contentType(JSON)
                .assertThat().body(JsonSchemaValidator.matchesJsonSchemaInClasspath(TREXSUBS_BULK_SUBSTITUTES_SCHEMA_JSON))
                .assertThat().body("", equalTo(getExpectedFullJsonBulkResponse()))
                .statusCode(SC_OK);
    }

    @Test
    public void substitutes_endpoint_should_return_empty_list_for_any_tpnb_that_has_no_subs_when_miultiple_subs_requested() {
        String nonPresentTpnb = "64522828,11122233";
        insertMultipleSubstitutionsInRedisForTpnb(nonPresentTpnb);

        given()
                .header(ACCEPT_ENCODING, JSON_UTF_8.toString())
                .body(getJsonBodyForTpnbs(nonPresentTpnb))
                .log().all()
                .when()
                .post(SUBSTITUTES_PATH)
                .then()
                .log().all()
                .assertThat()
                .contentType(JSON)
                .assertThat().body(JsonSchemaValidator.matchesJsonSchemaInClasspath(TREXSUBS_BULK_SUBSTITUTES_SCHEMA_JSON))
                .assertThat().body("substitutions.tpnb", hasItems("64522828", "11122233"))
                .assertThat().body("substitutions[0].substitutes", is(readSubTpnsFromFileForGiveTpnb("64522828")))
                .assertThat().body("substitutions[1].substitutes", is(Collections.emptyList()))
                .statusCode(SC_OK);
    }

    @Test
    public void a_bad_request_status_code_and_custom_message_should_return_if_no_tpnbs_have_been_passed_as_parameters_for_bulk_subs() {
        given()
                .header(ACCEPT_ENCODING, JSON_UTF_8.toString())
                .body(getJsonBodyForTpnbs(""))
                .log().all()
                .when()
                .post(SUBSTITUTES_PATH)
                .then()
                .log().all()
                .assertThat()
                .contentType(JSON)
                .statusLine(containsString(BAD_REQUEST_ERROR_MESSAGE))
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void a_bad_request_status_code_and_custom_message_should_return_if_empty_body_has_been_passed(){
        given()
                .header(ACCEPT_ENCODING, JSON_UTF_8.toString())
                .log().all()
                .when()
                .post(SUBSTITUTES_PATH)
                .then()
                .log().all()
                .assertThat()
                .contentType(JSON)
                .statusLine(containsString(BAD_REQUEST_EMPTY_BODY_MESSAGE))
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void a_bad_request_status_code_and_custom_message_should_return_if_no_if_any_of_tpnbs_passed_are_not_formatted_as_a_long_number() {
        insertMultipleSubstitutionsInRedisForTpnb("64522828");
        given()
                .header(ACCEPT_ENCODING, JSON_UTF_8.toString())
                .body(getJsonBodyForTpnbs("64522828,abcdef12"))
                .log().all()
                .when()
                .post(SUBSTITUTES_PATH)
                .then()
                .log().all()
                .assertThat()
                .contentType(JSON)
                .statusLine(containsString(BAD_REQUEST_ERROR_MESSAGE))
                .statusCode(SC_BAD_REQUEST);
    }

    private String getJsonBodyForTpnbs(String tpnbs) {
        return new JsonObject().put(UNAVAILABLE_MULTIPLE_TPNBS_PARAMETER, new JsonArray(Lists.newArrayList(tpnbs.split(",")))).toString();
    }

    private void insertSubstitutionsInRedisForTpnb(String tpnb) {
        RedisClient client = getRedisClient(vertx);
        //TODO read tpnbs subs from the response json file
        client.rxSet(SubstitutesRedisService.REDIS_KEYS_SUBS_NAMESPACE + tpnb, "11111111,2222222").subscribe();
    }

    //only tpnbs from both json file and 'tpnbs' parameter go to redis
    private void insertMultipleSubstitutionsInRedisForTpnb(String tpnbs) {
        JsonObject jsonResponse = new JsonObject(
                vertx.fileSystem().readFileBlocking(TEST_DATA_TREXSUBS_BULK_RESPONSE_JSON_FILE).getDelegate());
        jsonResponse.getJsonArray("substitutions").forEach(obj -> {
            JsonObject substitution = (JsonObject) obj;
            String tpnb = substitution.getString("tpnb");
            if (!tpnbs.contains(tpnb)) {
                return;
            }
            getRedisClient(vertx).rxSet(SubstitutesRedisService.REDIS_KEYS_SUBS_NAMESPACE + tpnb,
                    removeQuotesFromSubTpnbs(substitution.getJsonArray("substitutes"))).subscribe();
        });
    }

    private String removeQuotesFromSubTpnbs(JsonArray jsonArray) {
        return jsonArray.stream()
                .map(object -> ((String) object).replace("\"", ""))
                .map(String::trim)
                .collect(Collectors.toList())
                .toString().replace("[", "").replace("]", "");
    }

    private Map<Object, Object> getExpectedFullJsonResponse() {
        return new JsonPath(getFileFromClasspath(TEST_DATA_TREXSUBS_RESPONSE_JSON_FILE)).getMap("");
    }

    private Object getExpectedFullJsonBulkResponse() {
        return new JsonPath(getFileFromClasspath(TEST_DATA_TREXSUBS_BULK_RESPONSE_JSON_FILE)).getMap("");
    }

    private File getFileFromClasspath(String pathToSchemaInClasspath) {
        return new File(Thread.currentThread().getContextClassLoader().getResource(pathToSchemaInClasspath).getFile());
    }

    private List<String> readSubTpnsFromFileForGiveTpnb(String tpnb) {
        return new JsonPath(getFileFromClasspath(TEST_DATA_TREXSUBS_BULK_RESPONSE_JSON_FILE))
                .get("substitutions.find { it.tpnb == '" + tpnb + "'}.substitutes");
    }
}
