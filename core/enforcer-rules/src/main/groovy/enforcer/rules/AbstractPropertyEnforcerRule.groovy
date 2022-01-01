/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2020-2022 The author and/or original authors.
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
import org.kordamp.gradle.plugin.enforcer.api.EnforcerPhase
import org.kordamp.gradle.plugin.enforcer.api.EnforcerRuleException
import org.kordamp.gradle.plugin.enforcer.api.RepeatableEnforcerRule

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
abstract class AbstractPropertyEnforcerRule extends AbstractStandardEnforcerRule implements RepeatableEnforcerRule {
    final Property<String> regex
    final Property<String> regexMessage
    final Property<Boolean> displayValue

    AbstractPropertyEnforcerRule(ObjectFactory objects) {
        this(objects, EnforcerPhase.values())
    }

    AbstractPropertyEnforcerRule(ObjectFactory objects, EnforcerPhase[] allowedPhases) {
        super(objects, allowedPhases)
        regex = objects.property(String)
        regexMessage = objects.property(String)
        displayValue = objects.property(Boolean).convention(true)
    }

    protected void enforceValue(Object propValue) throws EnforcerRuleException {
        if (propValue == null) {
            String msg = message.orNull
            if (msg == null) {
                msg = getName() + ' "' + getPropertyName() + '" is required for this build.'
            }
            throw fail(msg)
        }

        if (regex.present && !propValue.toString().matches(regex.get())) {
            if (!regexMessage.present) {
                if (!message.present) {
                    regexMessage.set(getName() + ' "' + getPropertyName() +
                        '" does not match the regular expression "' + regex.get() + '".')
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

    /**
     * The name of the property being evaluated
     */
    protected abstract String getName()

    protected abstract String getPropertyName()
}
