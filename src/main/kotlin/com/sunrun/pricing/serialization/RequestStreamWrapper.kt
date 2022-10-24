package com.sunrun.pricing.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import java.io.InputStream
import java.io.OutputStream

val logger = KotlinLogging.logger { }

fun requestStreamWrapper(
    input: InputStream,
    output: OutputStream,
    json: Json,
    block: SerializationWrapper.() -> Unit,
) {
    SerializationWrapper(input, output, json).also { wrapper ->
        block.invoke(wrapper)
    }
}

data class SerializationWrapper(
    val input: InputStream,
    val output: OutputStream,
    val json: Json,
) {

    inline fun <reified I> decode(deserializer: KSerializer<I>): I {
        val inputJsonString = input.reader().use { it.readText() }
        logger.info { "Lambda Input $inputJsonString" }
        return json.decodeFromString(deserializer, inputJsonString)
    }

    inline fun <reified O> encode(serializer: KSerializer<O>, response: O) {
        val outputJsonString = json.encodeToString(serializer, response)
        logger.info { "Lambda Output $outputJsonString" }
        output.writer().use { it.write(outputJsonString) }
    }
}
