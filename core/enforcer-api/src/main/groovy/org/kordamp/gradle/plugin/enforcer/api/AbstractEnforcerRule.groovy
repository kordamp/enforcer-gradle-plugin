/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2020-2021 The author and/or original authors.
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
import org.gradle.api.internal.provider.Providers
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property

import static org.apache.commons.lang3.StringUtils.isNotBlank

/**
 * Base class for implementing {@code EnforcerRule}.
 *
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
abstract class AbstractEnforcerRule implements EnforcerRule {
    final Property<Boolean> enabled
    final Property<EnforcerLevel> enforcerLevel

    AbstractEnforcerRule(ObjectFactory objects) {
        enabled = objects.property(Boolean).convention(true)
        enforcerLevel = objects.property(EnforcerLevel).convention(Providers.notDefined())
    }

    @Override
    void setEnforcerLevel(String enforcerLevel) {
        if (isNotBlank(enforcerLevel)) {
            this.enforcerLevel.set(EnforcerLevel.valueOf(enforcerLevel.trim().toUpperCase()))
        }
    }

    @Override
    void validate(EnforcerContext context) throws EnforcerRuleException {
        context.logger.debug("Validating rule ${resolveClassName()} on ${context}")
        doValidate(context)
    }

    @Override
    void execute(EnforcerContext context) throws EnforcerRuleException {
        context.logger.debug("Enforcing rule ${resolveClassName()} on ${context}")
        doExecute(context)
    }

    protected abstract void doValidate(EnforcerContext context) throws EnforcerRuleException

    protected abstract void doExecute(EnforcerContext context) throws EnforcerRuleException

    protected String resolveClassName() {
        String ruleClassName = this.class.name
        if (ruleClassName.endsWith('_Decorated')) ruleClassName -= '_Decorated'
        ruleClassName
    }

    protected IllegalArgumentException illegalArgumentException(String msg) throws IllegalArgumentException {
        throw new IllegalArgumentException(resolveClassName() + ' ' + msg)
    }

    protected IllegalArgumentException illegalArgumentException(String msg, Throwable cause) throws IllegalArgumentException {
        throw new IllegalArgumentException(resolveClassName() + ' ' + msg, cause)
    }


    protected EnforcerRuleException fail(String message) throws EnforcerRuleException {
        throw new EnforcerRuleException(this.class, message)
    }

    protected EnforcerRuleException fail(String message, Exception cause) throws EnforcerRuleException {
        throw new EnforcerRuleException(this.class, message, cause)
    }

    protected EnforcerRuleException fail(Exception cause) throws EnforcerRuleException {
        throw new EnforcerRuleException(this.class, cause)
    }
}
