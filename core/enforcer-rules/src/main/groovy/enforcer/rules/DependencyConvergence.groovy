/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2020-2021 The author and/or original authors.
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
package enforcer.rules

import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.util.GradleVersion
import org.kordamp.gradle.plugin.enforcer.api.EnforcerContext
import org.kordamp.gradle.plugin.enforcer.api.EnforcerPhase
import org.kordamp.gradle.plugin.enforcer.api.EnforcerRuleException

import javax.inject.Inject

import static org.kordamp.gradle.plugin.enforcer.api.EnforcerPhase.AFTER_PROJECT
import static org.kordamp.gradle.plugin.enforcer.api.EnforcerPhase.AFTER_PROJECTS
import static org.kordamp.gradle.plugin.enforcer.api.EnforcerPhase.BEFORE_PROJECT
import static org.kordamp.gradle.plugin.enforcer.api.EnforcerPhase.BEFORE_PROJECTS

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
class DependencyConvergence extends AbstractFilteringEnforcerRule {
    final Property<Boolean> failOnDynamicVersions
    final Property<Boolean> failOnChangingVersions
    final Property<Boolean> failOnNonReproducibleResolution
    final Property<Boolean> activateDependencyLocking
    final Property<Boolean> deactivateDependencyLocking

    @Inject
    DependencyConvergence(ObjectFactory objects) {
        super(objects, [BEFORE_PROJECTS, BEFORE_PROJECT, AFTER_PROJECT, AFTER_PROJECTS] as EnforcerPhase[])
        failOnDynamicVersions = objects.property(Boolean).convention(false)
        failOnChangingVersions = objects.property(Boolean).convention(false)
        failOnNonReproducibleResolution = objects.property(Boolean).convention(false)
        activateDependencyLocking = objects.property(Boolean).convention(false)
        deactivateDependencyLocking = objects.property(Boolean).convention(false)
    }

    @Override
    protected void doExecute(EnforcerContext context) throws EnforcerRuleException {
        switch (context.enforcerPhase) {
            case BEFORE_PROJECTS:
                // configure
                context.project.configurations.all(resolutionStrategyConfigurer())
                for (Project project : context.project.childProjects.values()) {
                    project.configurations.all(resolutionStrategyConfigurer())
                }
                break
            case BEFORE_PROJECT:
                // configure
                context.project.configurations.all(resolutionStrategyConfigurer())
                break
            case AFTER_PROJECT:
                // resolve
                context.project.configurations.each { Configuration c ->
                    if (c.canBeResolved) c.copyRecursive().resolve()
                }
                break
            case AFTER_PROJECTS:
                // resolve
                context.project.configurations.each { Configuration c ->
                    if (c.canBeResolved) c.copyRecursive().resolve()
                }
                for (Project project : context.project.childProjects.values()) {
                    project.configurations.each { Configuration c ->
                        if (c.canBeResolved) c.copyRecursive().resolve()
                    }
                }
                break
            default:
                break
        }
    }

    private Action<Configuration> resolutionStrategyConfigurer() {
        new Action<Configuration>() {
            @Override
            void execute(Configuration c) {
                c.resolutionStrategy.failOnVersionConflict()
                if (activateDependencyLocking.get()) c.resolutionStrategy.activateDependencyLocking()
                if (GradleVersion.current() >= GradleVersion.version('6.0')) {
                    if (deactivateDependencyLocking.get()) c.resolutionStrategy.deactivateDependencyLocking()
                }
                if (GradleVersion.current() >= GradleVersion.version('6.1')) {
                    if (failOnDynamicVersions.get()) c.resolutionStrategy.failOnDynamicVersions()
                    if (failOnChangingVersions.get()) c.resolutionStrategy.failOnChangingVersions()
                    if (failOnNonReproducibleResolution.get()) c.resolutionStrategy.failOnNonReproducibleResolution()
                }
            }
        }
    }
}
