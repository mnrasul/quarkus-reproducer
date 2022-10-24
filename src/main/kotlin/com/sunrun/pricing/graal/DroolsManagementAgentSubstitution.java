package com.sunrun.pricing.graal;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.drools.core.management.DroolsManagementAgent;
import org.kie.api.event.KieRuntimeEventManager;

import javax.management.ObjectName;

/**
 * See https://kie.zulipchat.com/#narrow/stream/232676-kogito for context. This resolves a mysterious build failure
 * where Drools Core brings in a JMX mbean, which are not supported in Graal Native Image builds.
 * https://github.com/galderz/infinispan-quarkus/commit/196b9e90c1d4f2aa5c28c47441e49ae70308e79f
 */
@TargetClass(value = DroolsManagementAgent.class)
final class DroolsManagementAgentSubstitution {

    @Substitute
    public void registerMBean(Object owner, Object mbean, ObjectName name) {
    }

    @Substitute
    public void unregisterMBean( Object owner, ObjectName mbean ) {
    }

    @Substitute
    private void unregisterKnowledgeSessionUnderName(DroolsManagementAgent.CBSKey cbsKey, KieRuntimeEventManager ksession) {
        System.out.println("WARN: NOOP DroolsSubstitution.unregisterKnowledgeSessionUnderName");
    }

}
