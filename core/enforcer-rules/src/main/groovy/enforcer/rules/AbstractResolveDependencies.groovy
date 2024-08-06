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
import groovy.transform.ToString
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.model.ObjectFactory
import org.kordamp.gradle.plugin.enforcer.api.EnforcerPhase

import java.util.regex.Pattern

import static org.apache.commons.lang3.StringUtils.isNotBlank

/**
 * Bans duplicate classes on the classpath.
 *
 * Adapted from {@code org.apache.maven.plugins.enforcer.AbstractResolveDependencies}.
 * Original author: Robert Scholte
 *
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
abstract class AbstractResolveDependencies extends AbstractStandardEnforcerRule {
    AbstractResolveDependencies(ObjectFactory objects) {
        this(objects, EnforcerPhase.values())
    }

    AbstractResolveDependencies(ObjectFactory objects, EnforcerPhase[] allowedPhases) {
        super(objects, allowedPhases)
    }

    /**
     * Convert a wildcard into a regex.
     *
     * @param wildcard the wildcard to convert.
     * @return the equivalent regex.
     */
    protected static String asRegex(String wildcard) {
        StringBuilder result = new StringBuilder(wildcard.length())
        result.append('^')
        for (int index = 0; index < wildcard.length(); index++) {
            char character = wildcard.charAt(index)
            switch (character) {
                case '*':
                    result.append('.*')
                    break;
                case '?':
                    result.append('.')
                    break;
                case '$':
                case '(':
                case ')':
                case '.':
                case '[':
                case '\\':
                case ']':
                case '^':
                case '{':
                case '|':
                case '}':
                    result.append('\\')
                default:
                    result.append(character)
                    break;
            }
        }
        result.append('(\\.class)?')
        result.append('$')
        return result.toString()
    }

    @ToString
    protected static class IgnorableDependency {
        Pattern groupId
        Pattern artifactId
        Pattern version
        Pattern classifier
        List<Pattern> ignores = []

        IgnorableDependency applyIgnoreClasses(List<String> ignores) {
            for (String ignore : ignores) {
                ignore = ignore.replace('.', '/')
                String pattern = asRegex(ignore)
                this.ignores.add(Pattern.compile(pattern))
            }
            return this
        }

        boolean matchesArtifact(ResolvedArtifact dup) {
            return (artifactId == null || artifactId.matcher(dup.moduleVersion.id.name).matches()) &&
                (groupId == null || groupId.matcher(dup.moduleVersion.id.group).matches()) &&
                (version == null || version.matcher(dup.moduleVersion.id.version).matches()) &&
                (classifier == null || (isNotBlank(dup.classifier) && classifier.matcher(dup.classifier).matches()))
        }

        boolean matches(String className) {
            for (Pattern p : ignores) {
                if (p.matcher(className).matches()) {
                    return true
                }
            }
            return false
        }
    }
}
