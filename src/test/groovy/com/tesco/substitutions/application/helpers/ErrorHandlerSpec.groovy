package com.tesco.substitutions.application.helpers

import com.tesco.personalisation.commons.errorhandling.ApiErrorException
import com.tesco.personalisation.commons.errorhandling.UnAuthorizedException
import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.json.DecodeException
import io.vertx.rxjava.core.http.HttpServerResponse
import spock.lang.Specification

import java.util.concurrent.TimeoutException


class ErrorHandlerSpec extends Specification {

    def response, chainedResponse

    def setup() {
        response = Mock(HttpServerResponse.class)
        chainedResponse = Mock(HttpServerResponse.class)
    }

    def "if an Exception happens, the status code and the exception message should be set in the response following some rules. Any exception that is not an ApiErrorException, DecodeException or TimeoutException will behave with the same default"() {
        when:
        ErrorHandler.prepareErrorResponse(response, cause);

        then:
        1 * response.setStatusCode(expectedStatus.code()) >> chainedResponse
        1 * chainedResponse.setStatusMessage(expectedMessage) >> chainedResponse
        1 * chainedResponse.end(_ as String)

        where: "the exceptions that trigger the error match the following expected code and exception message in the response"

        cause                                                                                      ||          expectedStatus                       |           expectedMessage
        new ApiErrorException( HttpResponseStatus.BAD_GATEWAY, "ApiErrorException Error Message" ) ||      HttpResponseStatus.BAD_GATEWAY           |           "ApiErrorException Error Message"
        new DecodeException("DecodeException Error Message")                                       ||      HttpResponseStatus.BAD_REQUEST           |           "DecodeException Error Message"
        new TimeoutException("My message exception that will be ignored")                          ||      HttpResponseStatus.INTERNAL_SERVER_ERROR |           "Connecting timeout"
        new RuntimeException("My custom exception Message")                                        ||      HttpResponseStatus.INTERNAL_SERVER_ERROR |           "My custom exception Message"
        new UnAuthorizedException("My custom UnAuthorized exception message")                      ||      HttpResponseStatus.FORBIDDEN             |           "My custom UnAuthorized exception message"
    }

}
