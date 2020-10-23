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
package org.kordamp.gradle.plugin.enforcer

import org.gradle.api.Action
import org.gradle.api.reflect.TypeOf
import org.kordamp.gradle.plugin.enforcer.api.BuildEnforcerExtension
import org.kordamp.gradle.plugin.enforcer.api.EnforcerRule
import org.kordamp.gradle.plugin.enforcer.api.EnforcerRuleConfiguration
import org.kordamp.gradle.plugin.enforcer.api.ProjectEnforcerExtension

/**
 * Creates an instance of [TypeOf] for the given parameterized type.
 *
 * @param T the type
 * @return the [TypeOf] that captures the generic type of the given parameterized type
 */
inline fun <reified T> typeOf(): TypeOf<T> =
        object : TypeOf<T>() {}

/**
 * Configure rules for this extension.
 *
 * Parameter should be loaded from the classpath.
 */
inline fun <reified T : Action<BuildEnforcerExtension>> BuildEnforcerExtension.configure(): Unit =
        typeOf<T>().let { type ->
            configure(type.concreteClass)
        }

/**
 * Configure rules for this project.
 *
 * Parameter should be loaded from the classpath.
 */
inline fun <reified T : Action<ProjectEnforcerExtension>> ProjectEnforcerExtension.configure(): Unit =
        typeOf<T>().let { type ->
            configure(type.concreteClass)
        }

/**
 * Define a rule
 */
inline fun <reified T : EnforcerRule> EnforcerRuleConfiguration.rule(): Unit =
        typeOf<T>().let { type ->
            rule(type.concreteClass)
        }

/**
 * Define and configure a rule
 */
inline fun <reified T : EnforcerRule> EnforcerRuleConfiguration.rule(noinline configuration: T.() -> Unit): Unit =
        typeOf<T>().let { type ->
            rule(type.concreteClass, configuration)
        }
