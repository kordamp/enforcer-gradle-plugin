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
import org.kordamp.gradle.plugin.enforcer.api.EnforcerLevel
import org.kordamp.gradle.plugin.enforcer.api.EnforcerRule

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@PackageScope
@CompileStatic
class MultipleEnforcerRuleException extends Exception {
    final boolean error

    MultipleEnforcerRuleException(List<AbstractEnforcerRuleInvoker.RuleExecutionFailure<? extends EnforcerRule>> exceptions) {
        super(format(exceptions))

        boolean errorState = false
        if (exceptions.size() > 0) {
            for (AbstractEnforcerRuleInvoker.RuleExecutionFailure failure : exceptions) {
                if (isFailOnError(failure.rule)) {
                    errorState = true
                    break
                }
            }
        }
        error = errorState
    }

    static String format(List<AbstractEnforcerRuleInvoker.RuleExecutionFailure<? extends EnforcerRule>> exceptions) {
        if (!exceptions) {
            throw new IllegalStateException('The exceptions list is empty!')
        }

        StringBuilder b = new StringBuilder('')
        for (int i = 0; i < exceptions.size(); i++) {
            if (i != 0) {
                b.append(System.lineSeparator())
            }
            b.append('\t').append(exceptions[i].failure.message)
        }
        b.toString()
    }

    private static <RULE extends EnforcerRule> boolean isFailOnError(RULE rule) {
        if (rule.enforcerLevel.isPresent()) {
            return rule.enforcerLevel.get() == EnforcerLevel.ERROR
        }
        false
    }
}
