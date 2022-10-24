package com.sunrun.pricing.aws

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestStreamHandler
import com.sunrun.pricing.rest.*
import com.sunrun.pricing.helpers.aws.*
import com.sunrun.pricing.config.ConfigService
import com.sunrun.pricing.helpers.LambdaContext
import com.sunrun.pricing.serialization.requestStreamWrapper
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import java.io.InputStream
import java.io.OutputStream
import java.math.BigDecimal
import javax.inject.Named

private val logger = KotlinLogging.logger {}

@Named("HandlerNameFnHandler")
class HandlerNameLambdaHandler(
    private val handlernameService: HandlerNameService,
    private val configService: ConfigService,
    private val lambdaContext: LambdaContext,
    private val json: Json,
) : RequestStreamHandler {

    override fun handleRequest(input: InputStream, output: OutputStream, context: Context?) {
        initializeConfig()
        initializeContext(context)
        requestStreamWrapper(input, output, json) {
            runBlocking {
                val request = decode(HandlerNameRequest.serializer())

                val realResponse = handlernameService.begin(request)
                val response = if (configService.hasFeature("handlernameMocks")) {
                    mockResponse()
                } else {
                    realResponse
                }
                encode(HandlerNameResponse.serializer(), response)
            }
        }
    }

    fun mockResponse() = HandlerNameResponse("a", listOf(RulesEngineOutput()))

    private fun initializeConfig() {
        configService.configStore
    }

    private fun initializeContext(context: Context?) {
        lambdaContext.version = context?.invokedFunctionArn ?: ""
    }
}
