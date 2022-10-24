//@file:UseSerializers(BigDecimalSerializer::class)

package com.sunrun.pricing.quarkus

import com.sunrun.pricing.config.ConfigService
import com.sunrun.pricing.rest.*
import org.kie.kogito.rules.RuleUnitData
import org.kie.kogito.rules.SingletonStore
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.kie.kogito.rules.DataSource
import org.kie.kogito.rules.DataStore

@Serializable
class RulesEngineUnit(
    var inputObject: DataStore<RulesEngineInput> = DataSource.createStore(),
    var config: SingletonStore<ConfigService.Config> = DataSource.createSingleton(),
    var outputObject: DataStore<RulesEngineOutput> = DataSource.createStore(),
) : RuleUnitData
