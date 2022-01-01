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
package enforcer.rules.kordamp

import enforcer.rules.AbstractStandardEnforcerRule
import groovy.transform.CompileStatic
import org.gradle.api.model.ObjectFactory
import org.kordamp.gradle.plugin.base.ProjectConfigurationExtension
import org.kordamp.gradle.plugin.enforcer.api.EnforcerContext
import org.kordamp.gradle.plugin.enforcer.api.EnforcerPhase
import org.kordamp.gradle.plugin.enforcer.api.EnforcerRuleException
import org.kordamp.gradle.util.PluginUtils

import javax.inject.Inject

import static org.kordamp.gradle.plugin.enforcer.api.EnforcerPhase.AFTER_PROJECTS

/**
 * Base rule for checking Kordamp DSL values
 *
 * @author Andres Almiray
 * @since 0.7.0
 */
@CompileStatic
abstract class AbstractKordampEnforcerRule extends AbstractStandardEnforcerRule {
    @Inject
    AbstractKordampEnforcerRule(ObjectFactory objects) throws EnforcerRuleException {
        super(objects, [AFTER_PROJECTS] as EnforcerPhase[])
    }

    @Override
    protected void doExecute(EnforcerContext context) throws EnforcerRuleException {
        ProjectConfigurationExtension config = PluginUtils.resolveConfig(context.project)
        check(context, config)
    }

    protected abstract void check(EnforcerContext context, ProjectConfigurationExtension config) throws EnforcerRuleException
}
