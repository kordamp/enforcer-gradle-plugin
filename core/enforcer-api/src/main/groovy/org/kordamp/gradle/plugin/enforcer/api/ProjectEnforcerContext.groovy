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
package org.kordamp.gradle.plugin.enforcer.api

import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.ProjectState
import org.gradle.api.logging.Logger

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
final class ProjectEnforcerContext implements EnforcerContext {
    final EnforcerPhase enforcerPhase
    final Project project
    final ProjectState projectState
    final Logger logger

    private ProjectEnforcerContext(EnforcerPhase enforcerPhase, Project project, ProjectState projectState) {
        this.enforcerPhase = enforcerPhase
        this.project = project
        this.projectState = projectState
        this.logger = new ProjectEnforcerLogger(project.logger)
    }

    @Override
    String toString() {
        "ProjectEnforcerContext[phase=${enforcerPhase.name()}, project=${project.path}, state=${project}]".toString()
    }

    @Override
    Project getProject() {
        project
    }

    static ProjectEnforcerContext beforeProject(Project project) {
        return new ProjectEnforcerContext(EnforcerPhase.BEFORE_PROJECT, project, null)
    }

    static ProjectEnforcerContext afterProject(Project project, ProjectState projectState) {
        return new ProjectEnforcerContext(EnforcerPhase.AFTER_PROJECT, project, projectState)
    }

    private class ProjectEnforcerLogger extends EnforcerContextLogger {
        ProjectEnforcerLogger(Logger delegate) {
            super(delegate)
        }

        @Override
        protected String getPrefix() {
            "[project-enforcer ${project.path}] "
        }
    }
}
