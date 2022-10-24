package com.sunrun.pricing.services

import com.sunrun.pricing.quarkus.KotlinxRuleUnitService
import com.sunrun.pricing.quarkus.RulesEngineUnit
import org.kie.kogito.incubation.application.AppRoot
import org.kie.kogito.incubation.rules.QueryId
import org.kie.kogito.incubation.rules.RuleUnitIds
import javax.enterprise.context.ApplicationScoped
import com.sunrun.pricing.rest.*
import com.sunrun.pricing.config.ConfigService
import com.sunrun.pricing.helpers.extensions.get
import org.drools.ruleunits.api.DataSource

@ApplicationScoped
class KogitoQueryHandler(
    private val appRoot: AppRoot,
    private val ruleUnitService: KotlinxRuleUnitService,
    private val configService: ConfigService,
) {
    suspend fun getSheet(request: HandlerNameRequest): List<RulesEngineOutput> {
        /**
         * Intentionally, this is a valid name that is coded in the sheets. The intent is to generate a working sample
         * that people can play and iterate on. The audience would be relatively more inexperienced colleagues to the stack.
         *
         * Overtime people will gain experience and figure out how all the bits are connected. Until that time, we all
         * need all the help we can get.
         */
        val query = queryId("RulesEngineQuery")
        val inputObject = DataSource.createStore<RulesEngineInput>()
        inputObject.add(RulesEngineInput(request))
        val config = configService.configStore
        val configStore = DataSource.createSingleton<ConfigService.Config>().apply { set(config) }
        val outputObject = DataSource.createStore<RulesEngineOutput>()

        val unit = RulesEngineUnit(inputObject, configStore, outputObject)
        return ruleUnitService.evaluate(query, unit)
            .map { it as RulesEngineOutput }.ifEmpty {
                throw IllegalArgumentException("No drools rule matches found for request $request")
            }
    }

    fun queryId(query: String): QueryId = appRoot.get<RuleUnitIds>().get<RulesEngineUnit>().queries().get(query)

    // TODO enable if you have priority rules
    // fun <T : DroolsPriority> List<T>.filterPriority() = this.groupBy { it.priority }.minByOrNull { (k, _) -> k }?.value
}

// TODO enable if you have priority rules
interface DroolsPriority {
    val priority: Int
}
