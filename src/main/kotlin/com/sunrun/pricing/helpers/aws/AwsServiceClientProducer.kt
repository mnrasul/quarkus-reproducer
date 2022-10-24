package com.sunrun.pricing.helpers.aws

import software.amazon.awssdk.http.SdkHttpClient
import software.amazon.awssdk.http.apache.ApacheHttpClient
import software.amazon.awssdk.http.async.SdkAsyncHttpClient
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient
import java.time.Duration
import javax.enterprise.context.ApplicationScoped
import javax.enterprise.inject.Produces

class AwsServiceClientProducer {

    /**
     * Reuse clients to limit resource consumption.
     * https://console.aws.amazon.com/support/home#/case/?displayId=7523052041&language=en
     */
    @ApplicationScoped
    @Produces
    fun nettyClient(): SdkAsyncHttpClient = NettyNioAsyncHttpClient.builder()
        .connectionMaxIdleTime(Duration.ofSeconds(5)) // https://github.com/aws/aws-sdk-java-v2/issues/1122
        .build()

    @ApplicationScoped
    @Produces
    fun apacheClient(): SdkHttpClient = ApacheHttpClient.builder().build()
}
