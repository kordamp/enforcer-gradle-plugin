/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2020-2023 The author and/or original authors.
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

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.gradle.BuildResult
import org.gradle.api.Project
import org.gradle.api.ProjectEvaluationListener
import org.gradle.api.ProjectState
import org.gradle.api.initialization.Settings
import org.gradle.api.invocation.Gradle
import org.gradle.api.model.ObjectFactory
import org.kordamp.gradle.plugin.enforcer.api.EnforcerContext
import org.kordamp.gradle.plugin.enforcer.api.EnforcerExtension
import org.kordamp.gradle.plugin.enforcer.api.EnforcerRule
import org.kordamp.gradle.plugin.enforcer.api.EnforcerRuleException

import static org.kordamp.gradle.plugin.enforcer.api.BuildEnforcerContext.afterBuild
import static org.kordamp.gradle.plugin.enforcer.api.BuildEnforcerContext.afterProjects
import static org.kordamp.gradle.plugin.enforcer.api.BuildEnforcerContext.beforeBuild
import static org.kordamp.gradle.plugin.enforcer.api.BuildEnforcerContext.beforeProjects
import static org.kordamp.gradle.plugin.enforcer.api.ProjectEnforcerContext.afterProject
import static org.kordamp.gradle.plugin.enforcer.api.ProjectEnforcerContext.beforeProject

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
class BuildEnforcerRuleInvoker extends AbstractEnforcerRuleInvoker implements ProjectEvaluationListener {
    private final List<? extends EnforcerRule> buildRules = []
    private final Map<Project, List<? extends EnforcerRule>> projectRules = [:]
    private final ObjectFactory objects
    private final Settings settings

    BuildEnforcerRuleInvoker(Settings settings, EnforcerExtension extension, ObjectFactory objects) {
        super(settings.gradle, extension)
        this.settings = settings
        this.objects = objects
    }

    @Override
    protected <T extends EnforcerRule> T instantiateRule(EnforcerContext context, Class<T> ruleType) {
        return objects.newInstance(ruleType)
    }

    protected DefaultBuildEnforcerExtension extension() {
        (DefaultBuildEnforcerExtension) extension
    }

    @Override
    void settingsEvaluated(Settings settings) {
        EnforcerContext context = beforeBuild(gradle, settings, extension.warnings.get())
        if (!isBuildPhaseEnabled(context)) return

        extension().LOG.debug("${extension().prefix} ${context}")

        List<RuleExecutionFailure<? extends EnforcerRule>> collector = []
        createAndInvokeRules(extension().buildHelpers,
            extension().mergeStrategy.get(),
            context,
            buildRules,
            collector)

        report(context, collector)
    }

    @Override
    void projectsLoaded(Gradle gradle) {
        EnforcerContext context = beforeProjects(gradle, settings, extension.warnings.get())
        if (!isBuildPhaseEnabled(context)) return

        extension().LOG.debug("${extension().prefix} ${context}")

        List<RuleExecutionFailure<? extends EnforcerRule>> collector = []
        maybeCreateAndInvokeRules(context,
            extension().buildHelpers,
            buildRules,
            collector)

        report(context, collector)
    }

    @Override
    void projectsEvaluated(Gradle gradle) {
        if (!hasKordampBasePluginApplied(gradle.rootProject)) {
            doProjectsEvaluated()
        }
    }

    private void doProjectsEvaluated() {
        EnforcerContext context = afterProjects(gradle, settings, extension.warnings.get())
        if (!isBuildPhaseEnabled(context)) return

        extension().LOG.debug("${extension().prefix} ${context}")

        List<RuleExecutionFailure<? extends EnforcerRule>> collector = []
        maybeCreateAndInvokeRules(
            context,
            extension().buildHelpers,
            buildRules,
            collector)

        report(context, collector)
    }

    @Override
    void buildFinished(BuildResult buildResult) {
        EnforcerContext context = afterBuild(gradle, settings, buildResult, extension.warnings.get())
        if (!isBuildPhaseEnabled(context)) {
            cleanup()
            return
        }

        if (buildResult.failure) {
            // find out if the cause is an EnforcerRuleException
            Throwable t = buildResult.failure
            while (t) {
                if (t instanceof EnforcerRuleException || t instanceof MultipleEnforcerRuleException) {
                    // we're done here
                    return
                }
                t = t.cause
            }
        }

        extension().LOG.debug("${extension().prefix} ${context}")

        try {
            if (buildRules) {
                List<RuleExecutionFailure<? extends EnforcerRule>> collector = []

                for (EnforcerRule rule : buildRules) {
                    invokeRule(context, rule, collector)
                }

                report(context, collector)
            }
        } finally {
            cleanup()
        }
    }

    private void cleanup() {
        extension().buildHelpers.clear()
        extension().projectHelpers.clear()
        buildRules.clear()
        projectRules.clear()
    }

    @Override
    void beforeEvaluate(Project project) {
        EnforcerContext context = beforeProject(project, extension.warnings.get())
        if (!isBuildPhaseEnabled(context)) return

        extension().LOG.debug("${extension().prefix} ${context}")

        List<RuleExecutionFailure<? extends EnforcerRule>> collector = []
        createAndInvokeRules(extension().projectHelpers,
            extension().mergeStrategy.get(),
            context,
            projectRules.computeIfAbsent(project, { k -> [] }),
            collector)

        report(context, collector)
    }

    @Override
    void afterEvaluate(Project project, ProjectState projectState) {
        EnforcerContext context = afterProject(project, projectState, extension.warnings.get())
        if (!isBuildPhaseEnabled(context)) return

        extension().LOG.debug("${extension().prefix} ${context}")

        List<? extends EnforcerRule> rules = projectRules.computeIfAbsent(project, { k -> [] })

        List<RuleExecutionFailure<? extends EnforcerRule>> collector = []
        maybeCreateAndInvokeRules(
            context,
            extension().projectHelpers,
            rules,
            collector)

        report(context, collector)

        if (hasKordampBasePluginApplied(project)) {
            registerAllProjectsEvaluatedListener(project)
        }
    }

    private boolean hasKordampBasePluginApplied(Project project) {
        project.rootProject == project && project.plugins.findPlugin('org.kordamp.gradle.base')
    }

    @CompileDynamic
    private void registerAllProjectsEvaluatedListener(Project project) {
        Class c = Class.forName('org.kordamp.gradle.plugin.enforcer.internal.BuildEnforcerAllProjectsEvaluatedListener')
        def listener = c.getConstructor().newInstance()
        listener.runnable = { -> doProjectsEvaluated() }

        Class m = Class.forName('org.kordamp.gradle.listener.ProjectEvaluationListenerManager')
        m.addAllProjectsEvaluatedListener(project, listener)
    }
}
