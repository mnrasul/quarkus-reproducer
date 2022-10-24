package com.sunrun.pricing.cdk.model

import com.sunrun.pricing.cdk.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import software.amazon.awscdk.core.*
import software.amazon.awscdk.core.CfnResource
import software.amazon.awscdk.core.Stack
import software.amazon.awscdk.services.apigateway.*
import software.amazon.awscdk.services.iam.Effect
import software.amazon.awscdk.services.iam.IRole
import software.amazon.awscdk.services.iam.PolicyStatement
import software.amazon.awscdk.services.iam.Role
import software.amazon.awscdk.services.lambda.*
import software.amazon.awscdk.services.lambda.Function
import software.amazon.awscdk.services.sns.Topic
import software.amazon.awscdk.services.ssm.StringParameter
import software.amazon.awscdk.services.ssm.StringParameterProps
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField
import java.util.*


/**
 * In principal, we could have a different version for each alias, but that would complicate things alot.
 * We'll stick to same version for all functions in a release. Over time it could result in lots of versions, but
 * in practice, we only pay for actual invocations.
 *
 */
data class FunctionDefinition(
    val name: String,
    val handler: String,
)

object OS {
    // Cloud formations does not support . characters
    val whoAmI: String
        get() = String(java.lang.Runtime.getRuntime().exec("whoami").inputStream.readBytes()).replace(".", "-").trim()
    val branch: String
            by lazy {
                System.getenv("GITHUB_HEAD_REF") ?: "no-branch-found"
            }
}

data class StackConfiguration(
    val envName: String,
    val stackName: String,
    val awsEnv: SunrunApplicationEnvironment,
) {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    val envNameWithoutEphemeral: String
        get() = if (awsEnv.isEphemeral) "devmaj" else envName

    init {
        logger.info("Synthesizing $this")
    }

    companion object {
        fun fromCliArgs(args: Array<String>, applicationName: String): StackConfiguration {
            val envName = if (args.isNotEmpty()) {
                args[0]
            } else {
                OS.whoAmI
            }

            val stackName = "$envName-$applicationName"
            val applicationEnvironment = SunrunApplicationEnvironment.valueOfCaseInsensitive(envName)

            return StackConfiguration(envName, stackName, applicationEnvironment)
        }
    }
}

enum class SunrunAwsAccount(val accountNumber: String) {
    Local("000000000000"), Dev("563116987804"), Stage("339585210943"), Prd("578915239930")
}

enum class SunrunApplicationEnvironment(val account: SunrunAwsAccount, val isEphemeral: Boolean) {
    Local(SunrunAwsAccount.Local, true),
    Sandbox(SunrunAwsAccount.Dev, true),
    Devmaj(SunrunAwsAccount.Dev, false),
    Integration(SunrunAwsAccount.Dev, false),
    Relcert(SunrunAwsAccount.Stage, false),
    Majstg(SunrunAwsAccount.Prd, false),
    Prd(SunrunAwsAccount.Prd, false);

    val removalPolicy: RemovalPolicy
        get() = if (isEphemeral) RemovalPolicy.DESTROY else RemovalPolicy.RETAIN

    companion object {
        private val valuesByNameCaseInsensitive = values().associateBy { it.name.lowercase().trim() }

        fun valueOfCaseInsensitive(value: String): SunrunApplicationEnvironment {
            return valuesByNameCaseInsensitive.getOrDefault(value.lowercase().trim(), Sandbox)
        }
    }
}


class AppBuilder(private val args: Array<String>) {
    private val app: App = App()
    lateinit var applicationName: String
    private val stack: StackConfiguration by lazy { StackConfiguration.fromCliArgs(args, applicationName) }
    private val tags = TagsBuilder()

    fun tags(builder: TagsBuilder.() -> Unit) {
        builder.invoke(tags)
    }

    inner class TagsBuilder {
        val defaultTags = true
        private val list = mutableListOf<Pair<String, String>>()

        fun tag(name: String, value: String) {
            list.add(name to value)
        }

        fun defaultTags() {
            list.add("branchName" to OS.branch)
        }

        fun build() = list.forEach { Tags.of(app).add(it.first, it.second) }
    }

    fun build(): App {
        if (tags.defaultTags) tags.defaultTags()
        tags.build()
        app.synth()
        return app
    }

    fun stack(block: StackBuilder.() -> Unit) {
        val stackBuilder = StackBuilder(app, stack)
        block.invoke(stackBuilder)
        stackBuilder.build()
    }

    inner class StackBuilder(
        private val scope: Construct,
        private val stack: StackConfiguration,
    ) {
        private val logger: Logger = LoggerFactory.getLogger(this::class.java)

        fun function(block: FunctionBuilder.() -> Unit) {
            val functionBuilder = FunctionBuilder()
            block.invoke(functionBuilder)
            functionBuilder.build()
        }

        fun build() {
        }

        inner class FunctionBuilder {
            var enableAlias: Boolean = true
            var enableAppConfig: Boolean = true
            var exportFunctionArn: Boolean = true
            var enableRestApi: Boolean = false
            var enableLegacyVersioning: Boolean = false

            private lateinit var config: FunctionConfigurationBuilder
            private val environments = EnvironmentBuilder()
            private val tags = TagsBuilder()
            private val lambdas = LambdasBuilder(stack)
            private val topics = TopicsBuilder()
            private val invokes = InvokeGranterBuilder()

            fun lambdas(builder: LambdasBuilder.() -> Unit) {
                builder.invoke(lambdas)
            }

            inner class LambdasBuilder(val stack: StackConfiguration) {
                val lambdas = mutableListOf<LambdaBuilder>()

                inner class LambdaBuilder(val stack: StackConfiguration, val name: String, val value: String) {
                    private val policies = mutableListOf(PolicyBuilder(stack))
                    var policyStatements = mutableListOf<PolicyStatement>()
                    val import = getImportFunction(value)

                    inner class PolicyBuilder(val stack: StackConfiguration) {
                        private val actions = mutableListOf("lambda:InvokeFunction", "lambda:ListVersionsByFunction")
                        private val resources = mutableListOf(lambdaArn(stack, value))
                        var effect = Effect.ALLOW

                        fun actions(vararg actions: String) = this.actions.addAll(actions)
                        fun resources(vararg arns: String) = resources.addAll(arns)
                        fun build(): PolicyStatement {
                            val policy = PolicyStatement()
                            policy.addActions(*actions.toTypedArray())
                            policy.addResources(*resources.toTypedArray())
                            policy.effect = effect
                            return policy
                        }
                    }

                    fun policy(builder: PolicyBuilder.() -> Unit) {
                        val policy = PolicyBuilder(stack)
                        builder.invoke(policy)
                        policies.add(policy)
                    }

                    fun build(): LambdaBuilder {
                        policies.forEach { policyStatements.add(it.build()) }
                        return this
                    }
                }

                /**
                 * Given a [name] and a [value]:
                 * Imports an value from the export of another stack using importValue
                 * https://docs.aws.amazon.com/cdk/api/v1/docs/@aws-cdk_core.Fn.html#static-importwbrvaluesharedvaluetoimport
                 * Then using that value, it sets an environment variable [name] to that value.
                 *
                 * Also, it adds an execution policy allowing this function to execute the
                 * [value] function
                 *
                 * Sets the environment variable [name] to the
                 * given [value] on the AWS Function
                 */
                fun lambda(name: String, value: String, builder: (LambdaBuilder.() -> Unit)? = null) {
                    val lambda = LambdaBuilder(this@StackBuilder.stack, name, value)
                    builder?.invoke(lambda)
                    lambdas.add(lambda)
                }
            }


            fun topics(builder: TopicsBuilder.() -> Unit) {
                builder.invoke(topics)
            }

            /**
             * Given a list of Pairs, where the first component is the topicSuffix and the
             * second is the environmentVariable, grants access to the given topicSuffix
             * and sets an environment variable to the constructed arn
             *
             * @param topicSuffix the partial suffix of the SNS Topic ARN.
             * Will look something like `service-name-FunctionNameTopic`.
             * Example: for the ARN `arn:aws:sns:us-west-2:563116987804:devmaj-price-cache-GetPricePointFnTopic`
             * you would pass `price-cache-GetPricePointFnTopic
             * @param envVar the environment variable that will be configured in AWS for use in your function
             */
            inner class TopicsBuilder {
                val topics = mutableListOf<TopicBuilder>()

                inner class TopicBuilder(val name: String, val envVar: String) {
                    val arn = snsArn(stack, name)
                }

                /**
                 * Given a [name] and a [value]:
                 * Imports an value from the export of another stack using importValue
                 * https://docs.aws.amazon.com/cdk/api/v1/docs/@aws-cdk_core.Fn.html#static-importwbrvaluesharedvaluetoimport
                 * Then using that value, it sets an environment variable [name] to that value.
                 *
                 * Also, it adds an execution policy allowing this function to execute the
                 * [value] function
                 *
                 * Sets the environment variable [name] to the
                 * given [value] on the AWS Function
                 */
                fun topic(name: String, value: String) {
                    topics.add(TopicBuilder(name, value))
                }
            }

            fun environment(builder: EnvironmentBuilder.() -> Unit) {
                builder.invoke(environments)
            }

            inner class EnvironmentBuilder {
                val list = mutableListOf<Pair<String, String>>()
                operator fun Pair<String, String>.unaryPlus() {
                    list.add(this)
                }

                /**
                 * Sets the environment variable [name] to the
                 * given [value] on the AWS Function
                 */
                fun e(name: String, value: String) = list.add(name to value)
            }

            fun configuration(
                name: String,
                handler: String,
                builder: (FunctionConfigurationBuilder.() -> Unit)? = null,
            ) {
                if (this::config.isInitialized) {
                    throw IllegalStateException("You can only have one configuration call per function")
                }
                val fnConfig = FunctionConfigurationBuilder(name, handler)
                builder?.invoke(fnConfig)
                config = fnConfig
            }

            inner class FunctionConfigurationBuilder(val name: String, val handler: String) {
                var memory: Int = 256
                var runtime = Runtime.PROVIDED
                var codePath = "target/function.zip"
                var tracing = Tracing.ACTIVE
                var timeout = Duration.seconds(30)
                var removalPolicy = RemovalPolicy.RETAIN
            }

            fun tags(builder: TagsBuilder.() -> Unit) {
                builder.invoke(tags)
            }

            inner class TagsBuilder {
                var defaultTags = true
                private val list = mutableListOf<Pair<String, String>>()

                fun tag(name: String, value: String) {
                    list.add(name to value)
                }

                fun defaultTags() {
                    list.add("Family" to "pricing")
                    list.add("AppName" to applicationName)
                    list.add("ServiceOwner" to "pricing")
                }

                fun build(func: Function) = list.forEach { Tags.of(func).add(it.first, it.second) }
            }

            fun grantInvoke(builder: InvokeGranterBuilder.() -> Unit) {
                builder.invoke(invokes)
            }

            inner class InvokeGranterBuilder {
                val ecsTasks = mutableListOf<String>()

                fun ecsTasks(vararg name: String) {
                    ecsTasks.addAll(name)
                }
            }

            private inner class FnForScope : Stack(scope, stack.stackName, null) {
                fun build(): Function {
                    val function = function(config.name, functionProps {
                        this.runtime(config.runtime)
                        this.handler("not.used.by.quarkus.in.native.mode")
                        this.code(Code.fromAsset(config.codePath))
                        this.tracing(config.tracing)
                        this.timeout(config.timeout)
                        this.memorySize(config.memory)
                        this.functionName("${stack.stackName}-${config.name}")

                        val versionOptions = VersionOptions.builder()
                            .description("${UUID.randomUUID()}")
                            .removalPolicy(config.removalPolicy)
                            .build()
                        this.currentVersionOptions(versionOptions)

                        environment(
                            mapOf(
                                "DISABLE_SIGNAL_HANDLERS" to "true",// required by graal native
                                "ENVIRONMENT" to stack.envName
                            )
                        )
                    })

                    val version = function.currentVersion
                    logger.info("Current Version is: ${version.latestVersion.version}")

                    if (exportFunctionArn) function.outputFunctionArn(stack, config.name)
                    if (enableAppConfig) function.addAppConfig(stack)
                    // Added below code to enable legacy versioning - BEGIN
                    if(enableLegacyVersioning) {
                        val versionParameterProps = StringParameterProps.builder()
                            .parameterName("/$stackName-${config.name}/LATEST_VERSION")
                            .stringValue(version.version)
                            .build()
                        val versionParameter = StringParameter(this, "${config.name}LatestVersion", versionParameterProps)
                        versionParameter.grantRead(Role.fromRoleArn(this, "VersionReadableArn", function.role!!.roleArn))
                    }
                    //enable legacy versioning - END

                    val lambdas: List<LambdasBuilder.LambdaBuilder> = lambdas.lambdas.map { it.build() }
                    function.addLambdaImports(lambdas)
                    function.addLambdaExecutionPolicy(lambdas)

                    if (enableAlias) function.addAlias()
                    if (enableRestApi) function.createRestApi()

                    val topics = topics.topics
                    function.setupTopics(topics)

                    if (this@FunctionBuilder.tags.defaultTags) this@FunctionBuilder.tags.defaultTags()
                    this@FunctionBuilder.tags.build(function)

                    function.grantInvokes()
                    return function
                }

                /**
                 * creates an alias in the form `v2022-03-03:900` where 900 is just how many
                 * minutes into the day you are.
                 *
                 * This is an expanded form of [CalVer](https://calver.org/) with the minutes of
                 * the day tacked onto the end to account for multiple deploys happening in a single
                 * day. In general we are not to the point where we can release multiple times a minute,
                 * so functionally there is no issue with this yet. If we ever get to that point
                 * this format should be revisited.
                 *
                 * Note: There is a small issue if we were to deploy at the same minute on a time-change,
                 * e.g. if we deployed at 12:01AM and then again at 1:01AM on DST then we could have an
                 * issue.
                 */
                private fun Function.addAlias() {
                    println("creating a new alias")
                    val zid = ZoneId.of("America/Denver")
                    val current = ZonedDateTime.now(zid)

                    val d = DateTimeFormatterBuilder()
                        .parseCaseInsensitive()
                        .appendLiteral('v')
                        .append(DateTimeFormatter.ISO_LOCAL_DATE)
                        .optionalStart()
                        .appendLiteral('m')
                        .appendText(ChronoField.MINUTE_OF_DAY)
                        .toFormatter()
                    val aliasName = current.format(d)
                    println("aliasName is $aliasName")

                    val alias = Alias.Builder.create(this@FnForScope, "${config.name}PlainAlias")
                        .aliasName(aliasName)
                        .version(this.currentVersion)
                        .build()
                    val headAlias = Alias.Builder.create(this@FnForScope, "${config.name}HeadAlias")
                        .aliasName("HEAD")
                        .version(this.currentVersion)
                        .build()
                    (alias.node.defaultChild as CfnResource).applyRemovalPolicy(RemovalPolicy.RETAIN);
                }

                private fun Function.setupTopics(topics: MutableList<TopicsBuilder.TopicBuilder>) {
                    topics.forEach { topic ->
                        this.addEnvironment(topic.envVar, topic.arn)
                        val tp = Topic.fromTopicArn(this@FnForScope, topic.arn, topic.arn)
                        tp.grantPublish(this)
                    }
                }

                private fun Function.createRestApi() {
                    val api = RestApi.Builder.create(this@FnForScope, "$stackName-${config.name}-api")
                        .endpointTypes(mutableListOf(EndpointType.REGIONAL)).build()

                    val integration = LambdaIntegration(
                        this, LambdaIntegrationOptions.builder()
                            .proxy(false)
                            .integrationResponses(
                                listOf(
                                    IntegrationResponse.builder()
                                        .statusCode("200").build()
                                )
                            )
                            .build()
                    )
                    val options = MethodOptions.builder()
                        .apiKeyRequired(true)
                        .methodResponses(listOf(MethodResponse.builder().statusCode("200").build()))
                        .build()
                    api.root.addMethod("ANY", integration, options)

                    val devApiKey = api.addApiKey("$stackName-Developers")
                    api.addUsagePlan(
                        "$stackName-Developers-Usage",
                        UsagePlanProps.builder()
                            .apiKey(devApiKey)
                            .apiStages(listOf(UsagePlanPerApiStage.builder().api(api).stage(api.deploymentStage)
                                .build()))
                            .build()
                    )

                    val gsheetApiKey = api.addApiKey("$stackName-GSheet")
                    api.addUsagePlan(
                        "$stackName-GSheet-Usage",
                        UsagePlanProps.builder()
                            .apiKey(gsheetApiKey)
                            .apiStages(listOf(UsagePlanPerApiStage.builder().api(api).stage(api.deploymentStage)
                                .build()))
                            .build()
                    )
                }

                private fun Function.grantInvokes() {
                    if (this@StackBuilder.stack.awsEnv.isEphemeral) {
                        logger.info("Skipping IAM policies because this is an ephemeral env.")
                        return
                    }
                }

                private fun ecsTaskRole(id: String): IRole {
                    val awsAccountNumber = stack.awsEnv.account.accountNumber
                    // lifted from https://github.com/SunRun/zopio/blob/master/CF/Zopio_apppolicy.yaml
                    return Role.fromRoleArn(this, id, "arn:aws:iam::${awsAccountNumber}:role/${id}")
                }
            }

            fun build() {
                FnForScope().build()
            }

            private fun Function.addLambdaImports(lambdas: List<LambdasBuilder.LambdaBuilder>) {
                lambdas.forEach { this.addEnvironment(it.name, it.import) }
            }

            private fun Function.addLambdaExecutionPolicy(lambdas: List<LambdasBuilder.LambdaBuilder>) =
                lambdas.forEach { lambda ->
                    lambda.policyStatements.forEach { this.addToRolePolicy(it) }
                }

            private fun getImportFunction(funValue: String) =
                if (stack.awsEnv.isEphemeral) Fn.importValue("devmaj-$funValue")
                else Fn.importValue("${stack.envName}-$funValue")

            private fun lambdaArn(stack: StackConfiguration, name: String) =
                "arn:aws:lambda:us-west-2:${stack.awsEnv.account.accountNumber}:function:${stack.envNameWithoutEphemeral}-$name*"

            private fun snsArn(stack: StackConfiguration, name: String) =
                "arn:aws:sns:us-west-2:${stack.awsEnv.account.accountNumber}:${stack.envNameWithoutEphemeral}-$name"
        }
    }
}
