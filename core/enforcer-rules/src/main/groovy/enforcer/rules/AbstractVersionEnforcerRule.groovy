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
import org.apache.maven.artifact.versioning.ArtifactVersion
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException
import org.apache.maven.artifact.versioning.Restriction
import org.apache.maven.artifact.versioning.VersionRange
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.kordamp.gradle.plugin.enforcer.api.EnforcerContext
import org.kordamp.gradle.plugin.enforcer.api.EnforcerPhase
import org.kordamp.gradle.plugin.enforcer.api.EnforcerRuleException

import static org.apache.commons.lang3.StringUtils.isBlank

/**
 * Contains the common code to compare a version against a version range.
 *
 * Examples of valid version values
 * <ul>
 * <li><code>2.0.4</code> Version 2.0.4 and higher (different from Maven meaning)</li>
 * <li><code>[2.0,2.1)</code> Versions 2.0 (included) to 2.1 (not included)</li>
 * <li><code>[2.0,2.1]</code> Versions 2.0 to 2.1 (both included)</li>
 * <li><code>[2.0.5,)</code> Versions 2.0.5 and higher</li>
 * <li><code>(,2.0.5],[2.1.1,)</code> Versions up to 2.0.5 (included) and 2.1.1 or higher</li>
 * </ul>
 *
 * Adapted from {@code org.apache.maven.plugins.enforcer.AbstractVersionEnforcer}.
 * Original author: <a href="mailto:brianf@apache.org">Brian Fox</a>
 *
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
abstract class AbstractVersionEnforcerRule extends AbstractStandardEnforcerRule {
    final Property<String> version

    AbstractVersionEnforcerRule(ObjectFactory objects) {
        this(objects, EnforcerPhase.values())
    }

    AbstractVersionEnforcerRule(ObjectFactory objects, EnforcerPhase[] allowedPhases) {
        super(objects, allowedPhases)
        version = objects.property(String)
    }

    @Override
    protected void doValidate(EnforcerContext context) throws EnforcerRuleException {
        super.doValidate(context);

        String requiredVersionRange = version.get()

        if (isBlank(requiredVersionRange)) {
            throw fail(variableName + " version can't be empty.")
        }
    }

    @Override
    protected void doExecute(EnforcerContext context) throws EnforcerRuleException {
        ArtifactVersion detectedVersion = detectVersion(context)
        String requiredVersionRange = adjustVersion(version.get())

        String msg = 'Detected ' + variableName + ' Version: ' + detectedVersion

        // short circuit check if the strings are exactly equal
        if (detectedVersion.toString().equals(requiredVersionRange)) {
            context.logger.info(msg + ' is allowed in the range ' + requiredVersionRange + '.')
        } else {
            try {
                VersionRange vr = VersionRange.createFromVersionSpec(requiredVersionRange)

                if (containsVersion(vr, detectedVersion)) {
                    context.logger.info(msg + ' is allowed in the range ' + requiredVersionRange + '.')
                } else {
                    String m = message.orNull

                    if (isBlank(m)) {
                        m = msg + ' is not in the allowed range ' + vr + '.'
                    }

                    throw fail(m)
                }
            } catch (InvalidVersionSpecificationException e) {
                throw fail('The requested ' + variableName + ' version '
                    + requiredVersionRange + ' is invalid.', e)
            }
        }
    }

    protected abstract ArtifactVersion detectVersion(EnforcerContext context)

    protected abstract String getVariableName()

    /**
     * Copied from Artifact.VersionRange. This is tweaked to handle singular ranges properly. Currently the default
     * containsVersion method assumes a singular version means allow everything. This method assumes that "2.0.4" ==
     * "[2.0.4,)"
     *
     * @param allowedRange range of allowed versions.
     * @param theVersion the version to be checked.
     * @return true if the version is contained by the range.
     */
    static boolean containsVersion(VersionRange allowedRange, ArtifactVersion theVersion) {
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

    protected String adjustVersion(String version) {
        version
    }
}
