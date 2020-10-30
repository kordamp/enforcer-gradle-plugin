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
import org.gradle.BuildAdapter
import org.gradle.api.Action
import org.gradle.api.invocation.Gradle
import org.gradle.tooling.BuildException
import org.kordamp.gradle.plugin.enforcer.api.EnforcerContext
import org.kordamp.gradle.plugin.enforcer.api.EnforcerExtension
import org.kordamp.gradle.plugin.enforcer.api.EnforcerLevel
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
abstract class AbstractEnforcerRuleInvoker extends BuildAdapter {
    protected final Gradle gradle
    protected final AbstractEnforcerExtension extension

    AbstractEnforcerRuleInvoker(Gradle gradle, EnforcerExtension extension) {
        this.gradle = gradle
        this.extension = (AbstractEnforcerExtension) extension
    }

    protected void maybeCreateAndInvokeRules(EnforcerContext context,
                                             List<? extends EnforcerRuleHelper> helpers,
                                             List<? extends EnforcerRule> rules,
                                             List<RuleExecutionFailure<? extends EnforcerRule>> collector) {
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
                                        List<RuleExecutionFailure<? extends EnforcerRule>> collector) {
        for (EnforcerRuleHelper helper : helpers) {
            if (helper.match(context)) {
                if (mergeStrategy == MergeStrategy.DUPLICATE) {
                    if (helper.actions) {
                        for (Action<? extends EnforcerRule> action : helper.actions) {
                            EnforcerRule rule = instantiateRule(context, helper.ruleType)
                            action.execute(rule)
                            if (validateRule(context, rule, collector)) {
                                rules.add(rule)
                                invokeRule(context, rule, collector)
                            }
                        }
                    } else {
                        EnforcerRule rule = instantiateRule(context, helper.ruleType)
                        if (validateRule(context, rule, collector)) {
                            rules.add(rule)
                            invokeRule(context, rule, collector)
                        }
                    }
                } else {
                    EnforcerRule rule = instantiateRule(context, helper.ruleType)
                    helper.actions.each { a -> a.execute(rule) }
                    if (validateRule(context, rule, collector)) {
                        rules.add(rule)
                        invokeRule(context, rule, collector)
                    }
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

    protected void report(EnforcerContext context, List<RuleExecutionFailure<? extends EnforcerRule>> collector) {
        if (collector) {
            if (collector.size() == 1) {
                RuleExecutionFailure<? extends EnforcerRule> x = collector[0]
                if (isFailOnError(x.rule)) {
                    throw new BuildException("${toPrefix(context)} An Enforcer rule has failed", x.failure)
                } else {
                    context.logger.warn("${toPrefix(context)} An Enforcer rule has failed\n${x.failure}")
                }
            } else {
                MultipleEnforcerRuleException e = new MultipleEnforcerRuleException(collector)
                if (e.error || isFailOnError()) {
                    throw new BuildException("${toPrefix(context)} ${collector.size()} Enforcer rules have failed", e)
                } else {
                    context.logger.warn("${toPrefix(context)} ${collector.size()} Enforcer rules have failed\n${e}")
                }
            }
        }
    }

    protected <RULE extends EnforcerRule> boolean validateRule(EnforcerContext context,
                                                               RULE rule,
                                                               List<RuleExecutionFailure<? extends EnforcerRule>> collector) {
        String ruleClassName = normalizeClassName(rule.class)
        try {
            if (!isRuleEnabled(rule)) return
            extension.LOG.debug("${extension.prefix} Validating rule ${ruleClassName}")
            rule.validate(context)
            return true
        } catch (EnforcerRuleException e) {
            if (extension.resolvedFailFast.get()) {
                if (isFailOnError(rule)) {
                    throw new BuildException("${toPrefix(context)} An Enforcer rule has failed", e)
                } else {
                    context.logger.warn("${toPrefix(context)} An Enforcer rule has failed\n${e}")
                    return false
                }
            } else {
                collector.add(new RuleExecutionFailure<EnforcerRule>(rule, e))
                return false
            }
        } catch (Exception e) {
            throw new BuildException("${toPrefix(context)} Unexpected error when validating enforcer rule '${ruleClassName}'", e)
        }
    }

    protected <RULE extends EnforcerRule> void invokeRule(EnforcerContext context,
                                                          RULE rule,
                                                          List<RuleExecutionFailure<? extends EnforcerRule>> collector) {
        String ruleClassName = normalizeClassName(rule.class)
        try {
            if (!isRuleEnabled(rule)) return
            extension.LOG.debug("${extension.prefix} Invoking candidate rule ${ruleClassName}")
            rule.execute(context)
        } catch (EnforcerRuleException e) {
            if (extension.resolvedFailFast.get()) {
                if (isFailOnError(rule)) {
                    throw new BuildException("${toPrefix(context)} An Enforcer rule has failed", e)
                } else {
                    context.logger.warn("${toPrefix(context)} An Enforcer rule has failed\n${e}")
                }
            } else {
                collector.add(new RuleExecutionFailure<EnforcerRule>(rule, e))
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

    protected <RULE extends EnforcerRule> boolean isFailOnError(RULE rule) {
        if (rule.enforcerLevel.isPresent()) {
            return rule.enforcerLevel.get() == EnforcerLevel.ERROR
        }
        return extension.enforcerLevel.get() == EnforcerLevel.ERROR
    }

    protected boolean isFailOnError() {
        return extension.enforcerLevel.get() == EnforcerLevel.ERROR
    }

    @CompileStatic
    static class RuleExecutionFailure<RULE extends EnforcerRule> {
        final RULE rule
        final EnforcerRuleException failure

        private RuleExecutionFailure(RULE rule, EnforcerRuleException failure) {
            this.rule = rule
            this.failure = failure
        }
    }
}
