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
import org.apache.maven.artifact.versioning.DefaultArtifactVersion
import org.gradle.api.model.ObjectFactory
import org.gradle.util.GradleVersion
import org.kordamp.gradle.plugin.enforcer.api.EnforcerContext
import org.kordamp.gradle.plugin.enforcer.api.EnforcerPhase

import javax.inject.Inject

import static org.kordamp.gradle.plugin.enforcer.api.EnforcerPhase.BEFORE_BUILD

/**
 * This rule checks that the Gradle version is allowed.
 *
 * Adapted from {@code org.apache.maven.plugins.enforcer.RequireMavenVersion}.
 * Original author: <a href="mailto:brianf@apache.org">Brian Fox</a>
 *
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
class RequireGradleVersion extends AbstractVersionEnforcerRule {
    @Inject
    RequireGradleVersion(ObjectFactory objects) {
        super(objects, [BEFORE_BUILD] as EnforcerPhase[])
    }

    @Override
    ArtifactVersion detectVersion(EnforcerContext context) {
        String gradleVersion = GradleVersion.current().version
        context.logger.info("Detected Gradle String: '" + gradleVersion + "'")
        new DefaultArtifactVersion(gradleVersion)
    }

    @Override
    protected String getVariableName() {
        'Gradle'
    }
}
