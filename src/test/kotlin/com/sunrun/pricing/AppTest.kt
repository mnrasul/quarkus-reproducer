package com.sunrun.pricing

import com.sunrun.pricing.aws.HandlerNameLambdaHandler
import io.quarkus.test.junit.QuarkusTest
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import io.quarkus.test.junit.TestProfile

@QuarkusTest
@TestProfile(LocalTestProfile::class)
class AppTest {
    @Inject
    lateinit var handler: HandlerNameLambdaHandler

    @Test
    fun test() {
        val input = """
            {
               "string": "CA12",
               "utility": "PG&E",
               "bigDecimal": 0
            }
        """.trimIndent()
        val outputStream = ByteArrayOutputStream()
        val output = handler.handleRequest(input.byteInputStream(), outputStream, null)
        println(output.toString())
        require(output != null) {
            "Expect some output"
        }
    }
}