package com.tesco.substitutions.test.integration;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.StringContains.containsString;

import com.google.common.net.MediaType;
import com.tesco.substitutions.application.handler.SubsHandler;
import com.tesco.substitutions.application.verticle.MainStarter;
import com.tesco.substitutions.infrastructure.endpoints.StatusEndpointDefinition;
import com.tesco.substitutions.infrastructure.endpoints.SubsEndpointDefinition;
import com.tesco.substitutions.infrastructure.endpoints.SubstitutionsRoutes;
import io.restassured.RestAssured;
import io.restassured.config.LogConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.module.jsv.JsonSchemaValidator;
import io.restassured.path.json.JsonPath;
import io.vertx.core.DeploymentOptions;
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
import java.util.Map;
import java.util.Set;
import org.apache.http.HttpHeaders;
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
    private static final String TREXSUBS_SUBSTITUTES_SCHEMA_JSON = "testData/trexsubsSubstitutesSchema.json";
    private static final String CONFIG_JSON_KEY = "config";
    private static Vertx v;
    private static Set<String> deploymentIDs;


    //TODO refactor once we do the refactor of the tests code. Duplicate code.
    @BeforeClass
    public static void beforeAllTests(final TestContext context) throws FileNotFoundException {

        ConfigureRestAssuredLog();

        final Async async = context.async();
        v = Vertx.vertx();

        v.fileSystem().readFile(CONFIG_JSON_FILE, result -> {
            if (result.succeeded()) {
                final JsonObject config = result.result().toJsonObject();

                //Multideploy class expects this structure
                config.put("verticles", config.getJsonObject("config").getJsonArray("verticles"));

                final DeploymentOptions options = new DeploymentOptions().setInstances(
                        NUMBER_INSTANCES).setConfig(config);

                configureRestAssured(options);
                v.deployVerticle(MainStarter.class.getName(), options,
                        ar -> {
                            if (ar.succeeded()) {
                                async.complete();
                                saveDeplomentIds(v.deploymentIDs());

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

    private  static RedisClient getRedisClient(Vertx vertx){
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
        deploymentIDs.stream().forEach(id -> {
            v.rxUndeploy(id).toBlocking().value();
        });
    }

    private static void waitUntilVertxContextIsClosed() {
        final Single<Void> result = v.rxClose();
        result.toBlocking().value();
    }

    //TODO duplicate from SubstitutionsStatus, do we want to keep this here and remove the other class? If we can extract the common test code, probably leave it in status test
    @Test
    public void status_endpoint_returns_alive_message() {
        given().
                header(HttpHeaders.ACCEPT_ENCODING, MediaType.JSON_UTF_8.toString()).
                log().all().
                when().
                get(StatusEndpointDefinition.STATUS_PATH).
                then().
                log().all().
                assertThat().
                statusCode(HttpStatus.SC_OK).
                contentType(ContentType.JSON).
                body("message", equalTo("alive"));
    }

    @Test
    public void substitutes_endpoint_returns_empty_substitutions_for_a_tpnb_not_found() {
        given().
                header(HttpHeaders.ACCEPT_ENCODING, MediaType.JSON_UTF_8.toString()).
                queryParam(SubsHandler.TPNB_UNAVAILABLE_PRODUCT_PARAMETER, "11111122").
                log().all().
                when().
                get(SubsEndpointDefinition.SUBSTITUTES_PATH).
                then().
                log().all().
                assertThat().
                contentType(ContentType.JSON).
                assertThat().body(containsString("\"substitutions\" : [ ]")).
                statusCode(HttpStatus.SC_OK);
    }

    @Test
    public void substitutes_endpoint_returns_subs_candidates_list_for_a_tpnb_that_has_subs() {

        String tpnb = "99999999";
        insertSubstitutionsInRedisForTpnb(tpnb);
        given().
                header(HttpHeaders.ACCEPT_ENCODING, MediaType.JSON_UTF_8.toString()).
                queryParam(SubsHandler.TPNB_UNAVAILABLE_PRODUCT_PARAMETER, tpnb).
                log().all().
                when().
                get(SubsEndpointDefinition.SUBSTITUTES_PATH).
                then().
                log().all().
                assertThat().
                contentType(ContentType.JSON).
                assertThat().body(JsonSchemaValidator.matchesJsonSchemaInClasspath(TREXSUBS_SUBSTITUTES_SCHEMA_JSON)).
                assertThat().body("", equalTo(getExpectedFullJsonResponse())).
                statusCode(HttpStatus.SC_OK);

    }

    @Test
    public void a_bad_request_status_code_and_custom_message_is_returned_if_no_tpnb_has_been_passed_as_parameter() {

        given().
                header(HttpHeaders.ACCEPT_ENCODING, MediaType.JSON_UTF_8.toString()).
                log().all().
                when().
                get(SubsEndpointDefinition.SUBSTITUTES_PATH).
                then().
                log().all().
                assertThat().
                contentType(ContentType.JSON).
                statusLine(containsString(SubsHandler.BAD_REQUEST_ERROR_MESSAGE)).
                statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    public void a_bad_request_status_code_is_returned_and_custom_message_if_tpnb_has_been_passed_as_parameter_but_it_is_not_formatted_as_a_long_number() {

        given().
                header(HttpHeaders.ACCEPT_ENCODING, MediaType.JSON_UTF_8.toString()).
                log().all().
                when().
                get(SubsEndpointDefinition.SUBSTITUTES_PATH).
                then().
                log().all().
                assertThat().
                contentType(ContentType.JSON).
                statusLine(containsString(SubsHandler.BAD_REQUEST_ERROR_MESSAGE)).
                statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    public void a_not_found_status_code_is_returned_if_the_endpoint_path_is_incorrect() {

        given().
                header(HttpHeaders.ACCEPT_ENCODING, MediaType.JSON_UTF_8.toString()).
                log().all().
                when().
                get(SubsEndpointDefinition.SUBSTITUTES_PATH + "/wrongpath").
                then().
                log().all().
                assertThat().
                statusCode(HttpStatus.SC_NOT_FOUND);

    }

    private void insertSubstitutionsInRedisForTpnb(String tpnb) {
        RedisClient client = getRedisClient(v);
        //TODO read tpnbs subs from the response json file
        client.rxSet(tpnb, "11111111,2222222").subscribe();
    }

    private Map<Object, Object> getExpectedFullJsonResponse() {
        return new JsonPath(getFileFromClasspath(TEST_DATA_TREXSUBS_RESPONSE_JSON_FILE)).getMap("");
    }

    private File getFileFromClasspath(String pathToSchemaInClasspath) {
        return new File(Thread.currentThread().getContextClassLoader().getResource(pathToSchemaInClasspath).getFile());
}
}
