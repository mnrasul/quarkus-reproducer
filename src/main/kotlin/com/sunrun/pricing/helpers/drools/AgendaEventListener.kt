package com.sunrun.pricing.helpers.drools

import mu.KotlinLogging
import org.kie.api.event.rule.*
import javax.enterprise.context.ApplicationScoped

private val logger = KotlinLogging.logger {}

@ApplicationScoped
class AgendaEventListener : DefaultAgendaEventListener() {
    /**
     * After match fired is semantically equivalent to a rule firing.
     */
    override fun afterMatchFired(event: AfterMatchFiredEvent?) {
        logger.info("Rule Fired: ${event?.match?.rule?.name}")
    }

    override fun matchCancelled(event: MatchCancelledEvent?) {
        logger.info("Rule cancelled: ${event?.match?.rule?.name}")
    }

    override fun matchCreated(event: MatchCreatedEvent?) {
        logger.info("Rule created: ${event?.match?.rule?.name}")
    }
    override fun beforeMatchFired(event: BeforeMatchFiredEvent?) {
        logger.info("before Rule fired: ${event?.match?.rule?.name}")
    }
}
