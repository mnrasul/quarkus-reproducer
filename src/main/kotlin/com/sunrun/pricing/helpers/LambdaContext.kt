package com.sunrun.pricing.helpers

import javax.inject.Singleton

/**
 * Should be ok to do this since even if it's cached, it's only cached for a single lambda version,
 * e.g. if a different version is called, a new container will need to spin up and then this cache will
 * not exist there.
 */
@Singleton
object LambdaContext {
    var version: String = ""
}