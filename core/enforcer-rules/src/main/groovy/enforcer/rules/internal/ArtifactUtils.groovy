/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2020-2021 The author and/or original authors.
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
import org.apache.commons.lang3.StringUtils
import org.gradle.api.artifacts.ResolvedArtifact

/**
 * Utility methods for working with Artifacts.
 *
 * Adapted from {@code org.apache.maven.plugins.enforcer.ArtifactUtils}.
 * Original author:  Robert Scholte
 *
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
final class ArtifactUtils {
    /**
     * Checks the set of dependencies against the list of patterns.
     *
     * @param thePatterns the patterns
     * @param dependencies the dependencies
     * @return a set containing artifacts matching one of the patterns or <code>null</code>
     */
    static Set<ResolvedArtifact> checkDependencies(Set<ResolvedArtifact> dependencies, List<String> thePatterns) {
        Set<ResolvedArtifact> foundMatches = null

        if (thePatterns != null && thePatterns.size() > 0) {
            for (String pattern : thePatterns) {
                String[] subStrings = pattern.split(':')
                subStrings = StringUtils.stripAll(subStrings)
                String resultPattern = StringUtils.join(subStrings, ':')

                for (ResolvedArtifact artifact : dependencies) {
                    if (compareDependency(resultPattern, artifact)) {
                        // only create if needed
                        if (foundMatches == null) {
                            foundMatches = []
                        }
                        foundMatches.add(artifact)
                    }
                }
            }
        }
        return foundMatches
    }

    /**
     * Filters the set of dependencies against the list of patterns.
     *
     * @param thePatterns the patterns
     * @param dependencies the dependencies
     * @return a set containing artifacts not matching one of the patterns.
     */
    static Set<ResolvedArtifact> filterDependencies(Set<ResolvedArtifact> dependencies, List<String> thePatterns) {
        Set<ResolvedArtifact> filtered = new LinkedHashSet<>(dependencies)

        if (thePatterns != null && thePatterns.size() > 0) {
            for (String pattern : thePatterns) {
                String[] subStrings = pattern.split(':')
                subStrings = StringUtils.stripAll(subStrings)
                String resultPattern = StringUtils.join(subStrings, ':')

                for (ResolvedArtifact artifact : dependencies) {
                    if (compareDependency(resultPattern, artifact)) {
                        filtered.remove(artifact)
                    }
                }
            }
        }
        return filtered
    }

    /**
     * Compares the given pattern against the given artifact. The pattern should follow the format
     * {@code groupId:artifactId:version:type:scope:classifier}.
     *
     * @param pattern The pattern to compare the artifact with.
     * @param artifact the artifact
     * @return {@code true} if the artifact matches one of the patterns
     */
    static boolean compareDependency(String pattern, ResolvedArtifact artifact) {
        ArtifactMatcher.Pattern am = new ArtifactMatcher.Pattern(pattern)
        try {
            return am.match(artifact)
        } catch (Exception e) {
            throw new IllegalStateException('Invalid Version Range: ', e)
        }
    }
}
