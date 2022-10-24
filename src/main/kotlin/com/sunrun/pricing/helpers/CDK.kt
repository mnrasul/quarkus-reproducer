package com.sunrun.pricing.helpers

import com.sunrun.pricing.cdk.app

class LambdaApp {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            // view pricing-cdk docs for all options
            app(args) {
                applicationName = "test2"
                stack {
                    function {
                        configuration("HandlerNameFn", "HandlerNameHandler") {
                            memory = 1024
                        }
                        // TODO only needed if you're going to be making calls to this lambda from ecs services
                        grantInvoke {
                            // TODO customize for the services you need
                            ecsTasks("blackbird", "pricing-blackbird", "lightmile", "pricing-lightmile")
                        }
                        // TODO only needed if you are going to be making calls to other lambdas
                        lambdas {
                            // TODO customize with the lambdas you need
                            lambda("AVAILABILITY_ARN", "availability-AvailabilityFn")
                            lambda("A_LA_CARTE_ARN", "a-la-carte-pricing-MenuPriceFn")
                        }
                        // TODO only needed if you are going to be storing things in topics
                        topics {
                            // TODO customize with the topics you need
                            topic("price-cache-SaveTemporaryPricePointAsyncFnTopic",
                                "SaveTemporaryPricePointAsyncFnTopic")
                        }
                    }
                }
            }
        }
    }
}
