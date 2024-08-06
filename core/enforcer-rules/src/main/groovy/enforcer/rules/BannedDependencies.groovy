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

import enforcer.rules.internal.ArtifactUtils
import groovy.transform.CompileStatic
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.kordamp.gradle.plugin.enforcer.api.EnforcerContext
import org.kordamp.gradle.plugin.enforcer.api.EnforcerPhase

import javax.inject.Inject

import static org.kordamp.gradle.plugin.enforcer.api.EnforcerPhase.AFTER_PROJECT
import static org.kordamp.gradle.plugin.enforcer.api.EnforcerPhase.AFTER_PROJECTS

/**
 * This rule checks that lists of dependencies are not included.
 *
 * Adapted from {@code org.apache.maven.plugins.enforcer.BannedDependencies}.
 * Original author: <a href="mailto:brianf@apache.org">Brian Fox</a>
 *
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
class BannedDependencies extends AbstractBanDependencies {
    final ListProperty<String> includes
    final ListProperty<String> excludes

    @Inject
    BannedDependencies(ObjectFactory objects) {
        super(objects, [AFTER_PROJECT, AFTER_PROJECTS] as EnforcerPhase[])
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
    protected Set<ResolvedArtifact> checkDependencies(EnforcerContext context, Set<ResolvedArtifact> artifacts) {
        Set<ResolvedArtifact> excluded = ArtifactUtils.checkDependencies(artifacts, (List<String>) excludes.get())

        // anything specifically included should be removed from the ban list.
        if (excluded != null) {
            Set<ResolvedArtifact> included = ArtifactUtils.checkDependencies(artifacts, (List<String>) includes.get())

            if (included != null) {
                excluded.removeAll(included)
            }
        }

        return excluded
    }
}
