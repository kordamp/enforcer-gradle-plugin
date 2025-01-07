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
package enforcer.rules.kordamp

import groovy.transform.CompileStatic
import groovy.transform.Memoized
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.kordamp.gradle.plugin.base.ProjectConfigurationExtension
import org.kordamp.gradle.plugin.enforcer.api.EnforcerContext
import org.kordamp.gradle.plugin.enforcer.api.EnforcerRuleException
import org.kordamp.gradle.plugin.enforcer.api.RepeatableEnforcerRule
import org.kordamp.gradle.util.PluginUtils

import javax.inject.Inject
import java.util.regex.Pattern

/**
 * Check if a Kordamp DSL property is set.
 *
 * @author Andres Almiray
 * @since 0.8.0
 */
@CompileStatic
class RequireKordampProperty extends AbstractKordampEnforcerRule implements RepeatableEnforcerRule {
    final ListProperty<String> targets
    final Property<String> property
    final Property<String> regex
    final Property<String> regexMessage
    final Property<Boolean> displayValue

    @Inject
    RequireKordampProperty(ObjectFactory objects) throws EnforcerRuleException {
        super(objects)
        targets = objects.listProperty(String).convention([])
        property = objects.property(String)
        regex = objects.property(String)
        regexMessage = objects.property(String)
        displayValue = objects.property(Boolean).convention(true)
    }

    @Override
    protected void doValidate(EnforcerContext context) throws EnforcerRuleException {
        super.doValidate(context);

        if (!property.present) {
            throw fail("Missing value for 'property'.")
        }
    }

    protected String getPropertyName() {
        return property.orNull
    }

    @Override
    protected void doExecute(EnforcerContext context) throws EnforcerRuleException {
        Set<ProjectConfigurationExtension> configs = resolveTargets(context)

        if (!configs) {
            throw fail("No macthes were found for specified targets ${targets.get()}")
        }

        for (ProjectConfigurationExtension config : configs) {
            check(context, config)
        }
    }

    private Set<ProjectConfigurationExtension> resolveTargets(EnforcerContext context) {
        Set<ProjectConfigurationExtension> configs = new LinkedHashSet<>()

        if (targets.get().isEmpty()) {
            configs.add(PluginUtils.resolveConfig(context.project))
            return configs
        }

        collectTargets(context.project, configs)
        context.project.childProjects.values().each { p ->
            collectTargets(p, configs)
        }

        configs
    }

    private void collectTargets(Project project, Set<ProjectConfigurationExtension> configs) {
        for (String path : targets.get()) {
            if (path == project.path || pattern(path).matcher(project.path).matches()) {
                configs.add(PluginUtils.resolveConfig(project))
                return
            }
        }
    }

    @Override
    protected void check(EnforcerContext context, ProjectConfigurationExtension config) throws EnforcerRuleException {
        String prefix = config.project.path == ':' ? ':' : config.project.path + ':'

        Object value = null

        try {
            Binding binding = new Binding()
            binding.setProperty('config', config)
            GroovyShell shell = new GroovyShell(binding)
            value = shell.evaluate('config.' + getPropertyName())
        } catch (MissingPropertyException e) {
            throw fail("${prefix}config.${getPropertyName()} does not exist")
        } catch (Exception e) {
            throw fail("Unexpected error when evaluating ${prefix}config.${getPropertyName()}", e)
        }

        enforceValue(prefix, value)
    }

    protected void enforceValue(String prefix, Object propValue) throws EnforcerRuleException {

        if (propValue == null) {
            String msg = message.orNull
            if (msg == null) {
                msg = prefix + 'config.' + getPropertyName() + ' is required for this build.'
            }
            throw fail(msg)
        }

        if (regex.present && !propValue.toString().matches(regex.get())) {
            if (!regexMessage.present) {
                if (!message.present) {
                    regexMessage.set(prefix + 'config.' + getPropertyName() +
                        ' does not match the regular expression "' + regex.get() + '".')
                } else {
                    regexMessage.set(message.get())
                }
            }
            if (displayValue.get()) {
                regexMessage.set(regexMessage.get() + ' Value is "' + propValue + '".')
            }
            throw fail(regexMessage.get())
        }
    }

    @Memoized
    private Pattern pattern(String regex) {
        Pattern.compile(asGlobRegex(regex))
    }

    private String asGlobRegex(String globPattern) {
        if (globPattern == '*') return '^.*$'

        StringBuilder result = new StringBuilder(globPattern.length() + 2)
        result.append('^')
        for (int index = 0; index < globPattern.length(); index++) {
            char character = globPattern.charAt(index)
            switch (character) {
                case '*':
                    result.append('.*')
                    break
                case '?':
                    result.append('.')
                    break
                case '$':
                case '(':
                case ')':
                case '.':
                case '[':
                case '\\':
                case ']':
                case '^':
                case '{':
                case '|':
                case '}':
                    result.append('\\')
                default:
                    result.append(character)
                    break
            }
        }
        result.append('$')
        return result.toString()
    }
}
