package com.sunrun.pricing.helpers.extensions

import org.kie.kogito.incubation.application.AppRoot
import org.kie.kogito.incubation.common.ComponentRoot
import org.kie.kogito.incubation.rules.RuleUnitId
import org.kie.kogito.incubation.rules.RuleUnitIds

inline fun <reified T : ComponentRoot> AppRoot.get(): T = this.get(T::class.java)
inline fun <reified T> RuleUnitIds.get(): RuleUnitId = this.get(T::class.java)
