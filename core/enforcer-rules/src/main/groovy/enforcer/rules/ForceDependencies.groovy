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
package enforcer.rules

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

import static org.kordamp.gradle.plugin.enforcer.api.EnforcerPhase.BEFORE_PROJECT
import static org.kordamp.gradle.plugin.enforcer.api.EnforcerPhase.BEFORE_PROJECTS

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
class ForceDependencies extends AbstractFilteringEnforcerRule {
    final ListProperty<Object> dependencies

    @Inject
    ForceDependencies(ObjectFactory objects) {
        super(objects, [BEFORE_PROJECTS, BEFORE_PROJECT] as EnforcerPhase[])
        dependencies = objects.listProperty(Object).convention([])
    }

    @Override
    protected void doExecute(EnforcerContext context) throws EnforcerRuleException {
        if (context.enforcerPhase == BEFORE_PROJECTS) {
            context.project.configurations.all(resolutionStrategyConfigurer())
            for (Project project : context.project.childProjects.values()) {
                project.configurations.all(resolutionStrategyConfigurer())
            }
        } else {
            context.project.configurations.all(resolutionStrategyConfigurer())
        }
    }

    private Action<Configuration> resolutionStrategyConfigurer() {
        new Action<Configuration>() {
            @Override
            void execute(Configuration c) {
                c.resolutionStrategy.force(dependencies.get())
            }
        }
    }
}
