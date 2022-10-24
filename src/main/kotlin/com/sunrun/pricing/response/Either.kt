package com.sunrun.pricing.aws_common.response

import javax.ws.rs.Produces

@Produces("application/json")
sealed class Either<A, B> {
    @Produces("application/json")
    class Left<A, B>(val left: A) : Either<A, B>()

    @Produces("application/json")
    class Right<A, B>(val right: B) : Either<A, B>()
}

@Produces("application/json")
sealed class Result<out T> {
    @Produces("application/json")
    data class Error(val message: String?, val e: Exception?) : Result<Nothing>()

    @Produces("application/json")
    data class Success<T>(val value: T) : Result<T>()
}
