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
package org.kordamp.gradle.plugin.enforcer

import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import org.kordamp.gradle.plugin.enforcer.api.ProjectEnforcerExtension
import org.kordamp.gradle.plugin.enforcer.internal.DefaultProjectEnforcerExtension
import org.kordamp.gradle.plugin.enforcer.internal.ProjectEnforcerRuleInvoker

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
class ProjectEnforcerPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        if (project.gradle.startParameter.logLevel != LogLevel.QUIET) {
            project.gradle.sharedServices
                .registerIfAbsent('enforcer-banner', Banner, { spec -> })
                .get().display(project)
        }

        ProjectEnforcerExtension extension = project.extensions.create(
            ProjectEnforcerExtension,
            'enforce',
            DefaultProjectEnforcerExtension,
            project)

        project.gradle.addListener(new ProjectEnforcerRuleInvoker(project.name, project.gradle, extension))
    }
}
