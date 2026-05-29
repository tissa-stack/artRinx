package com.example.artrinx.core.network

sealed class ApiResult<out T> {
    data class Success<out T>(val data: T) : ApiResult<T>()

    sealed class Error : ApiResult<Nothing>() {
        data class Network(val cause: Throwable) : Error()
        data class Validation(val message: String) : Error()
        data class Server(val code: Int) : Error()
        data class Unknown(val cause: Throwable) : Error()
        data class Blocked(val message: String) : Error()
        data class NotFound(val message: String) : Error()
        data class Conflict(val message: String) : Error()
        data class RateLimited(val message: String, val retryAfterSeconds: Int = 60) : Error()
    }
}
