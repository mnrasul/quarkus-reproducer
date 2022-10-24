package com.sunrun.pricing.cdk

import com.sunrun.pricing.cdk.model.AppBuilder
import com.sunrun.pricing.cdk.model.FunctionDefinition
import com.sunrun.pricing.cdk.model.StackConfiguration
import software.amazon.awscdk.services.iam.Effect
import software.amazon.awscdk.services.iam.PolicyStatement
import software.amazon.awscdk.services.lambda.*
import software.amazon.awscdk.services.lambda.Function
import java.util.*
import mu.KotlinLogging
import software.amazon.awscdk.core.*
import software.amazon.awscdk.services.apigateway.*

private val logger = KotlinLogging.logger {}

private const val APP_CONFIG_WEST_2_LAYER_VERSION_ARN = "arn:aws:lambda:us-west-2:359756378197:layer:AWS-AppConfig-Extension:89"

fun Function.addAppConfig(stack: StackConfiguration) {
    val appConfigPolicy = PolicyStatement().apply {
        addActions(
            "appconfig:GetConfiguration",
            "logs:CreateLogGroup",
            "appconfig:GetLatestConfiguration",
            "appconfig:StartConfigurationSession",
        )
        addResources(
            "arn:aws:appconfig:*:${stack.awsEnv.account.accountNumber}:application/*",
            "arn:aws:appconfig:*:${stack.awsEnv.account.accountNumber}:application/*/environment/*",
            "arn:aws:appconfig:*:${stack.awsEnv.account.accountNumber}:application/*/configurationprofile/*"
        )
        effect = Effect.ALLOW
    }
    this.addToRolePolicy(appConfigPolicy)

    val appConfigLayer = LayerVersion.fromLayerVersionArn(
        this,
        "AWS-AppConfig-Extension",
        APP_CONFIG_WEST_2_LAYER_VERSION_ARN
    )
    logger.info("Adding app config layer")
    this.addLayers(appConfigLayer)
}

fun Function.outputFunctionArn(stack: StackConfiguration, name: String) {
    cfnOutput("${name}Output", cfnOutputProps {
        exportName("${stack.stackName}-${name}")
        value(this@outputFunctionArn.functionArn)
    })
}

/**
 * @see Function
 */
fun Construct.function(
    id: String,
    props: FunctionProps,
    init: (Function.() -> Unit)? = null
): Function {

    val obj = Function(this, id, props)
    init?.invoke(obj)
    return obj
}


/**
 * @see FunctionProps.Builder
 */
fun functionProps(init: FunctionProps.Builder.() -> Unit): FunctionProps {
    val builder = FunctionProps.Builder()
    builder.init()
    return builder.build()
}


/**
 * @see software.amazon.awscdk.CfnOutput
 */
fun Construct.cfnOutput(
    id: String,
    props: CfnOutputProps,
    init: (CfnOutput.() -> Unit)? = null
) : CfnOutput {
    val obj = CfnOutput(this, id, props)
    init?.invoke(obj)
    return obj
}


/**
 * @see software.amazon.awscdk.CfnOutputProps.Builder
 */
fun cfnOutputProps(init: CfnOutputProps.Builder.() -> Unit): CfnOutputProps {
    val builder = CfnOutputProps.Builder()
    builder.init()
    return builder.build()
}

fun app(args: Array<String>, block: AppBuilder.() -> Unit): App {
    val appBuilder = AppBuilder(args)
    block.invoke(appBuilder)
    return appBuilder.build()
}
