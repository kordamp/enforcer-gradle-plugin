/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2020-2025 The author and/or original authors.
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
import org.gradle.api.Project
import org.kordamp.gradle.annotations.DependsOn
import org.kordamp.gradle.listener.AllProjectsEvaluatedListener

import javax.inject.Named

/**
 * @author Andres Almiray
 * @since 0.8.0
 */
@CompileStatic
@DependsOn(['base','*'])
@Named('build-enforcer')
class BuildEnforcerAllProjectsEvaluatedListener implements AllProjectsEvaluatedListener {
    Runnable runnable

    @Override
    void allProjectsEvaluated(Project project) {
        runnable?.run()
    }
}
