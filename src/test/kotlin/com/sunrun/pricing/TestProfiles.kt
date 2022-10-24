package com.sunrun.pricing

import io.quarkus.test.junit.QuarkusTestProfile


class LocalTestProfile : QuarkusTestProfile {
    override fun tags(): Set<String> {
        return setOf("local","test")
    }
}

class CloudTestProfile : QuarkusTestProfile {
    override fun tags(): Set<String> {
        return setOf("cloud")
    }
}
class IntegrationTestProfile : QuarkusTestProfile {
    override fun tags(): Set<String> {
        return setOf("integration")
    }
}