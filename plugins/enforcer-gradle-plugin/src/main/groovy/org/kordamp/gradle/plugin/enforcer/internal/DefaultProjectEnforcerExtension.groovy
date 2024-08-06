/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2020-2024 The author and/or original authors.
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
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ProviderFactory
import org.kordamp.gradle.plugin.enforcer.api.EnforcerExtension
import org.kordamp.gradle.plugin.enforcer.api.EnforcerRule
import org.kordamp.gradle.plugin.enforcer.api.MergeStrategy
import org.kordamp.gradle.plugin.enforcer.api.ProjectEnforcerExtension
import org.kordamp.gradle.plugin.enforcer.api.RepeatableEnforcerRule

import javax.inject.Inject

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
class DefaultProjectEnforcerExtension extends AbstractEnforcerExtension implements ProjectEnforcerExtension {
    final Project project
    final List<DefaultEnforcerRuleHelper> helpers = []

    @Inject
    DefaultProjectEnforcerExtension(Project project) {
        super(project.objects, project.providers)
        this.project = project
    }

    @Override
    String getPrefix() {
        "[project-enforcer ${project.path}]"
    }

    @Override
    void configure(Class<? extends Action<? extends ProjectEnforcerExtension>> configurerClass) {
        if (!configurerClass) {
            throw new IllegalArgumentException("${prefix} Invalid configuration: null configurer class")
        }

        Action<? extends EnforcerExtension> configurer = (Action<? extends EnforcerExtension>) objects.newInstance((Class) configurerClass)
        LOG.debug("${prefix} Configuring with ${configurerClass.name}")
        configurer.execute(this)
    }

    @Override
    <R extends EnforcerRule> void rule(Class<R> ruleType) {
        List<DefaultEnforcerRuleHelper> matchingHelpers = helpers.findAll { h -> h.ruleType == ruleType }

        if (matchingHelpers) {
            for (DefaultEnforcerRuleHelper helper : matchingHelpers) {
                switch (mergeStrategy.get()) {
                    case MergeStrategy.DENY:
                        LOG.debug("${prefix} Cannot override ${ruleType.name}")
                        throw deny(ruleType)
                    case MergeStrategy.OVERRIDE:
                        if (RepeatableEnforcerRule.class.isAssignableFrom(ruleType)) {
                            LOG.debug("${prefix} Adding ${ruleType.name}")
                            helpers.add(new DefaultEnforcerRuleHelper(ruleType))
                        } else {
                            LOG.debug("${prefix} Setting configuration to null on ${ruleType.name}")
                            helper.setAction(null)
                        }
                        break
                    case MergeStrategy.PREPEND:
                    case MergeStrategy.APPEND:
                    case MergeStrategy.DUPLICATE:
                        LOG.debug("${prefix} Adding ${ruleType.name}")
                        helpers.add(new DefaultEnforcerRuleHelper(ruleType))
                        break
                }
            }
        } else {
            LOG.debug("${prefix} Adding ${ruleType.name}")
            helpers.add(new DefaultEnforcerRuleHelper(ruleType))
        }
    }

    @Override
    <R extends EnforcerRule> void rule(Class<R> ruleType, Action<R> configurer) {
        List<DefaultEnforcerRuleHelper> matchingHelpers = helpers.findAll { h -> h.ruleType == ruleType }

        if (matchingHelpers) {
            for (DefaultEnforcerRuleHelper helper : matchingHelpers) {
                switch (mergeStrategy.get()) {
                    case MergeStrategy.DENY:
                        LOG.debug("${prefix} Cannot override configuration of ${ruleType.name}")
                        throw deny(ruleType)
                    case MergeStrategy.OVERRIDE:
                        if (RepeatableEnforcerRule.class.isAssignableFrom(ruleType)) {
                            LOG.debug("${prefix} Adding configured ${ruleType.name}")
                            helpers.add(new DefaultEnforcerRuleHelper(ruleType, configurer))
                        } else {
                            LOG.debug("${prefix} Setting configuration on ${ruleType.name}")
                            helper.setAction(configurer)
                        }
                        break
                    case MergeStrategy.PREPEND:
                        if (RepeatableEnforcerRule.class.isAssignableFrom(ruleType)) {
                            LOG.debug("${prefix} Adding configured ${ruleType.name}")
                            helpers.add(new DefaultEnforcerRuleHelper(ruleType, configurer))
                        } else {
                            LOG.debug("${prefix} Prepending configuration to ${ruleType.name}")
                            helper.prepend(configurer)
                        }
                        break
                    case MergeStrategy.APPEND:
                        if (RepeatableEnforcerRule.class.isAssignableFrom(ruleType)) {
                            LOG.debug("${prefix} Adding configured ${ruleType.name}")
                            helpers.add(new DefaultEnforcerRuleHelper(ruleType, configurer))
                        } else {
                            LOG.debug("${prefix} Appending configuration to ${ruleType.name}")
                            helper.append(configurer)
                        }
                        break
                    case MergeStrategy.DUPLICATE:
                        LOG.debug("${prefix} Adding configured ${ruleType.name}")
                        helpers.add(new DefaultEnforcerRuleHelper(ruleType, configurer))
                        break
                }
            }
        } else {
            LOG.debug("${prefix} Adding configured ${ruleType.name}")
            helpers.add(new DefaultEnforcerRuleHelper(ruleType, configurer))
        }
    }
}
