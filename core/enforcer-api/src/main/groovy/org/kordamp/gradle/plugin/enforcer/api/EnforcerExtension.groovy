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
import org.gradle.api.provider.Property

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
interface EnforcerExtension extends EnforcerRuleConfiguration {
    /**
     * Whether enforcer behavior is enabled or not. Defaults to {@code true}.
     */
    Property<Boolean> getEnabled()

    /**
     * Whether the enforce should fail the build on the first rule violation. Defaults to {@code true}.
     */
    Property<Boolean> getFailFast()

    void setMergeStrategy(MergeStrategy mergeStrategy)

    void setMergeStrategy(String mergeStrategy)

    void setEnforcerLevel(EnforcerLevel level)

    void setEnforcerLevel(String level)
}
