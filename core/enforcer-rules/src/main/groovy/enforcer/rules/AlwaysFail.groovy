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
import org.kordamp.gradle.plugin.enforcer.api.EnforcerContext
import org.kordamp.gradle.plugin.enforcer.api.EnforcerRuleException

import javax.inject.Inject

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
class AlwaysFail extends AbstractStandardEnforcerRule {
    @Inject
    AlwaysFail(ObjectFactory objects) throws EnforcerRuleException {
        super(objects)
    }

    @Override
    protected void doExecute(EnforcerContext context) throws EnforcerRuleException {
        throw fail(resolveMessage())
    }

    private String resolveMessage() {
        StringBuilder b = new StringBuilder()
        if (message.present) {
            b.append(message.get()).append(System.lineSeparator())
        }
        b.append('Always fails!')
        return b.toString()
    }
}
