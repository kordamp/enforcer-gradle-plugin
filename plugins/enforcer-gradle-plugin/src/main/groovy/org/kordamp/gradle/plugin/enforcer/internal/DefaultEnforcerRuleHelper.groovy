/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2020-2022 The author and/or original authors.
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
import org.kordamp.gradle.plugin.enforcer.api.EnforcerContext
import org.kordamp.gradle.plugin.enforcer.api.EnforcerRule

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@ToString(includeNames = true)
@PackageScope
@CompileStatic
class DefaultEnforcerRuleHelper implements EnforcerRuleHelper {
    final Class<? extends EnforcerRule> ruleType
    final List<Action<? extends EnforcerRule>> actions = []

    DefaultEnforcerRuleHelper(Class<? extends EnforcerRule> ruleType) {
        this.ruleType = ruleType
    }

    DefaultEnforcerRuleHelper(Class<? extends EnforcerRule> ruleType, Action<? extends EnforcerRule> action) {
        this.ruleType = ruleType
        this.actions.add(action)
    }

    @Override
    void setAction(Action<? extends EnforcerRule> action) {
        actions.clear()
        if (action) actions.add(action)
    }

    @Override
    boolean match(EnforcerContext context) {
        true
    }

    void prepend(Action<? extends EnforcerRule> action) {
        actions.add(action)
    }

    void append(Action<? extends EnforcerRule> action) {
        actions.add(action)
    }
}