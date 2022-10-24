package com.sunrun.pricing.aws_common.helpers

import io.quarkus.arc.DefaultBean
import software.amazon.awssdk.http.SdkHttpClient
import software.amazon.awssdk.http.apache.ApacheHttpClient
import software.amazon.awssdk.http.async.SdkAsyncHttpClient
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient
import software.amazon.awssdk.services.sns.SnsClient
import java.time.Duration
import javax.enterprise.context.Dependent
import javax.enterprise.inject.Produces

@Dependent
class AwsServiceClientProducer {

    /**
     * Reuse clients to limit resource consumption.
     * https://console.aws.amazon.com/support/home#/case/?displayId=7523052041&language=en
     */
    @Produces
    @DefaultBean
    fun nettyClient(): SdkAsyncHttpClient = NettyNioAsyncHttpClient.builder()
        .connectionMaxIdleTime(Duration.ofSeconds(5)) // https://github.com/aws/aws-sdk-java-v2/issues/1122
        .build()

    @Produces
    @DefaultBean
    fun apacheClient(): SdkHttpClient = ApacheHttpClient.builder().build()

    @Produces
    @DefaultBean
    fun snsClient(): SnsClient = SnsClient.builder().httpClient(apacheClient()).build()
}

