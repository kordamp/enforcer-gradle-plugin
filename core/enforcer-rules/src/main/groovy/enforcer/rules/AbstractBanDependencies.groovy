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
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.kordamp.gradle.plugin.enforcer.api.EnforcerContext
import org.kordamp.gradle.plugin.enforcer.api.EnforcerPhase
import org.kordamp.gradle.plugin.enforcer.api.EnforcerRuleException

import static org.apache.commons.lang3.StringUtils.isNotBlank

/**
 * Abstract Rule for banning dependencies.
 *
 * Adapted from {@code org.apache.maven.plugins.enforcer.AbstractBanDependencies}.
 * Original author: <a href="mailto:brianf@apache.org">Brian Fox</a>
 *
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
abstract class AbstractBanDependencies extends AbstractStandardEnforcerRule {
    /**
     * Optional list of dependency configurations to search.
     */
    final ListProperty<String> configurations

    AbstractBanDependencies(ObjectFactory objects) {
        this(objects, EnforcerPhase.values())
    }

    AbstractBanDependencies(ObjectFactory objects, EnforcerPhase[] allowedPhases) {
        super(objects, allowedPhases)
        configurations = objects.listProperty(String).convention([])
    }

    @Override
    protected void doExecute(EnforcerContext context) throws EnforcerRuleException {
        // get the list of dependencies
        Set<ResolvedArtifact> dependencies = getDependenciesToCheck(context)

        // look for banned dependencies
        Set<ResolvedArtifact> foundExcludes = checkDependencies(context, dependencies)

        // if any are found, fail the check but list all of them
        if (foundExcludes != null && !foundExcludes.isEmpty()) {
            String message = message.orNull

            StringBuilder buf = new StringBuilder()
            if (isNotBlank(message)) {
                buf.append(message).append(System.lineSeparator())
            }
            for (ResolvedArtifact artifact : foundExcludes) {
                buf.append(getErrorMessage(artifact))
            }
            message = buf.append("Disable this rule temporarily with -D${resolveClassName()}.enabled=false and")
                .append(System.lineSeparator())
                .append("invoke 'dependencies' to locate the source of the banned dependencies.").toString()

            throw fail(message)
        }
    }

    protected CharSequence getErrorMessage(ResolvedArtifact artifact) {
        return 'Found Banned Dependency: ' + toString(artifact) + System.lineSeparator()
    }

    protected String toString(ResolvedArtifact artifact) {
        StringBuilder b = new StringBuilder(artifact.moduleVersion.id.group)
            .append(':')
            .append(artifact.moduleVersion.id.name)
            .append(':')
            .append(artifact.moduleVersion.id.version)
        if (isNotBlank(artifact.classifier)) {
            b.append(':')
                .append(artifact.classifier)
        }
        b.toString()
    }

    protected Set<ResolvedArtifact> getDependenciesToCheck(EnforcerContext context) {
        Set<ResolvedArtifact> artifacts = []

        context.logger.info("Configurations to be checked (if empty then all resolvable configurations will be checked): ${configurations.get()}")

        context.project.configurations.each { Configuration c ->
            handleConfiguration(context, context.project, c, artifacts)
        }
        for (Project project : context.project.childProjects.values()) {
            project.configurations.each { Configuration c ->
                handleConfiguration(context, project, c, artifacts)
            }
        }

        artifacts
    }

    protected void handleConfiguration(EnforcerContext context, Project project, Configuration configuration, Set<ResolvedArtifact> artifacts) {
        if (!(configurations.get().empty) && !(configurations.get().contains(configuration.name))) {
            return
        }

        String prefix = project.path == ':' ? ':' : project.path + ':'
        if (!configuration.canBeResolved) {
            context.logger.debug("Configuration '${prefix}${configuration.name}' cannot be resolved. Skipping check.")
            return
        }

        Configuration cfg = configuration.copyRecursive()
        context.logger.info("Resolving configuration ${prefix}${cfg.name}.")
        cfg.resolve()

        context.logger.info("Configuration ${prefix}${cfg.name} contains ${cfg.resolvedConfiguration.resolvedArtifacts.size()} artifacts.")
        artifacts.addAll(cfg.resolvedConfiguration.resolvedArtifacts)
    }

    protected abstract Set<ResolvedArtifact> checkDependencies(EnforcerContext context, Set<ResolvedArtifact> artifacts)
}
