@file:UseSerializers(BigDecimalSerializer::class)

package com.sunrun.pricing.rest

import com.sunrun.pricing.serialization.BigDecimalSerializer
import io.quarkus.runtime.annotations.RegisterForReflection
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.math.BigDecimal

/**
 * Until we are able to generate the bits in the xlsx file using a convention,
 * we are relying on a convention for the bits around kogito.
 *
 * We'll have to translate the requests and responses based on our needs.
 */
@Serializable
@RegisterForReflection
data class RulesEngineInput(
    val string: String,
    val utility: String,
    val bigDecimal: BigDecimal,
) {
    constructor(request: HandlerNameRequest) : this(request.string, request.utility, request.bigDecimal)
}

@Serializable
@RegisterForReflection
data class RulesEngineOutput(
    var string: String = "",
    var rule: String = "",
    var priority: Int = Integer.MAX_VALUE
)


@Serializable
@RegisterForReflection
data class HandlerNameRequest(
    val string: String,
    val utility: String,
    val bigDecimal: BigDecimal,
)

@Serializable
@RegisterForReflection
data class HandlerNameResponse(
    val version: String? = null,
    val outputs: List<RulesEngineOutput>,
    val excludedMatches: List<String> = listOf(),
)
