//@file:UseSerializers(BigDecimalSerializer::class)

package com.sunrun.pricing.quarkus

import com.sunrun.pricing.rest.*
import org.drools.ruleunits.api.DataSource
import org.drools.ruleunits.api.DataStore
import org.drools.ruleunits.api.RuleUnitData
import org.drools.ruleunits.api.SingletonStore
class RulesEngineUnit(
    var inputObject: DataStore<RulesEngineInput> = DataSource.createStore(),
    var outputObject: DataStore<RulesEngineOutput> = DataSource.createStore(),
) : RuleUnitData
