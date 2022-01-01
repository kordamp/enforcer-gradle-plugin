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
package org.kordamp.gradle.plugin.enforcer.api

import groovy.transform.CompileStatic
import org.gradle.api.Action

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
interface ProjectEnforcerExtension extends EnforcerExtension {
    /**
     * Configure rules for this project.
     *
     * Parameter should be loaded from the classpath.
     */
    void configure(Class<? extends Action<? extends ProjectEnforcerExtension>> configurerClass)

    /**
     * Define a rule for this project.
     */
    public <R extends EnforcerRule> void rule(Class<R> ruleType)

    /**
     * Define and configure a rule for this project.
     */
    public <R extends EnforcerRule> void rule(Class<R> ruleType, Action<R> configurer)
}
