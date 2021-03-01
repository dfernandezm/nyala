package com.tesco.substitutions.test.integration;

import static com.google.common.net.MediaType.JSON_UTF_8;
import static com.tesco.substitutions.application.handler.ChannelProxyHandler.BAD_REQUEST_EMPTY_BODY_MESSAGE;
import static com.tesco.substitutions.application.handler.ChannelProxyHandler.BAD_REQUEST_ERROR_MESSAGE;
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

import com.tesco.substitutions.application.handler.ChannelProxyHandler;
import io.restassured.module.jsv.JsonSchemaValidator;
import io.restassured.path.json.JsonPath;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

import org.apache.http.HttpStatus;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class SubstitutionsApiRestIT {

    private static final String TEST_DATA_TREXSUBS_RESPONSE_JSON_FILE = "testData/trexsubsResponse.json";
    private static final String TEST_DATA_TREXSUBS_CFC_RESPONSE_JSON_FILE = "testData/trexsubsResponseCfc.json";
    private static final String TREXSUBS_SUBSTITUTES_SCHEMA_JSON = "testData/trexsubsSubstitutesSchema.json";
    private static final String CFC_STORE_ID = "1111";
    private static final String NON_CFC_STORE_ID = "9999";

    @BeforeClass
    public static void beforeAllTests(final TestContext context) throws FileNotFoundException {
        TestHelper.configureTestSuite(context);
    }

    @AfterClass
    public static void tearDown() {
        TestHelper.tearDownTestSuite();
    }

    @Test
    public void a_not_found_status_code_is_returned_if_the_endpoint_path_is_incorrect() {
        given()
                .header(ACCEPT_ENCODING, JSON_UTF_8.toString())
                .log().all()
                .when()
                .post(SUBSTITUTES_PATH + "/wrongpath")
                .then()
                .log().all()
                .assertThat()
                .statusCode(HttpStatus.SC_NOT_FOUND);

    }

    @Test
    public void substitutes_endpoint_returns_empty_substitutions_for_a_tpnb_not_found() {
        given()
                .header(ACCEPT_ENCODING, JSON_UTF_8.toString())
                .body(JsonBodyRequestBuilder.getJsonRequestBodyForTpnbsInStore("11111122", NON_CFC_STORE_ID))
                .log().all()
                .when()
                .post(SUBSTITUTES_PATH)
                .then()
                .log().all()
                .assertThat()
                .contentType(JSON)
                .assertThat().body(containsString("\"substitutes\" : [ ]"))
                .statusCode(SC_OK);
    }

    @Test
    public void substitutes_endpoint_returns_empty_substitutions_for_a_tpnb_in_cfc_store_which_is_not_there(){
        given()
                .header(ACCEPT_ENCODING, JSON_UTF_8.toString())
                .body(JsonBodyRequestBuilder.getJsonRequestBodyForTpnbsInStore("11111122", CFC_STORE_ID))
                .log().all()
                .when()
                .post(SUBSTITUTES_PATH)
                .then()
                .log().all()
                .assertThat()
                .contentType(JSON)
                .assertThat().body(containsString("\"substitutes\" : [ ]"))
                .statusCode(SC_OK);
    }

    @Test
    public void substitutes_endpoint_returns_substitutions_list_for_a_tpnb_that_has_subs() {
        String tpnb = "99999999";

        given()
                .header(ACCEPT_ENCODING, JSON_UTF_8.toString())
                .body(JsonBodyRequestBuilder.getJsonRequestBodyForTpnbsInStore(tpnb, NON_CFC_STORE_ID))
                .log().all()
                .when()
                .post(SUBSTITUTES_PATH)
                .then()
                .log().all()
                .assertThat()
                .contentType(JSON)
                .assertThat().body(JsonSchemaValidator.matchesJsonSchemaInClasspath(TREXSUBS_SUBSTITUTES_SCHEMA_JSON))
                .assertThat().body("substitutions[0].substitutes", is(equalTo(readSubTpnsFromFileForGivenTpnb(tpnb))))
                .assertThat().body("substitutions.size()", is(equalTo(1)))
                .statusCode(SC_OK);
    }

    @Test
    public void substitutes_endpoint_returns_substitutions_list_for_a_tpnb_that_has_subs_in_cfc_store() {
        String tpnb = "64522828";

        given()
                .header(ACCEPT_ENCODING, JSON_UTF_8.toString())
                .body(JsonBodyRequestBuilder.getJsonRequestBodyForTpnbsInStore(tpnb, CFC_STORE_ID))
                .log().all()
                .when()
                .post(SUBSTITUTES_PATH)
                .then()
                .log().all()
                .assertThat()
                .contentType(JSON)
                .assertThat().body(JsonSchemaValidator.matchesJsonSchemaInClasspath(TREXSUBS_SUBSTITUTES_SCHEMA_JSON))
                .assertThat().body("substitutions[0].substitutes", is(equalTo(readSubTpnsFromFileForGivenTpnbInCfcStore(tpnb))))
                .assertThat().body("substitutions.size()", is(equalTo(1)))
                .statusCode(SC_OK);
    }

    @Test
    public void substitutes_endpoint_returns_substitutions_list_for_a_tpnb_that_starts_with_a_zero_and_has_subs() {
        String tpnb = "09999999";

        given()
                .header(ACCEPT_ENCODING, JSON_UTF_8.toString())
                .body(JsonBodyRequestBuilder.getJsonRequestBodyForTpnbsInStore(tpnb, NON_CFC_STORE_ID))
                .log().all()
                .when()
                .post(SUBSTITUTES_PATH)
                .then()
                .log().all()
                .assertThat()
                .contentType(JSON)
                .assertThat().body(JsonSchemaValidator.matchesJsonSchemaInClasspath(TREXSUBS_SUBSTITUTES_SCHEMA_JSON))
                .assertThat().body("substitutions[0].substitutes", is(equalTo(readSubTpnsFromFileForGivenTpnb(tpnb))))
                .assertThat().body("substitutions.size()", is(equalTo(1)))
                .statusCode(SC_OK);
    }

    @Test
    public void substitutes_endpoint_returns_substitutions_list_for_a_tpnb_that_starts_with_a_zero_and_has_subs_in_cfc_store() {
        String tpnb = "01111111";

        given()
                .header(ACCEPT_ENCODING, JSON_UTF_8.toString())
                .body(JsonBodyRequestBuilder.getJsonRequestBodyForTpnbsInStore(tpnb, CFC_STORE_ID))
                .log().all()
                .when()
                .post(SUBSTITUTES_PATH)
                .then()
                .log().all()
                .assertThat()
                .contentType(JSON)
                .assertThat().body(JsonSchemaValidator.matchesJsonSchemaInClasspath(TREXSUBS_SUBSTITUTES_SCHEMA_JSON))
                .assertThat().body("substitutions[0].substitutes", is(equalTo(readSubTpnsFromFileForGivenTpnbInCfcStore(tpnb))))
                .assertThat().body("substitutions.size()", is(equalTo(1)))
                .statusCode(SC_OK);
    }

    @Test
    public void substitutes_endpoint_should_return_substitutions_list_for_multiple_tpnbs_that_have_subs() {
        String tpnbs = "55555555,66666666";

        given()
                .header(ACCEPT_ENCODING, JSON_UTF_8.toString())
                .body(JsonBodyRequestBuilder.getJsonRequestBodyForTpnbsInStore(tpnbs, NON_CFC_STORE_ID))
                .log().all()
                .when()
                .post(SUBSTITUTES_PATH)
                .then()
                .log().all()
                .assertThat()
                .contentType(JSON)
                .assertThat().body(JsonSchemaValidator.matchesJsonSchemaInClasspath(TREXSUBS_SUBSTITUTES_SCHEMA_JSON))
                .assertThat().body("substitutions[0].substitutes", is(equalTo(readSubTpnsFromFileForGivenTpnb("55555555"))))
                .assertThat().body("substitutions[1].substitutes", is(equalTo(readSubTpnsFromFileForGivenTpnb("66666666"))))
                .assertThat().body("substitutions.size()", is(equalTo(2)))
                .statusCode(SC_OK);
    }

    @Test
    public void substitutes_endpoint_should_return_substitutions_list_for_multiple_tpnbs_that_have_subs_in_cfc_stores() {
        String tpnbs = "64522828,80644752";

        given()
                .header(ACCEPT_ENCODING, JSON_UTF_8.toString())
                .body(JsonBodyRequestBuilder.getJsonRequestBodyForTpnbsInStore(tpnbs, CFC_STORE_ID))
                .log().all()
                .when()
                .post(SUBSTITUTES_PATH)
                .then()
                .log().all()
                .assertThat()
                .contentType(JSON)
                .assertThat().body(JsonSchemaValidator.matchesJsonSchemaInClasspath(TREXSUBS_SUBSTITUTES_SCHEMA_JSON))
                .assertThat().body("substitutions[0].substitutes", is(equalTo(readSubTpnsFromFileForGivenTpnbInCfcStore("64522828"))))
                .assertThat().body("substitutions[1].substitutes", is(equalTo(readSubTpnsFromFileForGivenTpnbInCfcStore("80644752"))))
                .assertThat().body("substitutions.size()", is(equalTo(2)))
                .statusCode(SC_OK);
    }

    @Test
    public void substitutes_endpoint_should_return_substitutions_list_and_for_multiple_tpnbs_with_no_storeId() {
        String tpnbs = "55555555,66666666";

        given()
                .header(ACCEPT_ENCODING, JSON_UTF_8.toString())
                .body(JsonBodyRequestBuilder.getJsonRequestBodyForTpnbs(tpnbs))
                .log().all()
                .when()
                .post(SUBSTITUTES_PATH)
                .then()
                .log().all()
                .assertThat()
                .contentType(JSON)
                .header(ChannelProxyHandler.REQUEST_BODY_ERROR_HEADER, equalTo("No Store ID passed"))
                .assertThat().body(JsonSchemaValidator.matchesJsonSchemaInClasspath(TREXSUBS_SUBSTITUTES_SCHEMA_JSON))
                .assertThat().body("substitutions[0].substitutes", is(equalTo(readSubTpnsFromFileForGivenTpnb("55555555"))))
                .assertThat().body("substitutions[1].substitutes", is(equalTo(readSubTpnsFromFileForGivenTpnb("66666666"))))
                .assertThat().body("substitutions.size()", is(equalTo(2)))
                .statusCode(SC_OK);
    }

    @Test
    public void substitutes_endpoint_should_return_empty_list_for_any_tpnb_that_has_no_subs_when_multiple_subs_requested() {
        String tpnbString = "55555555,11122233,66666666";

        given()
                .header(ACCEPT_ENCODING, JSON_UTF_8.toString())
                .body(JsonBodyRequestBuilder.getJsonRequestBodyForTpnbsInStore(tpnbString, NON_CFC_STORE_ID))
                .log().all()
                .when()
                .post(SUBSTITUTES_PATH)
                .then()
                .log().all()
                .assertThat()
                .contentType(JSON)
                .assertThat().body(JsonSchemaValidator.matchesJsonSchemaInClasspath(TREXSUBS_SUBSTITUTES_SCHEMA_JSON))
                .assertThat().body("substitutions.tpnb", hasItems("55555555", "11122233","66666666"))
                .assertThat().body("substitutions[0].substitutes", is(equalTo(readSubTpnsFromFileForGivenTpnb("55555555"))))
                .assertThat().body("substitutions[1].substitutes", is(Collections.emptyList()))
                .assertThat().body("substitutions[2].substitutes", is(equalTo(readSubTpnsFromFileForGivenTpnb("66666666"))))
                .statusCode(SC_OK);
    }

    @Test
    public void substitutes_endpoint_should_return_empty_list_for_any_tpnb_that_has_no_subs_when_multiple_subs_requested_for_cfc_store() {
        String tpnbString = "64522828,11122233,80644752";

        given()
                .header(ACCEPT_ENCODING, JSON_UTF_8.toString())
                .body(JsonBodyRequestBuilder.getJsonRequestBodyForTpnbsInStore(tpnbString, CFC_STORE_ID))
                .log().all()
                .when()
                .post(SUBSTITUTES_PATH)
                .then()
                .log().all()
                .assertThat()
                .contentType(JSON)
                .assertThat().body(JsonSchemaValidator.matchesJsonSchemaInClasspath(TREXSUBS_SUBSTITUTES_SCHEMA_JSON))
                .assertThat().body("substitutions.tpnb", hasItems("64522828", "11122233","80644752"))
                .assertThat().body("substitutions[0].substitutes", is(equalTo(readSubTpnsFromFileForGivenTpnbInCfcStore("64522828"))))
                .assertThat().body("substitutions[1].substitutes", is(Collections.emptyList()))
                .assertThat().body("substitutions[2].substitutes", is(equalTo(readSubTpnsFromFileForGivenTpnbInCfcStore("80644752"))))
                .statusCode(SC_OK);
    }

    @Test
    public void a_bad_request_status_code_and_custom_message_should_return_if_no_tpnbs_have_been_passed_as_parameters_for_subs() {
        given()
                .header(ACCEPT_ENCODING, JSON_UTF_8.toString())
                .body(JsonBodyRequestBuilder.getJsonRequestBodyForTpnbsInStore("", NON_CFC_STORE_ID))
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
    public void a_bad_request_status_code_and_custom_message_should_return_if_no_tpnbs_have_been_passed_as_parameters_for_subs_in_CFC_store() {
        given()
                .header(ACCEPT_ENCODING, JSON_UTF_8.toString())
                .body(JsonBodyRequestBuilder.getJsonRequestBodyForTpnbsInStore("", CFC_STORE_ID))
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
    public void a_bad_request_status_code_and_custom_message_is_returned_if_no_tpnb_has_been_passed_as_parameter_and_no_store_id_has_been_passed() {
        given()
                .header(ACCEPT_ENCODING, JSON_UTF_8.toString())
                .body(JsonBodyRequestBuilder.getJsonRequestBodyForTpnbs(""))
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
    public void a_bad_request_status_code_and_custom_message_should_return_if_any_of_tpnbs_passed_are_not_formatted_as_a_long_number() {
        given()
                .header(ACCEPT_ENCODING, JSON_UTF_8.toString())
                .body(JsonBodyRequestBuilder.getJsonRequestBodyForTpnbsInStore("64522828,abcdef12", NON_CFC_STORE_ID))
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
    public void a_bad_request_status_code_and_custom_message_should_return_if_any_of_tpnbs_passed_are_not_formatted_as_a_long_number_for_cfc_store() {
        given()
                .header(ACCEPT_ENCODING, JSON_UTF_8.toString())
                .body(JsonBodyRequestBuilder.getJsonRequestBodyForTpnbsInStore("64522828,abcdef12", CFC_STORE_ID))
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
    public void a_bad_request_status_code_and_custom_message_should_return_if_any_of_tpnbs_passed_are_not_formatted_as_a_long_number_with_no_store_id() {
        given()
                .header(ACCEPT_ENCODING, JSON_UTF_8.toString())
                .body(JsonBodyRequestBuilder.getJsonRequestBodyForTpnbs("64522828,abcdef12"))
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
    public void a_bad_request_status_code_is_returned_and_custom_message_if_store_id_is_not_formatted_as_a_four_digit_number() {
        given()
                .header(ACCEPT_ENCODING, JSON_UTF_8.toString())
                .body(JsonBodyRequestBuilder.getJsonRequestBodyForTpnbsInStore("64522828,80644752", "12"))
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

    private File getFileFromClasspath(String pathToSchemaInClasspath) {
        return new File(Thread.currentThread().getContextClassLoader().getResource(pathToSchemaInClasspath).getFile());
    }

    private List<String> readSubTpnsFromFileForGivenTpnb(String tpnb) {
        return new JsonPath(getFileFromClasspath(TEST_DATA_TREXSUBS_RESPONSE_JSON_FILE))
                .get("substitutions.find { it.tpnb == '" + tpnb + "'}.substitutes");
    }

    private List<String> readSubTpnsFromFileForGivenTpnbInCfcStore(String tpnb) {
        return new JsonPath(getFileFromClasspath(TEST_DATA_TREXSUBS_CFC_RESPONSE_JSON_FILE))
                .get("substitutions.find { it.tpnb == '" + tpnb + "'}.substitutes");
    }
}
