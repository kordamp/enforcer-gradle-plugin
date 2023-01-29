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
package enforcer.rules

import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.kordamp.gradle.plugin.enforcer.api.EnforcerContext
import org.kordamp.gradle.plugin.enforcer.api.EnforcerRuleException

import javax.inject.Inject

import static org.kordamp.gradle.util.StringUtils.isNotBlank

/**
 * This rule checks the set of plugins used during the build and enforces that specific excluded plugins are not used.
 *
 * @author Andres Almiray
 * @since 0.9.0
 */
@CompileStatic
class BannedPlugins extends AbstractPluginsRule {
    final ListProperty<String> includes
    final ListProperty<String> excludes
    final Property<Boolean> failFast

    @Inject
    BannedPlugins(ObjectFactory objects) throws EnforcerRuleException {
        super(objects)
        includes = objects.listProperty(String).convention([])
        excludes = objects.listProperty(String).convention([])
        failFast = objects.property(Boolean).convention(true)
    }

    void include(String str) {
        includes.add(str)
    }

    void exclude(String str) {
        excludes.add(str)
    }

    @Override
    protected void checkPlugins(EnforcerContext context, Project project, List<PluginInfo> plugins) throws EnforcerRuleException {
        StringBuilder sb = new StringBuilder()
        for (PluginInfo pluginInfo : plugins) {
            if (checkPlugin(pluginInfo)) {
                sb.append(pluginInfo.id)
                sb.append(System.lineSeparator())
                if (failFast.get()) {
                    throw fail("Banned plugin found in ${project}: ${sb}")
                }
            }
        }

        if (sb.length() > 0) {
            throw fail("Banned plugins found in ${project}:${System.lineSeparator()}${sb}")
        }
    }

    private boolean checkPlugin(PluginInfo pluginInfo) {
        if (isNotBlank(pluginInfo.id)) {
            if (excludes.get().contains(pluginInfo.id)) {
                if (!includes.get().empty) {
                    if (!includes.get().contains(pluginInfo.id)) {
                        return true
                    }
                    return false
                }
                return true
            }
        }

        false
    }
}

