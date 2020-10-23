package org.kordamp.gradle.plugin.enforcer.api

import org.gradle.api.Action

import groovy.transform.CompileStatic


@CompileStatic
interface EnforcerRuleConfiguration {
    /**
     * Define a rule.
     */
    public <R extends EnforcerRule> void rule(Class<R> ruleType)

    /**
     * Define and configure a rule.
     */
    public <R extends EnforcerRule> void rule(Class<R> ruleType, Action<R> configurer)
}
