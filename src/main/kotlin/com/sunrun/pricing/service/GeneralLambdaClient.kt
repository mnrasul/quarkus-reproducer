package com.sunrun.pricing.service

import mu.KotlinLogging
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.http.SdkHttpClient
import software.amazon.awssdk.services.lambda.LambdaClient
import software.amazon.awssdk.services.lambda.model.InvocationType
import software.amazon.awssdk.services.lambda.model.InvokeRequest
import javax.enterprise.context.ApplicationScoped

private val logger = KotlinLogging.logger {}

@ApplicationScoped
class GeneralLambdaClient(
    private val apacheHttpClient: SdkHttpClient,
) {
    fun callService(json: String, lambdaArn: String): String {
        logger.info("Request to $lambdaArn is $json")
        val client = LambdaClient.builder()
            .httpClient(apacheHttpClient)
            .credentialsProvider(DefaultCredentialsProvider.create())
            .build()
        val invokeRequest = InvokeRequest
            .builder().functionName(lambdaArn)
            .payload(SdkBytes.fromUtf8String(json))
            .invocationType(InvocationType.REQUEST_RESPONSE)
            .build()
        val response = client.invoke(invokeRequest)
        val responseJson = response.payload().asUtf8String()
        logger.info("Response from $lambdaArn is $responseJson")
        return responseJson
    }
}
