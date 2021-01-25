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

import groovy.transform.Canonical
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.kordamp.gradle.plugin.enforcer.api.EnforcerContext
import org.kordamp.gradle.plugin.enforcer.api.EnforcerPhase
import org.kordamp.gradle.plugin.enforcer.api.EnforcerRuleException

import javax.inject.Inject

import static org.apache.commons.lang3.StringUtils.isBlank
import static org.kordamp.gradle.plugin.enforcer.api.EnforcerPhase.BEFORE_PROJECT
import static org.kordamp.gradle.plugin.enforcer.api.EnforcerPhase.BEFORE_PROJECTS

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
class ExcludeDependencies extends AbstractFilteringEnforcerRule {
    final ListProperty<Dependency> dependencies

    @Inject
    ExcludeDependencies(ObjectFactory objects) {
        super(objects, [BEFORE_PROJECTS, BEFORE_PROJECT] as EnforcerPhase[])
        dependencies = objects.listProperty(Dependency).convention([])
    }

    @Override
    protected void doExecute(EnforcerContext context) throws EnforcerRuleException {
        if (!dependencies.get().isEmpty()) {
            if (context.enforcerPhase == BEFORE_PROJECTS) {
                context.project.configurations.all(exclusionsConfigurer())
                for (Project project : context.project.childProjects.values()) {
                    project.configurations.all(exclusionsConfigurer())
                }
            } else {
                context.project.configurations.all(exclusionsConfigurer())
            }
        }
    }

    private Action<Configuration> exclusionsConfigurer() {
        new Action<Configuration>() {
            @Override
            @CompileDynamic
            void execute(Configuration c) {
                dependencies.get().each { dependency ->
                    c.exclude(dependency.asMap())
                }
            }
        }
    }

    void exclude(String str) {
        dependencies.add(parseDependency(str))
    }

    void exclude(Map<String, String> map) {
        dependencies.add(parseDependency(map))
    }

    @Canonical
    static class Dependency {
        final String groupId
        final String artifactId

        Map<String, String> asMap() {
            [
                group : groupId,
                module: artifactId
            ]
        }
    }

    private Dependency parseDependency(String str) {
        if (isBlank(str)) {
            throw illegalArgumentException('Unparseable dependency definition: empty input')
        }

        String[] parts = str.split(':')
        if (parts.length == 2 || parts.length == 3) {
            if (isBlank(parts[0])) {
                throw illegalArgumentException('Invalid dependency definition: empty groupId. ' + str)
            }
            if (isBlank(parts[1])) {
                throw illegalArgumentException('Invalid dependency definition: empty artifactId. ' + str)
            }
            return new Dependency(parts[0].trim(), parts[1].trim())
        } else {
            throw illegalArgumentException('Invalid dependency definition. ' + str)
        }
    }

    private Dependency parseDependency(Map<String, String> map) {
        if (map.isEmpty()) {
            throw illegalArgumentException('Unparseable dependency definition: empty input')
        }

        String groupId = map.groupId
        String artifactId = map.artifactId

        if (isBlank(groupId)) {
            throw illegalArgumentException('Invalid dependency definition: empty groupId. ' + map)
        }
        if (isBlank(artifactId)) {
            throw illegalArgumentException('Invalid dependency definition: empty artifactId. ' + map)
        }

        return new Dependency(groupId.trim(), artifactId.trim())
    }
}
