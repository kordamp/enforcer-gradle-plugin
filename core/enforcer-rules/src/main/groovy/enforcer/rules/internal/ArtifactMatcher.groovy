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
package enforcer.rules.internal

import groovy.transform.CompileStatic
import org.apache.maven.artifact.versioning.ArtifactVersion
import org.apache.maven.artifact.versioning.DefaultArtifactVersion
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException
import org.apache.maven.artifact.versioning.Restriction
import org.apache.maven.artifact.versioning.VersionRange
import org.gradle.api.artifacts.ResolvedArtifact

import static java.util.Objects.requireNonNull
import static org.apache.commons.lang3.StringUtils.isBlank
import static org.apache.maven.artifact.versioning.VersionRange.createFromVersionSpec

/**
 * This class is used for matching Artifacts against a list of patterns.
 *
 * Adapted from {@code org.apache.maven.plugins.enforcer.ArtifactMatcher}.
 * Original author:  Jakub Senko
 *
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
final class ArtifactMatcher {
    /**
     * @author I don't know
     */
    static class Pattern {
        private String pattern
        private String[] parts

        Pattern(String pattern) {
            this.pattern = requireNonNull(pattern, 'pattern')

            parts = pattern.split(":", 5)

            if (parts.length == 5) {
                throw new IllegalArgumentException('Pattern contains too many delimiters.')
            }

            for (String part : parts) {
                if (isBlank(part)) {
                    throw new IllegalArgumentException('Pattern or one of its part is empty.')
                }
            }
        }

        boolean match(ResolvedArtifact artifact) throws InvalidVersionSpecificationException {
            requireNonNull(artifact, 'artifact')

            switch (parts.length) {
                case 4:
                    if (!matches(parts[3], artifact.classifier)) {
                        return false
                    }
                case 3:
                    if (!matches(parts[2], artifact.moduleVersion.id.version)) {
                        if (!containsVersion(createFromVersionSpec(parts[2]),
                            new DefaultArtifactVersion(artifact.moduleVersion.id.version))) {
                            return false
                        }
                    }
                case 2:
                    if (!matches(parts[1], artifact.moduleVersion.id.name)) {
                        return false
                    }
                case 1:
                    return matches(parts[0], artifact.moduleVersion.id.group)
                default:
                    throw new AssertionError()
            }
        }

        private boolean matches(String expression, String input) {
            String regex = expression.replace('.', '\\.')
                .replace('*', '.*')
                .replace(':', '\\:')
                .replace('?', '.')
                .replace('[', '\\[')
                .replace(']', '\\]')
                .replace('(', '\\(')
                .replace(')', '\\)')

            // TODO: Check if this can be done better or prevented earlier.
            if (input == null) {
                input = ''
            }

            return java.util.regex.Pattern.matches(regex, input)
        }

        /**
         * Copied from Artifact.VersionRange. This is tweaked to handle singular ranges properly. Currently the default
         * containsVersion method assumes a singular version means allow everything. This method assumes that "2.0.4" ==
         * "[2.0.4,)"
         *
         * @param allowedRange range of allowed versions.
         * @param theVersion the version to be checked.
         * @return true if the version is contained by the range.
         */
        boolean containsVersion(VersionRange allowedRange, ArtifactVersion theVersion) {
            boolean matched = false
            ArtifactVersion recommendedVersion = allowedRange.getRecommendedVersion()
            if (recommendedVersion == null) {
                List<Restriction> restrictions = allowedRange.getRestrictions()
                for (Restriction restriction : restrictions) {
                    if (restriction.containsVersion(theVersion)) {
                        matched = true
                        break
                    }
                }
            } else {
                // only singular versions ever have a recommendedVersion
                @SuppressWarnings('unchecked')
                int compareTo = recommendedVersion.compareTo(theVersion)
                matched = (compareTo <= 0)
            }
            return matched
        }

        @Override
        String toString() {
            return pattern
        }
    }

    private Collection<Pattern> patterns = []

    private Collection<Pattern> ignorePatterns = []

    /**
     * Construct class by providing patterns as strings. Empty strings are ignored.
     *
     * @throws NullPointerException if any of the arguments is null
     */
    ArtifactMatcher(final Collection<String> patterns, final Collection<String> ignorePatterns) {
        requireNonNull(patterns, 'patterns')
        requireNonNull(ignorePatterns, 'ignorePatterns')

        for (String pattern : patterns) {
            if (pattern != null && !''.equals(pattern)) {
                this.patterns.add(new Pattern(pattern))
            }
        }

        for (String ignorePattern : ignorePatterns) {
            if (ignorePattern != null && !''.equals(ignorePattern)) {
                this.ignorePatterns.add(new Pattern(ignorePattern))
            }
        }
    }

    /**
     * Check if artifact matches patterns.
     *
     * @throws InvalidVersionSpecificationException
     */
    boolean match(ResolvedArtifact artifact)
        throws InvalidVersionSpecificationException {
        for (Pattern pattern : patterns) {
            if (pattern.match(artifact)) {
                for (Pattern ignorePattern : ignorePatterns) {
                    if (ignorePattern.match(artifact)) {
                        return false
                    }
                }
                return true
            }
        }
        return false
    }
}
