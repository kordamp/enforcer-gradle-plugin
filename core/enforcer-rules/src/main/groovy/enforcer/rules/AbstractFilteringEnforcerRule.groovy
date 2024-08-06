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
package enforcer.rules

import groovy.transform.CompileStatic
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.kordamp.gradle.plugin.enforcer.api.AbstractEnforcerRule
import org.kordamp.gradle.plugin.enforcer.api.EnforcerContext
import org.kordamp.gradle.plugin.enforcer.api.EnforcerPhase
import org.kordamp.gradle.plugin.enforcer.api.EnforcerRuleException

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
abstract class AbstractFilteringEnforcerRule extends AbstractEnforcerRule {
    final ListProperty<EnforcerPhase> phases
    private final List<EnforcerPhase> allowedPhases = new ArrayList<>()

    AbstractFilteringEnforcerRule(ObjectFactory objects) {
        this(objects, EnforcerPhase.values())
    }

    AbstractFilteringEnforcerRule(ObjectFactory objects, EnforcerPhase[] allowedPhases) {
        super(objects)
        if (allowedPhases == null || allowedPhases.length == 0) {
            this.allowedPhases.addAll(EnforcerPhase.values().toList())
        } else {
            this.allowedPhases.addAll(allowedPhases.toList())
        }

        phases = objects.listProperty(EnforcerPhase).convention(this.allowedPhases)
    }

    void setPhases(List<String> list) {
        if (!list) {
            throw new IllegalArgumentException('Phase list must not be empty nor null')
        }

        for (String phase : list) {
            phases.add(EnforcerPhase.valueOf(phase?.trim()?.toUpperCase()))
        }
    }

    @Override
    protected void doValidate(EnforcerContext context) throws EnforcerRuleException {
        List<EnforcerPhase> list = new ArrayList<>(phases.get())
        list.removeAll(allowedPhases)
        if (!list.isEmpty()) {
            throw fail("Phase${list.size() > 0 ? 's' : ''} not allowed: ${list*.name()}")
        }
    }

    @Override
    void execute(EnforcerContext context) throws EnforcerRuleException {
        if (phases.get().contains(context.enforcerPhase)) {
            context.logger.debug("Enforcing rule ${resolveClassName()} on ${context}")

            doExecute(context)
        }
    }
}
