/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2020-2022 The author and/or original authors.
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
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ProviderFactory
import org.kordamp.gradle.plugin.enforcer.api.BuildEnforcerExtension
import org.kordamp.gradle.plugin.enforcer.api.EnforcerRule
import org.kordamp.gradle.plugin.enforcer.api.MergeStrategy
import org.kordamp.gradle.plugin.enforcer.api.RepeatableEnforcerRule

import javax.inject.Inject

import static org.apache.commons.lang3.StringUtils.isBlank

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
class DefaultBuildEnforcerExtension extends AbstractEnforcerExtension implements BuildEnforcerExtension {
    final List<DefaultEnforcerRuleHelper> buildHelpers = []
    final List<ProjectEnforcerRuleHelper> projectHelpers = []

    @Inject
    DefaultBuildEnforcerExtension(ObjectFactory objects, ProviderFactory providers) {
        super(objects, providers)
    }

    @Override
    String getPrefix() {
        '[build-enforcer]'
    }

    @Override
    void configure(Class<? extends Action<? extends BuildEnforcerExtension>> configurerClass) {
        if (!configurerClass) {
            throw new IllegalArgumentException("${prefix} Invalid configuration: null configurer class")
        }

        Action<? extends BuildEnforcerExtension> configurer = (Action<? extends BuildEnforcerExtension>) objects.newInstance((Class) configurerClass)
        LOG.debug("${prefix} Configuring with ${configurerClass.name}")
        configurer.execute(this)
    }

    @Override
    <R extends EnforcerRule> void rule(Class<R> ruleType) {
        List<DefaultEnforcerRuleHelper> matchingHelpers = buildHelpers.findAll { h -> h.ruleType == ruleType }

        if (matchingHelpers) {
            for (DefaultEnforcerRuleHelper helper : matchingHelpers) {
                switch (mergeStrategy.get()) {
                    case MergeStrategy.DENY:
                        LOG.debug("${prefix} Cannot override ${ruleType.name}")
                        throw deny(ruleType)
                    case MergeStrategy.OVERRIDE:
                        if (RepeatableEnforcerRule.class.isAssignableFrom(ruleType)) {
                            LOG.debug("${prefix} Adding ${ruleType.name}")
                            buildHelpers.add(new DefaultEnforcerRuleHelper(ruleType))
                        } else {
                            LOG.debug("${prefix} Setting configuration to null on ${ruleType.name}")
                            helper.setAction(null)
                        }
                        break
                    case MergeStrategy.PREPEND:
                    case MergeStrategy.APPEND:
                    case MergeStrategy.DUPLICATE:
                        LOG.debug("${prefix} Adding ${ruleType.name}")
                        buildHelpers.add(new DefaultEnforcerRuleHelper(ruleType))
                        break
                }
            }
        } else {
            LOG.debug("${prefix} Adding ${ruleType.name}")
            buildHelpers.add(new DefaultEnforcerRuleHelper(ruleType))
        }
    }

    @Override
    <R extends EnforcerRule> void rule(Class<R> ruleType, Action<R> configurer) {
        List<DefaultEnforcerRuleHelper> matchingHelpers = buildHelpers.findAll { h -> h.ruleType == ruleType }

        if (matchingHelpers) {
            for (DefaultEnforcerRuleHelper helper : matchingHelpers) {
                switch (mergeStrategy.get()) {
                    case MergeStrategy.DENY:
                        LOG.debug("${prefix} Cannot override configuration of ${ruleType.name}")
                        throw deny(ruleType)
                    case MergeStrategy.OVERRIDE:
                        if (RepeatableEnforcerRule.class.isAssignableFrom(ruleType)) {
                            LOG.debug("${prefix} Adding configured ${ruleType.name}")
                            buildHelpers.add(new DefaultEnforcerRuleHelper(ruleType, configurer))
                        } else {
                            LOG.debug("${prefix} Setting configuration on ${ruleType.name}")
                            helper.setAction(configurer)
                        }
                        break
                    case MergeStrategy.PREPEND:
                        if (RepeatableEnforcerRule.class.isAssignableFrom(ruleType)) {
                            LOG.debug("${prefix} Adding configured ${ruleType.name}")
                            buildHelpers.add(new DefaultEnforcerRuleHelper(ruleType, configurer))
                        } else {
                            LOG.debug("${prefix} Prepending configuration to ${ruleType.name}")
                            helper.prepend(configurer)
                        }
                        break
                    case MergeStrategy.APPEND:
                        if (RepeatableEnforcerRule.class.isAssignableFrom(ruleType)) {
                            LOG.debug("${prefix} Adding configured ${ruleType.name}")
                            buildHelpers.add(new DefaultEnforcerRuleHelper(ruleType, configurer))
                        } else {
                            LOG.debug("${prefix} Appending configuration to ${ruleType.name}")
                            helper.append(configurer)
                        }
                        break
                    case MergeStrategy.DUPLICATE:
                        LOG.debug("${prefix} Adding configured ${ruleType.name}")
                        buildHelpers.add(new DefaultEnforcerRuleHelper(ruleType, configurer))
                        break
                }
            }
        } else {
            LOG.debug("${prefix} Adding configured ${ruleType.name}")
            buildHelpers.add(new DefaultEnforcerRuleHelper(ruleType, configurer))
        }
    }

    @Override
    void allprojects(Action<? extends EnforcerRuleConfiguration> configurer) {
        if (configurer) {
            EnforcerRuleConfiguration c = new ProjectEnforcerRuleConfiguration()
            LOG.debug('${prefix} Configuring rules for all projects')
            configurer.execute(c)
        }
    }

    @Override
    void project(String projectPath, Action<? extends EnforcerRuleConfiguration> configurer) {
        if (configurer) {
            if (isBlank(projectPath)) {
                throw new IllegalArgumentException('${prefix} Invalid configuration: empty projectPath')
            }
            EnforcerRuleConfiguration c = new ProjectEnforcerRuleConfiguration(projectPath)
            LOG.debug("${prefix} Configuring rules for project ${projectPath}")
            configurer.execute(c)
        }
    }

    @Override
    void projects(List<String> projectPaths, Action<? extends EnforcerRuleConfiguration> configurer) {
        if (configurer) {
            if (!projectPaths || projectPaths.any { isBlank(it) }) {
                throw new IllegalArgumentException('${prefix} Invalid configuration: empty projectPaths or empty element. ' + projectPaths)
            }
            EnforcerRuleConfiguration c = new ProjectEnforcerRuleConfiguration(projectPaths)
            LOG.debug("${prefix} Configuring rules for projects in ${projectPaths}")
            configurer.execute(c)
        }
    }

    private class ProjectEnforcerRuleConfiguration implements EnforcerRuleConfiguration {
        private final List<String> projectPaths = []

        ProjectEnforcerRuleConfiguration() {
            this(['*'])
        }

        ProjectEnforcerRuleConfiguration(String projectPath) {
            this([projectPath])
        }

        ProjectEnforcerRuleConfiguration(List<String> projectPaths) {
            this.projectPaths.addAll(projectPaths)
        }

        @Override
        <R extends EnforcerRule> void rule(Class<R> ruleType) {
            List<ProjectEnforcerRuleHelper> helpers = projectHelpers.findAll { h -> h.ruleType == ruleType }

            if (helpers) {
                boolean matches = false
                for (ProjectEnforcerRuleHelper helper : helpers) {
                    for (String projectPath : projectPaths) {
                        if (helper.match(projectPath)) {
                            matches = true
                            switch (mergeStrategy.get()) {
                                case MergeStrategy.DENY:
                                    LOG.debug("${prefix} Cannot override configuration of ${ruleType.name} for project ${projectPath}")
                                    throw deny(ruleType)
                                case MergeStrategy.OVERRIDE:
                                    if (RepeatableEnforcerRule.class.isAssignableFrom(ruleType)) {
                                        LOG.debug("${prefix} Adding ${ruleType.name} for project ${projectPath}")
                                        helpers.add(new ProjectEnforcerRuleHelper(projectPath, ruleType))
                                    } else {
                                        LOG.debug("${prefix} Setting configuration to null on ${ruleType.name} for project ${projectPath}")
                                        helper.setAction(null)
                                    }
                                    break
                                case MergeStrategy.PREPEND:
                                case MergeStrategy.APPEND:
                                case MergeStrategy.DUPLICATE:
                                    LOG.debug("${prefix} Adding ${ruleType.name} for project ${projectPath}")
                                    projectHelpers.add(new ProjectEnforcerRuleHelper(projectPath, ruleType))
                                    break
                            }
                        }
                    }
                }
                if (!matches) {
                    for (String projectPath : projectPaths) {
                        LOG.debug("${prefix} Adding ${ruleType.name} for project ${projectPath}")
                        projectHelpers.add(new ProjectEnforcerRuleHelper(projectPath, ruleType))
                    }
                }
            } else {
                for (String projectPath : projectPaths) {
                    LOG.debug("${prefix} Adding ${ruleType.name} for project ${projectPath}")
                    projectHelpers.add(new ProjectEnforcerRuleHelper(projectPath, ruleType))
                }
            }
        }

        @Override
        <R extends EnforcerRule> void rule(Class<R> ruleType, Action<R> configurer) {
            List<ProjectEnforcerRuleHelper> helpers = projectHelpers.findAll { h -> h.ruleType == ruleType }

            if (helpers) {
                boolean matches = false
                for (ProjectEnforcerRuleHelper helper : helpers) {
                    for (String projectPath : projectPaths) {
                        if (helper.match(projectPath)) {
                            matches = true
                            switch (mergeStrategy.get()) {
                                case MergeStrategy.DENY:
                                    LOG.debug("${prefix} Cannot override configuration of ${ruleType.name} for project ${projectPath}")
                                    throw deny(ruleType)
                                case MergeStrategy.OVERRIDE:
                                    if (RepeatableEnforcerRule.class.isAssignableFrom(ruleType)) {
                                        LOG.debug("${prefix} Adding ${ruleType.name} for project ${projectPath}")
                                        helpers.add(new ProjectEnforcerRuleHelper(projectPath, ruleType))
                                    } else {
                                        LOG.debug("${prefix} Setting configuration to null on ${ruleType.name} for project ${projectPath}")
                                        helper.setAction(null)
                                    }
                                    break
                                    LOG.debug("${prefix} Setting configuration on ${ruleType.name} for project ${projectPath}")
                                    helper.setAction(configurer)
                                    break
                                case MergeStrategy.PREPEND:
                                    if (RepeatableEnforcerRule.class.isAssignableFrom(ruleType) || (helper.isAny() && !helper.isAny(projectPath))) {
                                        LOG.debug("${prefix} Adding configured ${ruleType.name} for project ${projectPath}")
                                        projectHelpers.add(new ProjectEnforcerRuleHelper(projectPath, ruleType, configurer))
                                    } else {
                                        LOG.debug("${prefix} Prepending configuration to ${ruleType.name} for project ${projectPath}")
                                        helper.prepend(projectPath, configurer)
                                    }
                                    break
                                case MergeStrategy.APPEND:
                                case MergeStrategy.DUPLICATE:
                                    if (RepeatableEnforcerRule.class.isAssignableFrom(ruleType) || (helper.isAny() && !helper.isAny(projectPath))) {
                                        LOG.debug("${prefix} Adding configured ${ruleType.name} for project ${projectPath}")
                                        projectHelpers.add(new ProjectEnforcerRuleHelper(projectPath, ruleType, configurer))
                                    } else {
                                        LOG.debug("${prefix} Appending configuration to ${ruleType.name} for project ${projectPath}")
                                        helper.append(projectPath, configurer)
                                    }
                                    break
                            }
                        }
                    }
                }
                if (!matches) {
                    for (String projectPath : projectPaths) {
                        LOG.debug("${prefix} Adding configured ${ruleType.name} for project ${projectPath}")
                        projectHelpers.add(new ProjectEnforcerRuleHelper(projectPath, ruleType, configurer))
                    }
                }
            } else {
                for (String projectPath : projectPaths) {
                    LOG.debug("${prefix} Adding configured ${ruleType.name} for project ${projectPath}")
                    projectHelpers.add(new ProjectEnforcerRuleHelper(projectPath, ruleType, configurer))
                }
            }
        }
    }
}
