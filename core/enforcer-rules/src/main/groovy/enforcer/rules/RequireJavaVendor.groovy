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
package enforcer.rules

import groovy.transform.CompileStatic
import org.apache.commons.lang3.SystemUtils
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.kordamp.gradle.plugin.enforcer.api.EnforcerContext
import org.kordamp.gradle.plugin.enforcer.api.EnforcerPhase
import org.kordamp.gradle.plugin.enforcer.api.EnforcerRuleException

import javax.inject.Inject

import static org.kordamp.gradle.plugin.enforcer.api.EnforcerPhase.AFTER_PROJECT
import static org.kordamp.gradle.plugin.enforcer.api.EnforcerPhase.BEFORE_BUILD

/**
 * This rule checks that the Java vendor is allowed.
 *
 * Adapted from {@code org.apache.maven.plugins.enforcer.RequireJavaVendor}
 * Original author: Tim Sijstermans
 *
 * @author Andres Almiray
 * @since 0.9.0
 */
@CompileStatic
class RequireJavaVendor extends AbstractStandardEnforcerRule {
    final ListProperty<String> includes
    final ListProperty<String> excludes

    @Inject
    RequireJavaVendor(ObjectFactory objects) throws EnforcerRuleException {
        super(objects, [BEFORE_BUILD, AFTER_PROJECT] as EnforcerPhase[])
        includes = objects.listProperty(String).convention([])
        excludes = objects.listProperty(String).convention([])
    }

    void include(String str) {
        includes.add(str)
    }

    void exclude(String str) {
        excludes.add(str)
    }

    @Override
    protected void doExecute(EnforcerContext context) throws EnforcerRuleException {
        String javaVendor = SystemUtils.JAVA_VENDOR

        if (excludes.present && excludes.get().contains(javaVendor)) {
            throw fail(javaVendor, 'an excluded', (List<String>) excludes.get())
        } else if (includes.present && !includes.get().contains(javaVendor)) {
            throw fail(javaVendor, 'not an included', (List<String>) includes.get())
        }
    }

    private EnforcerRuleException fail(String javaVendor, String action, List<String> list) {
        StringBuilder sb = new StringBuilder()

        if (message.present) {
            sb.append(message.get())
                .append(System.lineSeparator())
        }
        sb.append("${javaVendor} is ${action} Required Java Vendor (JAVA_HOME=${SystemUtils.JAVA_HOME}) ${list}")

        fail(sb.toString())
    }
}

