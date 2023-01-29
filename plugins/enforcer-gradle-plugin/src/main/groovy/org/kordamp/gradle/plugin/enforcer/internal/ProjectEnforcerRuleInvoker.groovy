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
import org.gradle.BuildResult
import org.gradle.api.Project
import org.gradle.api.ProjectEvaluationListener
import org.gradle.api.ProjectState
import org.gradle.api.initialization.Settings
import org.gradle.api.invocation.Gradle
import org.kordamp.gradle.plugin.enforcer.api.EnforcerContext
import org.kordamp.gradle.plugin.enforcer.api.EnforcerExtension
import org.kordamp.gradle.plugin.enforcer.api.EnforcerRule

import static org.kordamp.gradle.plugin.enforcer.api.ProjectEnforcerContext.afterProject
import static org.kordamp.gradle.plugin.enforcer.api.ProjectEnforcerContext.beforeProject

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
class ProjectEnforcerRuleInvoker extends AbstractEnforcerRuleInvoker implements ProjectEvaluationListener {
    private final List<? extends EnforcerRule> rules = []

    ProjectEnforcerRuleInvoker(Gradle gradle, EnforcerExtension extension) {
        super(gradle, extension)
    }

    @Override
    protected <T extends EnforcerRule> T instantiateRule(EnforcerContext context, Class<T> ruleType) {
        return context.project.objects.newInstance(ruleType)
    }

    DefaultProjectEnforcerExtension extension() {
        (DefaultProjectEnforcerExtension) extension
    }

    @Override
    void settingsEvaluated(Settings settings) {
        // noop
    }

    @Override
    void projectsLoaded(Gradle gradle) {
        // noop
    }

    @Override
    void projectsEvaluated(Gradle gradle) {
        // noop
    }

    @Override
    void buildFinished(BuildResult buildResult) {
        extension().helpers.clear()
        rules.clear()
    }

    @Override
    void beforeEvaluate(Project project) {
        EnforcerContext context = beforeProject(project, extension.warnings.get())
        if (!isBuildPhaseEnabled(context)) return

        extension().LOG.debug("${extension().prefix} ${context}")

        List<RuleExecutionFailure<? extends EnforcerRule>> collector = []
        createAndInvokeRules(extension().helpers,
            extension().mergeStrategy.get(),
            context,
            rules,
            collector)

        report(context, collector)
    }

    @Override
    void afterEvaluate(Project project, ProjectState projectState) {
        EnforcerContext context = afterProject(project, projectState, extension.warnings.get())
        if (!isBuildPhaseEnabled(context)) return

        extension().LOG.debug("${extension().prefix} ${context}")

        List<RuleExecutionFailure<? extends EnforcerRule>> collector = []
        maybeCreateAndInvokeRules(context,
            extension().helpers,
            rules,
            collector)

        report(context, collector)
    }
}
