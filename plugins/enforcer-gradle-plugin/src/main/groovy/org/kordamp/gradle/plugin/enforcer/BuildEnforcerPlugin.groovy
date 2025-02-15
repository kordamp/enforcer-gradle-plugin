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
package org.kordamp.gradle.plugin.enforcer

import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings
import org.gradle.api.logging.LogLevel
import org.gradle.api.model.ObjectFactory
import org.kordamp.gradle.plugin.enforcer.api.BuildEnforcerExtension
import org.kordamp.gradle.plugin.enforcer.internal.BuildEnforcerRuleInvoker
import org.kordamp.gradle.plugin.enforcer.internal.DefaultBuildEnforcerExtension

import javax.inject.Inject

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
class BuildEnforcerPlugin implements Plugin<Settings> {
    private final ObjectFactory objects

    @Inject
    BuildEnforcerPlugin(ObjectFactory objects) {
        this.objects = objects
    }

    @Override
    void apply(Settings settings) {
        if (settings.gradle.startParameter.logLevel != LogLevel.QUIET) {
            settings.gradle.sharedServices
                .registerIfAbsent('enforcer-banner', Banner, { spec -> })
                .get().display(settings)
        }

        BuildEnforcerExtension extension = settings.extensions.create(
            BuildEnforcerExtension,
            'enforce',
            DefaultBuildEnforcerExtension)

        settings.gradle.addListener(new BuildEnforcerRuleInvoker(settings, extension, objects))
    }
}
