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
package org.kordamp.gradle.plugin.enforcer.api

import groovy.transform.CompileStatic
import org.gradle.BuildResult
import org.gradle.api.Project
import org.gradle.api.initialization.Settings
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
    final Settings settings
    final BuildResult buildResult
    final Logger logger
    final boolean warnings

    private BuildEnforcerContext(EnforcerPhase enforcerPhase, Gradle gradle, Settings settings, BuildResult buildResult, boolean warnings) {
        this.enforcerPhase = enforcerPhase
        this.gradle = gradle
        this.settings = settings
        this.buildResult = buildResult
        this.logger = new BuildEnforcerLogger(LOG)
        this.warnings = warnings
    }

    @Override
    String toString() {
        "BuildEnforcerContext[phase=${enforcerPhase.name()}, result=${buildResult}]".toString()
    }

    @Override
    Project getProject() {
        gradle.rootProject
    }

    @Override
    File getBasedir() {
        settings.settingsDir
    }

    @Override
    boolean isWarnings() {
        warnings
    }

    static BuildEnforcerContext beforeBuild(Gradle gradle, Settings settings, boolean warnings) {
        return new BuildEnforcerContext(EnforcerPhase.BEFORE_BUILD, gradle, settings, null, warnings)
    }

    static BuildEnforcerContext beforeProjects(Gradle gradle, Settings settings, boolean warnings) {
        return new BuildEnforcerContext(EnforcerPhase.BEFORE_PROJECTS, gradle, settings, null, warnings)
    }

    static BuildEnforcerContext afterProjects(Gradle gradle, Settings settings, boolean warnings) {
        return new BuildEnforcerContext(EnforcerPhase.AFTER_PROJECTS, gradle, settings, null, warnings)
    }

    static BuildEnforcerContext afterBuild(Gradle gradle, Settings settings, BuildResult buildResult, boolean warnings) {
        return new BuildEnforcerContext(EnforcerPhase.AFTER_BUILD, gradle, settings, buildResult, warnings)
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
