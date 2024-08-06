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
package org.kordamp.gradle.plugin.enforcer.api

import groovy.transform.CompileStatic

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
enum EnforcerPhase {
    /** After `Settings` have been evaluated and before any projects are loaded. */
    BEFORE_BUILD,
    /** When projects have been loaded and before any is evaluated. */
    BEFORE_PROJECTS,
    /** When a project is about to be evaluated. */
    BEFORE_PROJECT,
    /** When a project has been evaluated. */
    AFTER_PROJECT,
    /** When all projects have been evaluated. */
    AFTER_PROJECTS,
    /** When the build finishes. */
    AFTER_BUILD
}