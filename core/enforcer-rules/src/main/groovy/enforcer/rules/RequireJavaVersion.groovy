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
package enforcer.rules

import groovy.transform.CompileStatic
import org.apache.commons.lang3.SystemUtils
import org.apache.maven.artifact.versioning.ArtifactVersion
import org.apache.maven.artifact.versioning.DefaultArtifactVersion
import org.gradle.api.model.ObjectFactory
import org.kordamp.gradle.plugin.enforcer.api.EnforcerContext
import org.kordamp.gradle.plugin.enforcer.api.EnforcerPhase

import javax.inject.Inject

import static org.apache.commons.lang3.StringUtils.isNotBlank
import static org.apache.commons.lang3.StringUtils.split
import static org.apache.commons.lang3.StringUtils.stripEnd
import static org.kordamp.gradle.plugin.enforcer.api.EnforcerPhase.AFTER_PROJECT
import static org.kordamp.gradle.plugin.enforcer.api.EnforcerPhase.BEFORE_BUILD

/**
 * This rule checks that the Java version is allowed.
 *
 * Adapted from {@code org.apache.maven.plugins.enforcer.RequireJavaVersion}.
 * Original author: <a href="mailto:brianf@apache.org">Brian Fox</a>
 *
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
class RequireJavaVersion extends AbstractVersionEnforcerRule {
    @Inject
    RequireJavaVersion(ObjectFactory objects) {
        super(objects, [BEFORE_BUILD, AFTER_PROJECT] as EnforcerPhase[])
    }

    @Override
    ArtifactVersion detectVersion(EnforcerContext context) {
        String javaVersion = SystemUtils.JAVA_VERSION

        context.logger.info("Detected Java String: '" + javaVersion + "'")
        javaVersion = normalizeJDKVersion(javaVersion)
        context.logger.info("Normalized Java String: '" + javaVersion + "'")

        ArtifactVersion detectedJdkVersion = new DefaultArtifactVersion(javaVersion)

        context.logger.info("Parsed Version: Major: " + detectedJdkVersion.getMajorVersion() + " Minor: "
            + detectedJdkVersion.getMinorVersion() + " Incremental: " + detectedJdkVersion.getIncrementalVersion()
            + " Build: " + detectedJdkVersion.getBuildNumber() + " Qualifier: " + detectedJdkVersion.getQualifier())

        detectedJdkVersion
    }

    @Override
    protected String getVariableName() {
        'JDK'
    }

    /**
     * Converts a jdk string from 1.5.0-11b12 to a single 3 digit version like 1.5.0-11
     *
     * @param theJdkVersion to be converted.
     * @return the converted string.
     */
    private static String normalizeJDKVersion(String theJdkVersion) {
        theJdkVersion = theJdkVersion.replaceAll('_|-', '.')
        String[] tokenArray = split(theJdkVersion, '.')
        List<String> tokens = Arrays.asList(tokenArray)
        StringBuffer buffer = new StringBuffer(theJdkVersion.length())

        Iterator<String> iter = tokens.iterator()
        for (int i = 0; i < tokens.size() && i < 4; i++) {
            String section = iter.next()
            section = section.replaceAll('[^0-9]', '')

            if (isNotBlank(section)) {
                buffer.append(Integer.parseInt(section))

                if (i != 2) {
                    buffer.append('.')
                } else {
                    buffer.append('-')
                }
            }
        }

        String version = buffer.toString()
        version = stripEnd(version, '-')
        return stripEnd(version, '.')
    }
}
