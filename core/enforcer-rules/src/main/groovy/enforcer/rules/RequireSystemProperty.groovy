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
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.kordamp.gradle.plugin.enforcer.api.EnforcerContext
import org.kordamp.gradle.plugin.enforcer.api.EnforcerPhase
import org.kordamp.gradle.plugin.enforcer.api.EnforcerRuleException

import javax.inject.Inject

import static org.kordamp.gradle.plugin.enforcer.api.EnforcerPhase.AFTER_PROJECT
import static org.kordamp.gradle.plugin.enforcer.api.EnforcerPhase.AFTER_PROJECTS
import static org.kordamp.gradle.plugin.enforcer.api.EnforcerPhase.BEFORE_PROJECT
import static org.kordamp.gradle.plugin.enforcer.api.EnforcerPhase.BEFORE_PROJECTS

/**
 * This rule checks that certain properties are set.
 *
 * Adapted from {@code org.apache.maven.plugins.enforcer.RequireProperty}.
 * Original author: Paul Gier
 *
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
class RequireSystemProperty extends AbstractPropertyEnforcerRule {
    final Property<String> property
    final Property<Boolean> after

    @Inject
    RequireSystemProperty(ObjectFactory objects) throws EnforcerRuleException {
        super(objects, [BEFORE_PROJECTS, BEFORE_PROJECT, AFTER_PROJECT, AFTER_PROJECTS] as EnforcerPhase[])
        property = objects.property(String)
        after = objects.property(Boolean).convention(false)
    }

    @Override
    protected String getName() {
        return 'System Property'
    }

    @Override
    protected String getPropertyName() {
        return property.orNull
    }

    @Override
    protected void doValidate(EnforcerContext context) throws EnforcerRuleException {
        super.doValidate(context);

        if (!property.present) {
            throw fail("Missing value for 'property'.")
        }
    }

    @Override
    protected void doExecute(EnforcerContext context) throws EnforcerRuleException {
        switch (context.enforcerPhase) {
            case BEFORE_PROJECTS:
            case BEFORE_PROJECT:
                if (!after.get()) {
                    enforceValue(System.getProperty(property.get()))
                }
                break
            case AFTER_PROJECT:
            case AFTER_PROJECTS:
                if (after.get()) {
                    enforceValue(System.getProperty(property.get()))
                }
                break
            default:
                break
        }
    }
}
