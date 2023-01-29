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
import groovy.transform.ToString
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.kordamp.gradle.plugin.enforcer.api.EnforcerContext
import org.kordamp.gradle.plugin.enforcer.api.EnforcerPhase
import org.kordamp.gradle.plugin.enforcer.api.EnforcerRuleException

import javax.inject.Inject
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.regex.Matcher

import static org.kordamp.gradle.plugin.enforcer.api.EnforcerPhase.AFTER_PROJECT
import static org.kordamp.gradle.plugin.enforcer.api.EnforcerPhase.AFTER_PROJECTS

/**
 * @author Andres Almiray
 * @since 0.9.0
 */
@CompileStatic
abstract class AbstractPluginsRule extends AbstractStandardEnforcerRule {
    @Inject
    AbstractPluginsRule(ObjectFactory objects) throws EnforcerRuleException {
        super(objects, [AFTER_PROJECT, AFTER_PROJECTS] as EnforcerPhase[])
    }

    @Override
    protected final void doExecute(EnforcerContext context) throws EnforcerRuleException {
        Map<String, PluginMetadata> pluginMetadata = resolvePluginMetadata()

        switch (context.enforcerPhase) {
            case AFTER_PROJECT:
                // resolve
                checkPlugins(context, context.project, resolvePlugins(context.project, pluginMetadata))
                break
            case AFTER_PROJECTS:
                // resolve
                checkPlugins(context, context.project, resolvePlugins(context.project, pluginMetadata))
                for (Project project : context.project.childProjects.values()) {
                    checkPlugins(context, project, resolvePlugins(project, pluginMetadata))
                }
                break
            default:
                break
        }
    }

    protected Map<String, PluginMetadata> resolvePluginMetadata() {
        Map<String, PluginMetadata> pluginMetadata = new LinkedHashMap<>()
        Enumeration<URL> e = AbstractPluginsRule.classLoader.getResources('META-INF/gradle-plugins')
        while (e.hasMoreElements()) {
            extractPluginMetadata(e.nextElement(), pluginMetadata)
        }
        e = org.gradle.api.plugins.BasePlugin.classLoader.getResources('META-INF/gradle-plugins')
        while (e.hasMoreElements()) {
            extractPluginMetadata(e.nextElement(), pluginMetadata)
        }
        pluginMetadata
    }

    protected List<PluginInfo> resolvePlugins(Project project, Map<String, PluginMetadata> pluginMetadata) {
        List<PluginInfo> plugins = new ArrayList<PluginInfo>()

        project.plugins.each { plugin -> plugins.add(AbstractPluginsRule.collectPluginInformation(plugin, pluginMetadata)) }

        plugins
    }

    protected abstract void checkPlugins(EnforcerContext context, Project project, List<PluginInfo> plugins) throws EnforcerRuleException

    @CompileStatic
    @ToString(includeNames = true)
    static class PluginInfo {
        String id
        String version
        String implementationClass
    }

    @CompileStatic
    static class PluginMetadata {
        String id
        String version
    }

    private void extractPluginMetadata(URL url, Map<String, PluginMetadata> pluginMetadata) {
        if (url.protocol != 'jar') return

        JarFile jarFile = new JarFile(url.toString()[9..url.toString().indexOf('!') - 1])
        Enumeration<JarEntry> entries = jarFile.entries()
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement()
            Matcher matcher = (entry.name =~ /META-INF\/gradle-plugins\/(.+)\.properties/)
            if (matcher.matches()) {
                Properties props = new Properties()
                props.load(jarFile.getInputStream(entry))
                PluginMetadata pluginInfo = new PluginMetadata()
                pluginMetadata.put((String) props.'implementation-class', pluginInfo)
                pluginInfo.id = matcher.group(1) - 'org.gradle.'
                matcher = (jarFile.name =~ /.*\-(\d[\d+\-_A-Za-z\.]+)\.jar/)
                if (matcher.matches()) {
                    pluginInfo.version = matcher.group(1)
                }
            }
        }
    }

    private static PluginInfo collectPluginInformation(Plugin plugin, Map<String, PluginMetadata> pluginMetadata) {
        PluginInfo pluginInfo = new PluginInfo()

        PluginMetadata metadata = pluginMetadata[plugin.class.name]

        if (metadata) {
            pluginInfo.id = metadata.id
            if (metadata.version) pluginInfo.version = metadata.version
            pluginInfo.implementationClass = plugin.class.name
        } else {
            pluginInfo.implementationClass = plugin.class.name
        }

        pluginInfo
    }
}

