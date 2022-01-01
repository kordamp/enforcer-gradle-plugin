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
package org.kordamp.gradle.plugin.enforcer

import enforcer.rules.AbstractStandardEnforcerRule
import groovy.transform.CompileStatic
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.kordamp.gradle.plugin.enforcer.api.EnforcerContext
import org.kordamp.gradle.plugin.enforcer.api.EnforcerPhase
import org.kordamp.gradle.plugin.enforcer.api.EnforcerRuleException

import javax.inject.Inject

@CompileStatic
class Bomb extends AbstractStandardEnforcerRule {
    final Property<EnforcerPhase> buildPhase

    @Inject
    Bomb(ObjectFactory objects) {
        super(objects)
        buildPhase = objects.property(EnforcerPhase).convention(EnforcerPhase.BEFORE_BUILD)
        message.set('boom')
    }

    @Override
    protected void doExecute(EnforcerContext context) throws EnforcerRuleException {
        if (buildPhase.get() == context.enforcerPhase) {
            throw fail(message.get())
        }
    }
}
