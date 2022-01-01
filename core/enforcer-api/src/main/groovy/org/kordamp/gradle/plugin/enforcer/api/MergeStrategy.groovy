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

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
enum MergeStrategy {
    /** The last configuration action wins. All previous configuration(s) (if any) will be discarded. */
    OVERRIDE,
    /** Executes all configurations on a single rule instance, in FIFO order. */
    APPEND,
    /**  Executes all configurations on a single rule instance, in LIFO order. */
    PREPEND,
    /**  Creates a duplicate rule with no shared configuration. */
    DUPLICATE,
    /**  Does not allow configuration to be changed. First (if any) wins. */
    DENY
}