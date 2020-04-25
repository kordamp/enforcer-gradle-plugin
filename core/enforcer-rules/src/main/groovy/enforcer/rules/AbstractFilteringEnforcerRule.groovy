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
package enforcer.rules

import groovy.transform.CompileStatic
import org.gradle.api.model.ObjectFactory
import org.kordamp.gradle.plugin.enforcer.api.AbstractEnforcerRule
import org.kordamp.gradle.plugin.enforcer.api.EnforcerContext
import org.kordamp.gradle.plugin.enforcer.api.EnforcerPhase
import org.kordamp.gradle.plugin.enforcer.api.EnforcerRuleException

import static java.lang.System.arraycopy

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
abstract class AbstractFilteringEnforcerRule extends AbstractEnforcerRule {
    final EnforcerPhase[] allowedPhases

    AbstractFilteringEnforcerRule(ObjectFactory objects) {
        this(objects, EnforcerPhase.values())
    }

    AbstractFilteringEnforcerRule(ObjectFactory objects, EnforcerPhase[] allowedPhases) {
        super(objects)
        if (allowedPhases == null || allowedPhases.length == 0) {
            this.allowedPhases = EnforcerPhase.values()
        } else {
            this.allowedPhases = new EnforcerPhase[allowedPhases.length]
            arraycopy(allowedPhases, 0, this.allowedPhases, 0, allowedPhases.length)
        }
        Arrays.sort(this.allowedPhases)
    }

    @Override
    void execute(EnforcerContext context) throws EnforcerRuleException {
        if (Arrays.binarySearch(allowedPhases, context.enforcerPhase) > -1) {
            context.logger.debug("Enforcing rule ${resolveClassName()} on ${context}")

            doExecute(context)
        }
    }
}
