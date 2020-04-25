/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2020 The author and/or original authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kordamp.gradle.plugin.enforcer.internal

import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import org.gradle.api.Action
import org.gradle.api.invocation.Gradle
import org.gradle.tooling.BuildException
import org.kordamp.gradle.plugin.enforcer.api.EnforcerContext
import org.kordamp.gradle.plugin.enforcer.api.EnforcerExtension
import org.kordamp.gradle.plugin.enforcer.api.EnforcerRule
import org.kordamp.gradle.plugin.enforcer.api.EnforcerRuleException
import org.kordamp.gradle.plugin.enforcer.api.MergeStrategy
import org.kordamp.gradle.plugin.enforcer.api.ProjectEnforcerContext

import static org.apache.commons.lang3.StringUtils.isBlank
import static org.apache.commons.lang3.StringUtils.isNotBlank

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@PackageScope
@CompileStatic
abstract class AbstractEnforcerRuleInvoker {
    protected final Gradle gradle
    protected final AbstractEnforcerExtension extension

    AbstractEnforcerRuleInvoker(Gradle gradle, EnforcerExtension extension) {
        this.gradle = gradle
        this.extension = (AbstractEnforcerExtension) extension
    }

    protected void maybeCreateAndInvokeRules(EnforcerContext context,
                                             List<? extends EnforcerRuleHelper> helpers,
                                             List<? extends EnforcerRule> rules,
                                             List<EnforcerRuleException> collector) {
        if (rules) {
            for (EnforcerRule rule : rules) {
                invokeRule(context, rule, collector)
            }
        } else {
            createAndInvokeRules(helpers,
                extension.mergeStrategy.get(),
                context,
                rules,
                collector)
        }
    }

    protected abstract <T extends EnforcerRule> T instantiateRule(EnforcerContext context, Class<T> ruleType)

    protected void createAndInvokeRules(List<? extends EnforcerRuleHelper> helpers,
                                        MergeStrategy mergeStrategy,
                                        EnforcerContext context,
                                        List<? extends EnforcerRule> rules,
                                        List<EnforcerRuleException> collector) {
        for (EnforcerRuleHelper helper : helpers) {
            if (helper.match(context)) {
                if (mergeStrategy == MergeStrategy.DUPLICATE) {
                    if (helper.actions) {
                        for (Action<? extends EnforcerRule> action : helper.actions) {
                            EnforcerRule rule = instantiateRule(context, helper.ruleType)
                            action.execute(rule)
                            rules.add(rule)
                            invokeRule(context, rule, collector)
                        }
                    } else {
                        EnforcerRule rule = instantiateRule(context, helper.ruleType)
                        rules.add(rule)
                        invokeRule(context, rule, collector)
                    }
                } else {
                    EnforcerRule rule = instantiateRule(context, helper.ruleType)
                    helper.actions.each { a -> a.execute(rule) }
                    rules.add(rule)
                    invokeRule(context, rule, collector)
                }
            }
        }
    }

    protected String toPrefix(EnforcerContext context) {
        if (context instanceof ProjectEnforcerContext) {
            return "[${context.enforcerPhase.name()} ${((ProjectEnforcerContext) context).project.path}]"
        }
        return "[${context.enforcerPhase.name()}]"
    }

    protected void report(EnforcerContext context, List<EnforcerRuleException> collector) {
        if (collector) {
            if (collector.size() == 1) {
                EnforcerRuleException x = collector[0]
                throw new BuildException("${toPrefix(context)} An Enforcer rule has failed", x)

            } else {
                MultipleEnforcerRuleException e = new MultipleEnforcerRuleException(collector)
                throw new BuildException("${toPrefix(context)} ${collector.size()} Enforcer rules have failed", e)
            }
        }
    }

    protected <RULE extends EnforcerRule> void invokeRule(EnforcerContext context,
                                                          RULE rule,
                                                          List<EnforcerRuleException> collector) {
        String ruleClassName = normalizeClassName(rule.class)
        try {
            if (!isRuleEnabled(rule)) return
            extension.LOG.debug("${extension.prefix} Invoking candidate rule ${ruleClassName}")
            rule.execute(context)
        } catch (EnforcerRuleException e) {
            if (extension.resolvedFailFast.get()) {
                throw new BuildException("${toPrefix(context)} An Enforcer rule has failed", e)
            } else {
                collector.add(e)
            }
        } catch (Exception e) {
            throw new BuildException("${toPrefix(context)} Unexpected error when evaluating enforcer rule '${ruleClassName}'", e)
        }
    }

    private String normalizeClassName(Class<?> klass) {
        String className = klass.name
        if (className.endsWith('_Decorated')) className -= '_Decorated'
        className
    }

    private boolean isRuleEnabled(EnforcerRule rule) {
        String value = System.getProperty(normalizeClassName(rule.class) + '.enabled')
        if (isNotBlank(value)) {
            return Boolean.parseBoolean(value)
        }
        rule.getEnabled().getOrElse(false)
    }

    protected boolean isExtensionEnabled() {
        String value = System.getProperty('enforcer.enabled')
        if (isNotBlank(value)) {
            return Boolean.parseBoolean(value)
        }
        extension.getEnabled().getOrElse(false)
    }

    protected boolean isEnforcerPhaseEnabled(EnforcerContext context) {
        String key = 'enforcer.phase.' + context.enforcerPhase.name().toLowerCase() + '.enabled'
        String value = System.getProperty(key)

        if (isBlank(value)) {
            key = 'enforcer.phase.' + context.enforcerPhase.name().toLowerCase().replace('_', '.') + '.enabled'
            value = System.getProperty(key)
        }

        isNotBlank(value) ? Boolean.parseBoolean(value.trim()) : true
    }

    protected boolean isBuildPhaseEnabled(EnforcerContext context) {
        boolean phaseEnabled = isEnforcerPhaseEnabled(context) && isExtensionEnabled()
        extension.LOG.info("${extension.prefix} phase=${context.enforcerPhase.name()}, enabled=${phaseEnabled}, failFast=${extension.resolvedFailFast.get()}")
        phaseEnabled
    }
}
