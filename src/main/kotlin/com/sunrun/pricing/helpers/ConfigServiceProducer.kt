package com.sunrun.pricing.helpers

import io.quarkus.arc.DefaultBean
import kotlinx.serialization.json.Json
import javax.enterprise.context.Dependent
import javax.enterprise.inject.Produces

@Dependent
class ConfigServiceProducer {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    @Produces
    @DefaultBean
    fun json() = json
}
