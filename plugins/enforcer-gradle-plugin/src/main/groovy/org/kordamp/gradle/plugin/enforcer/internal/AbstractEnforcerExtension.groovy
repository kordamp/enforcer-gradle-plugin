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
package org.kordamp.gradle.plugin.enforcer.internal

import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.kordamp.gradle.plugin.enforcer.api.EnforcerExtension
import org.kordamp.gradle.plugin.enforcer.api.EnforcerLevel
import org.kordamp.gradle.plugin.enforcer.api.EnforcerRule
import org.kordamp.gradle.plugin.enforcer.api.MergeStrategy

import static org.apache.commons.lang3.StringUtils.isBlank
import static org.apache.commons.lang3.StringUtils.isNotBlank

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
@PackageScope
abstract class AbstractEnforcerExtension implements EnforcerExtension {
    @PackageScope
    static final Logger LOG = Logging.getLogger(Project)

    final Property<Boolean> enabled
    final Property<Boolean> warnings
    final Property<Boolean> failFast
    final Property<MergeStrategy> mergeStrategy
    final Property<EnforcerLevel> enforcerLevel
    final Provider<Boolean> resolvedFailFast

    protected final ObjectFactory objects
    protected final ProviderFactory providers

    private boolean mergeStrategySet

    AbstractEnforcerExtension(ObjectFactory objects, ProviderFactory providers) {
        this.objects = objects
        this.providers = providers
        this.enabled = objects.property(Boolean).convention(true)
        this.warnings = objects.property(Boolean).convention(true)
        this.failFast = objects.property(Boolean).convention(true)
        this.resolvedFailFast = booleanProvider(providers, 'GRADLE_ENFORCER_FAIL_FAST', 'enforcer.fail.fast', failFast)
        this.mergeStrategy = objects.property(MergeStrategy).convention(MergeStrategy.OVERRIDE)
        this.enforcerLevel = objects.property(EnforcerLevel).convention(EnforcerLevel.ERROR)
    }

    @Override
    void setMergeStrategy(MergeStrategy mergeStrategy) {
        if (mergeStrategySet) {
            println "${prefix} enforcer.mergeStrategy has been set to '${this.mergeStrategy.get().name()}'. Cannot override value."
            return
        }

        if (mergeStrategy != null) {
            this.mergeStrategy.set(mergeStrategy)
            mergeStrategySet = true
        }
    }

    @Override
    void setMergeStrategy(String mergeStrategy) {
        if (mergeStrategySet) {
            println "${prefix} enforcer.mergeStrategy has been set to '${this.mergeStrategy.get().name()}'. Cannot override value."
            return
        }

        if (isNotBlank(mergeStrategy)) {
            this.mergeStrategy.set(MergeStrategy.valueOf(mergeStrategy.trim().toUpperCase()))
            mergeStrategySet = true
        }
    }

    @Override
    void setEnforcerLevel(EnforcerLevel enforcerLevel) {
        if (enforcerLevel != null) {
            this.enforcerLevel.set(enforcerLevel)
        }
    }

    @Override
    void setEnforcerLevel(String enforcerLevel) {
        if (isNotBlank(enforcerLevel)) {
            this.enforcerLevel.set(EnforcerLevel.valueOf(enforcerLevel.trim().toUpperCase()))
        }
    }

    abstract String getPrefix()

    protected IllegalArgumentException deny(Class<? extends EnforcerRule> ruleType) throws IllegalArgumentException {
        throw new IllegalArgumentException("${prefix} enforcer.mergeStrategy has been set to '${mergeStrategy.get().name()}. Cannot override rule ${ruleType.name}.")
    }

    private static Provider<Boolean> booleanProvider(ProviderFactory providers,
                                                     String envKey,
                                                     String propertyKey,
                                                     Provider<Boolean> property) {
        providers.provider {
            String value = System.getProperty(propertyKey)
            if (isBlank(value)) value = System.getenv(envKey)
            if (isNotBlank(value)) {
                return Boolean.parseBoolean(value)
            }
            property.getOrElse(false)
        }
    }
}
