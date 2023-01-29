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

import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import groovy.transform.ToString
import org.gradle.api.Action
import org.gradle.api.Project
import org.kordamp.gradle.plugin.enforcer.api.EnforcerContext
import org.kordamp.gradle.plugin.enforcer.api.EnforcerRule

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@ToString(includeNames = true)
@PackageScope
@CompileStatic
class ProjectEnforcerRuleHelper implements EnforcerRuleHelper {
    final String projectPath
    final Class<? extends EnforcerRule> ruleType
    final List<Action<? extends EnforcerRule>> projectActions = []
    final List<Action<? extends EnforcerRule>> globalActions = []

    ProjectEnforcerRuleHelper(String projectPath, Class<? extends EnforcerRule> ruleType) {
        this.projectPath = projectPath
        this.ruleType = ruleType
    }

    ProjectEnforcerRuleHelper(String projectPath, Class<? extends EnforcerRule> ruleType, Action<? extends EnforcerRule> action) {
        this.projectPath = projectPath
        this.ruleType = ruleType
        this.projectActions.add(action)
    }

    void setAction(Action<? extends EnforcerRule> action) {
        projectActions.clear()
        globalActions.clear()
        if (action) projectActions.add(action)
    }

    @Override
    List<Action<? extends EnforcerRule>> getActions() {
        globalActions + projectActions
    }

    void prepend(String path, Action<? extends EnforcerRule> action) {
        if (isAny(path)) {
            globalActions.add(0, action)
        } else {
            projectActions.add(0, action)
        }
    }

    void append(String path, Action<? extends EnforcerRule> action) {
        if (isAny(path)) {
            globalActions.add(action)
        } else {
            projectActions.add(action)
        }
    }

    @Override
    boolean match(EnforcerContext context) {
        match(context.project)
    }

    boolean match(Project project) {
        match(project.path)
    }

    boolean match(String path) {
        isAny(projectPath) || exactMatch(path)
    }

    boolean exactMatch(String path) {
        exactMatch(projectPath, path)
    }

    boolean isAny() {
        isAny(projectPath)
    }

    static boolean exactMatch(String path1, String path2) {
        path1 == path2
    }

    static boolean isAny(String path) {
        '*' == path
    }
}
