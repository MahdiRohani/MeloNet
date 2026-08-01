package com.melonet.app.core.network

import com.melonet.app.core.common.AppError
import com.melonet.app.core.common.AppErrorException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class AppErrorMappingTest {

    @Test
    fun socketTimeout_mapsToTimeout() {
        assertEquals(AppError.Timeout, SocketTimeoutException().toAppError())
    }

    @Test
    fun unknownHost_mapsToNoConnection() {
        assertEquals(AppError.NoConnection, UnknownHostException().toAppError())
    }

    @Test
    fun connectException_mapsToNoConnection() {
        assertEquals(AppError.NoConnection, ConnectException().toAppError())
    }

    @Test
    fun appErrorException_unwraps() {
        val error = AppError.NotFound
        assertEquals(error, AppErrorException(error).toAppError())
    }

    @Test
    fun mapServerError_invalidCredentials() {
        val error = mapServerError("invalid_credentials", "bad login", 401)
        assertEquals(AppError.Http("invalid_credentials", 401, "bad login"), error)
    }

    @Test
    fun mapServerError_userExists() {
        val error = mapServerError("user_exists", "exists", 409)
        assertEquals(AppError.Http("user_exists", 409, "exists"), error)
    }

    @Test
    fun mapServerError_requestTimeout() {
        assertEquals(AppError.Timeout, mapServerError("request_timeout", null, 504))
    }

    @Test
    fun mapServerError_unauthorizedCode() {
        assertEquals(AppError.Unauthorized, mapServerError("unauthorized", null, 401))
    }

    @Test
    fun mapServerError_statusFallback() {
        assertEquals(AppError.Forbidden, mapServerError(null, null, 403))
        assertEquals(AppError.NotFound, mapServerError(null, null, 404))
        assertEquals(AppError.Server, mapServerError(null, null, 500))
        assertEquals(AppError.Timeout, mapServerError(null, null, 408))
    }

    @Test
    fun mapServerError_validationCodes() {
        assertTrue(mapServerError("invalid_email", null, 400) is AppError.Validation)
        assertEquals(
            AppError.Validation(code = "invalid_username"),
            mapServerError("invalid_username", null, 400),
        )
    }
}
