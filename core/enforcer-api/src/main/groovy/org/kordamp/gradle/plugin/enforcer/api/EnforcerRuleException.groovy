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
package org.kordamp.gradle.plugin.enforcer.api

import groovy.transform.CompileStatic

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
class EnforcerRuleException extends Exception {
    EnforcerRuleException(Class<? extends EnforcerRule> ruleType) {
        super(msg(ruleType))
    }

    EnforcerRuleException(Class<? extends EnforcerRule> ruleType, String message) {
        super(msg(ruleType) + message)
    }

    EnforcerRuleException(Class<? extends EnforcerRule> ruleType, String message, Throwable cause) {
        super(msg(ruleType) + message, cause)
    }

    EnforcerRuleException(Class<? extends EnforcerRule> ruleType, Throwable cause) {
        super(msg(ruleType), cause)
    }

    private static String msg(Class<? extends EnforcerRule> ruleType) {
        String className = ruleType.name
        if (className.endsWith('_Decorated')) className -= '_Decorated'
        "${System.lineSeparator()}Enforcer rule '${className}' was triggered.${System.lineSeparator()}"
    }
}
