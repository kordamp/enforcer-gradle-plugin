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
import org.gradle.BuildResult
import org.gradle.api.Project
import org.gradle.api.invocation.Gradle
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
final class BuildEnforcerContext implements EnforcerContext {
    private static final Logger LOG = Logging.getLogger(Project)

    final EnforcerPhase enforcerPhase
    final Gradle gradle
    final BuildResult buildResult
    final Logger logger

    private BuildEnforcerContext(EnforcerPhase enforcerPhase, Gradle gradle, BuildResult buildResult) {
        this.enforcerPhase = enforcerPhase
        this.gradle = gradle
        this.buildResult = buildResult
        this.logger = new BuildEnforcerLogger(LOG)
    }

    @Override
    String toString() {
        "BuildEnforcerContext[phase=${enforcerPhase.name()}, result=${buildResult}]".toString()
    }

    @Override
    Project getProject() {
        gradle.rootProject
    }

    static BuildEnforcerContext beforeBuild(Gradle gradle) {
        return new BuildEnforcerContext(EnforcerPhase.BEFORE_BUILD, gradle, null)
    }

    static BuildEnforcerContext beforeProjects(Gradle gradle) {
        return new BuildEnforcerContext(EnforcerPhase.BEFORE_PROJECTS, gradle, null)
    }

    static BuildEnforcerContext afterProjects(Gradle gradle) {
        return new BuildEnforcerContext(EnforcerPhase.AFTER_PROJECTS, gradle, null)
    }

    static BuildEnforcerContext afterBuild(Gradle gradle, BuildResult buildResult) {
        return new BuildEnforcerContext(EnforcerPhase.AFTER_BUILD, gradle, buildResult)
    }

    private class BuildEnforcerLogger extends EnforcerContextLogger {
        BuildEnforcerLogger(Logger delegate) {
            super(delegate)
        }

        @Override
        protected String getPrefix() {
            '[build-enforcer] '
        }
    }
}
