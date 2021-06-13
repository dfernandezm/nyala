package com.nyala.common.errorhandling

import io.netty.handler.codec.http.HttpResponseStatus

open class ApiErrorException : RuntimeException {

    var responseStatus: HttpResponseStatus

    constructor(resultCode: HttpResponseStatus, message: String) : super(message) {
        responseStatus = resultCode
    }

    constructor(responseStatus: HttpResponseStatus, message: String, cause: Throwable?) : super(message, cause) {
        this.responseStatus = responseStatus
    }

    val statusCode: Int
        get() = responseStatus.code()

    val statusAsString: String
        get() = responseStatus.codeAsText().toString()

    companion object {
        private const val serialVersionUID = 1L
        fun create(responseStatus: HttpResponseStatus, message: String): ApiErrorException {
            return ApiErrorException(responseStatus, message)
        }

        fun create(responseStatus: HttpResponseStatus, message: String, cause: Throwable?): ApiErrorException {
            return ApiErrorException(responseStatus, message, cause)
        }
    }
}