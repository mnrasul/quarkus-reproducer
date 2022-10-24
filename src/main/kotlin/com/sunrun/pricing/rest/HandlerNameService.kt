package com.sunrun.pricing.rest

import com.sunrun.pricing.quarkus.*
import com.sunrun.pricing.services.*
import com.sunrun.pricing.helpers.LambdaContext
import kotlinx.coroutines.coroutineScope
import mu.KotlinLogging
import javax.enterprise.context.ApplicationScoped

private val logger = KotlinLogging.logger {}

@ApplicationScoped
class HandlerNameService(
    private val kogitoQueryHandler: KogitoQueryHandler,
    private val lambdaContext: LambdaContext,
) {
    suspend fun begin(request: HandlerNameRequest): HandlerNameResponse = coroutineScope {
        val sheet = kogitoQueryHandler.getSheet(request)

        val (mostSpecificMatches, excludedMatches) = applyMostSpecificRulesOnly(sheet)
        HandlerNameResponse(lambdaContext.version, mostSpecificMatches, excludedMatches)
    }

    /**
     * A priority of 1 is higher than a priority of 2
     */
    fun applyMostSpecificRulesOnly(matches: List<RulesEngineOutput>): Pair<List<RulesEngineOutput>, List<String>> {
        val matchCountToFilterBy = matches.minOf { it.priority }
        val mostSpecificMatches = matches.filter { it.priority == matchCountToFilterBy }
        val excludedMatches = matches.filter { it.priority != matchCountToFilterBy }
            .map { "${it.rule} ${it.priority} was lower than $matchCountToFilterBy" }
        //return Pair(mostSpecificMatches, excludedMatches)
        return mostSpecificMatches to excludedMatches
    }
}
