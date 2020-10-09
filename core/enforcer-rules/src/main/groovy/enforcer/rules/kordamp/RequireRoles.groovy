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
package enforcer.rules.kordamp

import groovy.transform.CompileStatic
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.kordamp.gradle.plugin.base.ProjectConfigurationExtension
import org.kordamp.gradle.plugin.base.model.Person
import org.kordamp.gradle.plugin.enforcer.api.EnforcerContext
import org.kordamp.gradle.plugin.enforcer.api.EnforcerRuleException

import javax.inject.Inject

import static org.apache.commons.lang3.StringUtils.isNotBlank

/**
 * Check if roles are covered by people found in {@code project.config.info.people}.
 *
 * Adapted from {@code org.apache.maven.plugins.enforcer.AbstractRequireRoles}
 * Original authors: Mirko Friedenhagen
 *
 * @author Andres Almiray
 * @since 0.7.0
 */
@CompileStatic
class RequireRoles extends AbstractKordampEnforcerRule {
    final ListProperty<String> requiredRoles
    final ListProperty<String> validRoles

    @Inject
    RequireRoles(ObjectFactory objects) throws EnforcerRuleException {
        super(objects)
        requiredRoles = objects.listProperty(String).convention([])
        validRoles = objects.listProperty(String).convention([])
    }

    void required(String str) {
        if (isNotBlank(str)) {
            requiredRoles.add(str)
        }
    }

    void valid(String str) {
        if (isNotBlank(str)) {
            validRoles.add(str)
        }
    }

    @Override
    protected void check(EnforcerContext context, ProjectConfigurationExtension config) throws EnforcerRuleException {
        Set<String> set = requiredRoles.get().<String>toSet()
        Set<String> requiredRolesSet = Collections.unmodifiableSet(set)
        Set<String> rolesFromProject = Collections.unmodifiableSet(getRolesFromProject(config))

        checkRequiredRoles(requiredRolesSet, rolesFromProject)
        checkValidRoles(requiredRolesSet, rolesFromProject)
    }

    private Set<String> getRolesFromProject(ProjectConfigurationExtension config) {
        Set<String> roles = new LinkedHashSet<>()
        config.info.people.forEach { Person person ->
            roles.addAll(person.roles)
        }
        roles
    }

    private void checkRequiredRoles(Set<String> requiredRolesSet, Set<String> rolesFromProject) throws EnforcerRuleException {
        Set<String> copyOfRequiredRolesSet = new LinkedHashSet<String>(requiredRolesSet)
        copyOfRequiredRolesSet.removeAll(rolesFromProject)
        if (copyOfRequiredRolesSet.size() > 0) {
            String message = String.format("Found no person representing role(s) '%s'", copyOfRequiredRolesSet)
            throw fail(message)
        }
    }

    private void checkValidRoles(Set<String> requiredRolesSet, Set<String> rolesFromProject) throws EnforcerRuleException {
        Set<String> copyOfRolesFromProject = new LinkedHashSet<String>(rolesFromProject)
        Set<String> allowedRoles = validRoles.get().toSet()
        if (!allowedRoles.contains('*')) {
            allowedRoles.addAll(requiredRolesSet)

            // results in invalid roles
            copyOfRolesFromProject.removeAll(allowedRoles)
            if (copyOfRolesFromProject.size() > 0) {
                String message = String.format("Found invalid person role(s) '%s'", copyOfRolesFromProject)
                throw fail(message)
            }
        }
    }
}
