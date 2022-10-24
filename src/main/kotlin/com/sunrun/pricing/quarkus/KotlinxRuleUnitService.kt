package com.sunrun.pricing.quarkus

// TODO enable this if you're using the Unified API
// import com.sunrun.pricing.serialization.polySerializers
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import org.kie.kogito.incubation.common.Id
import org.kie.kogito.incubation.rules.QueryId
import org.kie.kogito.incubation.rules.RuleUnitId
import org.kie.kogito.rules.RuleUnitData
import org.kie.kogito.rules.RuleUnits
import javax.enterprise.context.ApplicationScoped
import javax.enterprise.inject.Instance

@ApplicationScoped
class KotlinxRuleUnitService(
    var ruleUnits: Instance<RuleUnits>,
) {
    val json: Json by lazy {
        Json {
            ignoreUnknownKeys = true
            isLenient = false
            // TODO enable this if you're using polymorphicDeserialization without the default discriminator of `type`
            // TODO e.g. the Unified API
            // classDiscriminator = "productType"
            encodeDefaults = false
            // TODO enable this if you're using the Unified API
            // serializersModule = polySerializers
        }
    }

    final fun evaluate(id: Id, input: RuleUnitData): List<Any> {
        val (ruleUnitId, queryId) = getIds(id)
        val ruleUnit = ruleUnits.get().create(input.javaClass)
        val instance = ruleUnit.createInstance(input)
        val executeQuery = instance.executeQuery(queryId.queryId())
        return executeQuery.flatMap { it.values }
    }

    fun getIds(id: Id): Pair<RuleUnitId, QueryId> {
        val ruleUnitId: RuleUnitId
        val queryId: QueryId
        if (id is QueryId) {
            queryId = id
            ruleUnitId = queryId.ruleUnitId()
        } else {
            throw IllegalArgumentException("Not a valid query id " + id.toLocalId())
        }
        return ruleUnitId to queryId
    }
}