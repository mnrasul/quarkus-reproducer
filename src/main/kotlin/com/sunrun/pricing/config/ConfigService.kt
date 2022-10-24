package com.sunrun.pricing.config

import com.sunrun.pricing.aws_common.response.Result
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl
import javax.inject.Qualifier
import javax.inject.Singleton
import javax.ws.rs.core.Response

private val logger = KotlinLogging.logger {}

@Singleton
class ConfigService(@ConfigServiceJson private val json: Json) {

    companion object {
        private const val APP = "products-and-pricing"
        private const val CONFIGURATION = "main"
        private const val DEFAULT_ENVIRONMENT = "main"
    }

    @Serializable
    data class Config(var features: List<String> = listOf())

    private var cachedConfig: Config = Config()

    private val env: String by lazy {
        System.getenv("ENVIRONMENT")?.also { env ->
            logger.info("Using environment: $env")
        } ?: run {
            logger.info("Failed to read environment, falling back to $DEFAULT_ENVIRONMENT environment")
            DEFAULT_ENVIRONMENT
        }
    }

    val configStore: Config
        get() = getConfig(true)

    /**
     * Handle caching the config
     */
    private fun getConfig(refreshCache: Boolean = false): Config {
        return if (refreshCache) {
            val config = when (val appConfigResponse = retrieveConfig(APP, env, CONFIGURATION)) {
                is Result.Success -> mapToConfig(appConfigResponse.value)
                is Result.Error -> Config()
            }
            cachedConfig = config
            config
        } else {
            return cachedConfig
        }
    }

    fun hasFeature(feature: String) = getConfig().features.contains(feature)

    private fun mapToConfig(response: Response): Config {
        if (response.status != 200) {
            val output = response.readEntity(String::class.java)
            logger.error("AppConfig call failed with error $output. Defaulting to default Config()")
            return Config()
        }

        return response.readEntity(String::class.java).let { output ->
            logger.info("AppConfig is $output")
            json.decodeFromString(Config.serializer(), output)
        }
    }

    private fun retrieveConfig(app: String, env: String, configuration: String): Result<Response> {
        val client = ResteasyClientBuilderImpl().build()
        return try {
            val appConfigUrl = "http://localhost:2772/applications/$app/environments/$env/configurations/$configuration"
            logger.info("retrieving AppConfig from $appConfigUrl")
            val response = client
                .target(appConfigUrl)
                .request()
                .header("content-type", "application/json")
                .get()
            Result.Success(response)
        } catch (e: Exception) {
            logger.error("Error trying to call AppConfig: $e")
            Result.Error("Error trying to call AppConfig: ", e)
        } finally {
            client.close()
        }
    }
}

@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class ConfigServiceJson