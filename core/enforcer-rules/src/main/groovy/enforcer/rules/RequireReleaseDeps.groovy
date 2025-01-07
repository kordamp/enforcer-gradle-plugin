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

import enforcer.rules.internal.ArtifactUtils
import groovy.transform.CompileStatic
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.kordamp.gradle.plugin.enforcer.api.EnforcerContext
import org.kordamp.gradle.plugin.enforcer.api.EnforcerPhase
import org.kordamp.gradle.plugin.enforcer.api.EnforcerRuleException

import javax.inject.Inject
import java.util.stream.Collectors

import static org.kordamp.gradle.plugin.enforcer.api.EnforcerPhase.AFTER_PROJECT
import static org.kordamp.gradle.plugin.enforcer.api.EnforcerPhase.AFTER_PROJECTS

/**
 * This rule checks that no snapshots are included.
 *
 * Adapted from {@code org.apache.maven.plugins.enforcer.RequireReleaseDeps}.
 * Original author: <a href="mailto:brianf@apache.org">Brian Fox</a>
 *
 * @author Andres Almiray
 * @since 0.8.0
 */
@CompileStatic
class RequireReleaseDeps extends AbstractBanDependencies {
    final ListProperty<String> includes
    final ListProperty<String> excludes

    /**
     * Allows this rule to execute only when this project is a release.
     */
    final Property<Boolean> onlyWhenRelease

    @Inject
    RequireReleaseDeps(ObjectFactory objects) {
        super(objects, [AFTER_PROJECT, AFTER_PROJECTS] as EnforcerPhase[])
        includes = objects.listProperty(String).convention([])
        excludes = objects.listProperty(String).convention([])
        onlyWhenRelease = objects.property(Boolean).convention(false)
    }

    void include(String str) {
        includes.add(str)
    }

    void exclude(String str) {
        excludes.add(str)
    }

    @Override
    void execute(EnforcerContext context) throws EnforcerRuleException {
        if (onlyWhenRelease.get()) {
            String version = String.valueOf(context.project.version)
            if (!version.endsWith('-SNAPSHOT')) {
                super.execute(context);
            }
        } else {
            super.execute(context)
        }
    }

    @Override
    protected Set<ResolvedArtifact> checkDependencies(EnforcerContext context, Set<ResolvedArtifact> artifacts) {
        Set<ResolvedArtifact> excluded = ArtifactUtils.filterDependencies(artifacts, (List<String>) excludes.get())

        if (includes.present) {
            excluded = ArtifactUtils.filterDependencies(excluded, (List<String>) includes.get())
        }

        return excluded?.stream()
            ?.filter({ a -> a.moduleVersion.id.version.endsWith('-SNAPSHOT') })
            ?.collect(Collectors.toSet())
    }
}